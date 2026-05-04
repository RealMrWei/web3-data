package com.web3.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.chain.MultiChainManager;
import com.web3.config.Web3Properties;
import com.web3.contract.DepositVault;
import com.web3.dto.DepositEventMessage;
import com.web3.entity.ChainConfig;
import com.web3.entity.EventListenerOffset;
import com.web3.mapper.EventListenerOffsetMapper;
import com.web3.service.ContractAddressService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 充值事件监听器 - 生产级优化版
 * 
 * 核心特性：
 * 1. 使用 Web3j 生成的合约类进行类型安全的事件监听
 * 2. 支持多链并行监听
 * 3. 集群部署下通过 ShedLock + Redis 防止重复执行
 * 4. ✅ 只发送 Kafka，不操作数据库（快速返回）
 * 5. ✅ Redis 去重，防止重复消费
 */
@Slf4j
@Component
public class DepositEventListener {

    @Autowired
    private MultiChainManager multiChainManager;

    @Autowired
    private ContractAddressService contractAddressService;

    @Autowired
    private Web3Properties web3Properties;

    @Autowired
    private EventListenerOffsetMapper offsetMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 定时任务：扫描充值事件
     * 生产环境：每 60 秒执行一次
     * 开发环境：每 10 秒执行一次（加快测试反馈）
     */
    @Scheduled(fixedDelayString = "${scheduled.deposit-sync-interval:10}000")
    @SchedulerLock(name = "deposit_event_listener_lock", lockAtMostFor = "50s", lockAtLeastFor = "10s")
    public void scanDepositEvents() {
        log.info("开始同步充值事件");

        // 从数据库获取启用的链配置
        List<ChainConfig> chains = multiChainManager.getActiveChains();

        for (ChainConfig chain : chains) {
            try {
                syncChainDeposits(chain);
            } catch (Exception e) {
                log.error("同步链 {} 充值事件失败", chain.getChainName(), e);
            }
        }
    }

    /**
     * 同步单链充值事件
     */
    private void syncChainDeposits(ChainConfig chain) {
        String chainName = chain.getChainName();
        Web3j web3j = multiChainManager.getWeb3j(chainName);

        // 从数据库动态获取合约地址
        String contractAddress = contractAddressService.getContractAddress(chainName, "deposit_vault");
        String privateKey = web3Properties.getPrivateKey();

        if (contractAddress == null || privateKey == null) {
            log.warn("链 {} 配置不完整，缺少合约地址或私钥", chainName);
            return;
        }

        DepositVault contract = DepositVault.load(
                contractAddress,
                web3j,
                Credentials.create(privateKey),
                new DefaultGasProvider());

        // 获取最后处理的区块号
        Long lastProcessedBlock = getLastProcessedBlock(chainName);

        // 获取当前区块号
        BigInteger currentBlock = multiChainManager.getCurrentBlock(chainName);

        if (lastProcessedBlock >= currentBlock.longValue()) {
            log.debug("链 {} 已同步到最新区块: {}", chainName, lastProcessedBlock);
            return;
        }

        // 每次最多处理 1000 个区块
        long fromBlock = lastProcessedBlock + 1;
        long toBlock = Math.min(fromBlock + 999, currentBlock.longValue());

        log.info("同步链 {} 的充值事件，区块范围: {} - {}", chainName, fromBlock, toBlock);
        log.info("🔍 [事件查询] 合约地址: {}", contractAddress);

        try {
            // 手动定义DepositReceived事件结构 - 修正参数类型
            Event depositEvent = new Event(
                    "DepositReceived",
                    Arrays.asList(
                            new TypeReference<Bytes32>(true) {}, // depositId (indexed) - bytes32类型
                            new TypeReference<Address>(true) {}, // user (indexed)
                            new TypeReference<Address>(true) {}, // token (indexed)
                            new TypeReference<Uint256>(false) {} // amount
                    )
            );

            String eventSignature = EventEncoder.encode(depositEvent);

            // 构建过滤器
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(fromBlock)),
                    DefaultBlockParameter.valueOf(BigInteger.valueOf(toBlock)),
                    contractAddress
            );
            filter.addSingleTopic(eventSignature);

            log.info("🔍 [事件查询] 事件签名: {}", eventSignature);

            // 执行查询
            EthLog ethLog = web3j.ethGetLogs(filter).send();
            List<EthLog.LogResult> logResults = ethLog.getLogs();

            log.info("链 {} 找到 {} 条充值事件", chainName, logResults.size());

            // 处理每条日志
            int sentCount = 0;
            for (EthLog.LogResult logResult : logResults) {
                Log ethLogItem = (Log) logResult.get();
                
                log.info("处理充值事件 - 交易哈希: {}, 区块号: {}", ethLogItem.getTransactionHash(), ethLogItem.getBlockNumber());
                
                try {
                    // 解码非索引事件参数
                    List<Type> nonIndexedValues = 
                        FunctionReturnDecoder.decode(
                            ethLogItem.getData(), depositEvent.getNonIndexedParameters());
                    
                    // 提取索引参数（在topics中）
                    List<Type> indexedValues = new ArrayList<>();
                    List<String> topics = ethLogItem.getTopics();
                    
                    if (topics.size() > 1) {
                        // depositId (第一个索引参数) - topic[1]
                        if (topics.size() > 1) {
                            Type decodedTopic1 = 
                                FunctionReturnDecoder.decodeIndexedValue(
                                    topics.get(1), depositEvent.getIndexedParameters().get(0));
                            indexedValues.add(decodedTopic1);
                        }
                        // user (第二个索引参数) - topic[2]
                        if (topics.size() > 2) {
                            Type decodedTopic2 = 
                                FunctionReturnDecoder.decodeIndexedValue(
                                    topics.get(2), depositEvent.getIndexedParameters().get(1));
                            indexedValues.add(decodedTopic2);
                        }
                        // token (第三个索引参数) - topic[3]
                        if (topics.size() > 3) {
                            Type decodedTopic3 = 
                                FunctionReturnDecoder.decodeIndexedValue(
                                    topics.get(3), depositEvent.getIndexedParameters().get(2));
                            indexedValues.add(decodedTopic3);
                        }
                    }
                    
                    // 手动构建事件响应对象
                    DepositVault.DepositReceivedEventResponse event = 
                        new DepositVault.DepositReceivedEventResponse();
                    event.log = ethLogItem;
                    if (indexedValues.size() > 0) {
                        // 修正：depositId 是 Bytes32 类型
                        event.depositId = (byte[]) indexedValues.get(0).getValue();
                    }
                    if (indexedValues.size() > 1) {
                        event.user = (String) indexedValues.get(1).getValue();
                    }
                    if (indexedValues.size() > 2) {
                        event.token = (String) indexedValues.get(2).getValue();
                    }
                    if (!nonIndexedValues.isEmpty()) {
                        event.amount = (BigInteger) nonIndexedValues.get(0).getValue();
                    }
                    
                    log.info("解码充值事件 - ID: {}, 用户: {}, 代币: {}, 金额: {}", 
                        event.depositId != null ? Arrays.toString(event.depositId) : "null", 
                        event.user, event.token, event.amount);

                    if (sendToKafka(chainName, event)) {
                        sentCount++;
                    }
                } catch (Exception e) {
                    log.error("解码充值事件失败: txHash={}, blockNumber={}", 
                        ethLogItem.getTransactionHash(), ethLogItem.getBlockNumber(), e);
                }
            }

            log.info("链 {} 成功发送 {} 条充值事件到 Kafka", chainName, sentCount);

            // 更新偏移量
            updateLastProcessedBlock(chainName, toBlock);

        } catch (Exception e) {
            log.error("同步链 {} 的充值事件失败", chainName, e);
            throw new RuntimeException("同步充值事件失败", e);
        }
    }

    /**
     * 发送充值事件到 Kafka（快速返回，不操作数据库）
     * 
     * @param chainName 链名称
     * @param event 充值事件响应对象
     * @return true-发送成功，false-已存在（去重）
     */
    private boolean sendToKafka(String chainName, DepositVault.DepositReceivedEventResponse event) {
        String txHash = event.log.getTransactionHash();
        
        // ✅ Redis 去重检查（24小时过期）- 加上链名称前缀避免跨链冲突
        String redisKey = "deposit:processed:" + chainName + ":" + txHash;
        Boolean exists = redisTemplate.hasKey(redisKey);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("交易 {} 在链 {} 上已处理，跳过", txHash, chainName);
            return false;
        }

        // 构建消息
        DepositEventMessage message = new DepositEventMessage();
        message.setChainName(chainName);
        message.setTxHash(txHash);
        message.setUserAddress(event.user);
        message.setTokenAddress(event.token);
        message.setAmount(event.amount.toString());
        message.setBlockNumber(event.log.getBlockNumber().longValue());
        message.setTimestamp(System.currentTimeMillis());

        // 发送 Kafka
        kafkaTemplate.send("deposit-events", txHash, JSON.toJSONString(message));

        // ✅ 标记为已处理（防止重复消费）
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);

        log.info("✅ 发送充值事件到 Kafka: txHash={}, user={}, amount={}", 
            txHash, event.user, event.amount);
        
        return true;
    }

    /**
     * 获取最后处理的区块号
     */
    private Long getLastProcessedBlock(String chainName) {
        LambdaQueryWrapper<EventListenerOffset> query = new LambdaQueryWrapper<>();
        query.eq(EventListenerOffset::getChainName, chainName)
             .eq(EventListenerOffset::getEventType, "deposit");
        
        EventListenerOffset offset = offsetMapper.selectOne(query);
        if (offset == null) {
            log.info("未找到链 {} 的充值事件偏移量，使用默认值 0", chainName);
            return 0L;
        }
        return offset.getLastProcessedBlock();
    }

    /**
     * 更新最后处理的区块号
     */
    private void updateLastProcessedBlock(String chainName, long blockNumber) {
        LambdaQueryWrapper<EventListenerOffset> query = new LambdaQueryWrapper<>();
        query.eq(EventListenerOffset::getChainName, chainName)
             .eq(EventListenerOffset::getEventType, "deposit");
        
        EventListenerOffset offset = offsetMapper.selectOne(query);
        if (offset == null) {
            offset = new EventListenerOffset();
            offset.setChainName(chainName);
            offset.setEventType("deposit");
            offset.setLastProcessedBlock(blockNumber);
            offsetMapper.insert(offset);
            log.info("插入链 {} 的充值事件偏移量: {}", chainName, blockNumber);
        } else {
            offset.setLastProcessedBlock(blockNumber);
            offsetMapper.updateById(offset);
            log.info("更新链 {} 的充值事件偏移量: {}", chainName, blockNumber);
        }
    }
}