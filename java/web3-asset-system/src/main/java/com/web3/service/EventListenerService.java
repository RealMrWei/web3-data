package com.web3.service;

import com.web3.chain.MultiChainManager;
import com.web3.config.Web3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * 事件监听服务 - 监听链上充值、提现等事件并同步状态
 */
@Slf4j
@Service
public class EventListenerService extends BaseContractService {

    private final Web3Properties web3Properties;

    public EventListenerService(MultiChainManager multiChainManager, 
                                ContractAddressService contractAddressService,
                                Web3Properties web3Properties) {
        super(multiChainManager, contractAddressService);
        this.web3Properties = web3Properties;
    }

    /**
     * 监听充值事件（Deposit）
     *
     * @param chainName   链名称
     * @param fromBlock   起始区块号
     * @param toBlock     结束区块号（null表示最新）
     * @return 充值事件列表
     */
    public List<DepositEvent> listenDepositEvents(String chainName, BigInteger fromBlock, 
                                                   BigInteger toBlock) {
        try {
            Web3j web3j = multiChainManager.getWeb3j(chainName);
            String vaultAddress = web3Properties.getContracts().getDepositVault();

            // 定义Deposit事件
            Event depositEvent = new Event(
                "Deposit",
                Arrays.asList(
                    new TypeReference<Address>() {},  // user
                    new TypeReference<Address>() {},  // token
                    new TypeReference<Uint256>() {}   // amount
                )
            );

            // 创建过滤器
            EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                toBlock != null ? DefaultBlockParameter.valueOf(toBlock) : DefaultBlockParameterName.LATEST,
                vaultAddress
            );
            filter.addSingleTopic(EventEncoder.encode(depositEvent));

            // 获取事件日志并转换类型
            List<Log> logs = web3j.ethGetLogs(filter).send().getLogs().stream()
                .map(logResult -> (Log) logResult.get())
                .toList();
            
            log.info("监听到 {} 个充值事件: chain={}", logs.size(), chainName);

            return parseDepositEvents(logs, depositEvent);

        } catch (IOException e) {
            log.error("监听充值事件失败: chain={}", chainName, e);
            throw new RuntimeException("监听事件失败", e);
        }
    }

    /**
     * 监听提现事件（Withdrawal）
     *
     * @param chainName   链名称
     * @param fromBlock   起始区块号
     * @param toBlock     结束区块号
     * @return 提现事件列表
     */
    public List<WithdrawalEvent> listenWithdrawalEvents(String chainName, BigInteger fromBlock,
                                                         BigInteger toBlock) {
        try {
            Web3j web3j = multiChainManager.getWeb3j(chainName);
            String withdrawalManagerAddress = web3Properties.getContracts().getWithdrawalManager();

            // 定义Withdrawal事件
            Event withdrawalEvent = new Event(
                "Withdrawal",
                Arrays.asList(
                    new TypeReference<Address>() {},  // user
                    new TypeReference<Address>() {},  // token
                    new TypeReference<Uint256>() {}   // amount
                )
            );

            // 创建过滤器
            EthFilter filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                toBlock != null ? DefaultBlockParameter.valueOf(toBlock) : DefaultBlockParameterName.LATEST,
                withdrawalManagerAddress
            );
            filter.addSingleTopic(EventEncoder.encode(withdrawalEvent));

            // 获取事件日志并转换类型
            List<Log> logs = web3j.ethGetLogs(filter).send().getLogs().stream()
                .map(logResult -> (Log) logResult.get())
                .toList();
            
            log.info("监听到 {} 个提现事件: chain={}", logs.size(), chainName);

            return parseWithdrawalEvents(logs, withdrawalEvent);

        } catch (IOException e) {
            log.error("监听提现事件失败: chain={}", chainName, e);
            throw new RuntimeException("监听事件失败", e);
        }
    }

    /**
     * 解析充值事件日志
     */
    private List<DepositEvent> parseDepositEvents(List<Log> logs, Event event) {
        return logs.stream().map(log -> {
            List<Type> decodedValues = FunctionReturnDecoder.decode(
                log.getData(),
                event.getNonIndexedParameters()
            );

            DepositEvent depositEvent = new DepositEvent();
            depositEvent.setTxHash(log.getTransactionHash());
            depositEvent.setBlockNumber(log.getBlockNumber());
            depositEvent.setUser((String) decodedValues.get(0).getValue());
            depositEvent.setToken((String) decodedValues.get(1).getValue());
            depositEvent.setAmount(((Uint256) decodedValues.get(2)).getValue());
            depositEvent.setTimestamp(System.currentTimeMillis());

            return depositEvent;
        }).toList();
    }

    /**
     * 解析提现事件日志
     */
    private List<WithdrawalEvent> parseWithdrawalEvents(List<Log> logs, Event event) {
        return logs.stream().map(log -> {
            List<Type> decodedValues = FunctionReturnDecoder.decode(
                log.getData(),
                event.getNonIndexedParameters()
            );

            WithdrawalEvent withdrawalEvent = new WithdrawalEvent();
            withdrawalEvent.setTxHash(log.getTransactionHash());
            withdrawalEvent.setBlockNumber(log.getBlockNumber());
            withdrawalEvent.setUser((String) decodedValues.get(0).getValue());
            withdrawalEvent.setToken((String) decodedValues.get(1).getValue());
            withdrawalEvent.setAmount(((Uint256) decodedValues.get(2)).getValue());
            withdrawalEvent.setTimestamp(System.currentTimeMillis());

            return withdrawalEvent;
        }).toList();
    }

    /**
     * 持续监听新事件（轮询方式）
     *
     * @param chainName      链名称
     * @param lastBlock      上次处理的区块号
     * @param pollInterval   轮询间隔（毫秒）
     */
    public void startContinuousListening(String chainName, BigInteger lastBlock, long pollInterval) {
        log.info("开始持续监听链 {} 的事件，起始区块: {}", chainName, lastBlock);

        // 使用数组包装以支持lambda中的修改
        final BigInteger[] currentLastBlock = {lastBlock};

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    BigInteger currentBlock = multiChainManager.getCurrentBlock(chainName);
                    
                    if (currentBlock.compareTo(currentLastBlock[0]) > 0) {
                        // 处理新区块的事件
                        List<DepositEvent> deposits = listenDepositEvents(
                            chainName, currentLastBlock[0].add(BigInteger.ONE), currentBlock
                        );
                        
                        List<WithdrawalEvent> withdrawals = listenWithdrawalEvents(
                            chainName, currentLastBlock[0].add(BigInteger.ONE), currentBlock
                        );

                        // TODO: 将事件保存到数据库或发送到消息队列
                        
                        currentLastBlock[0] = currentBlock;
                        log.info("已处理到区块: {}", currentBlock);
                    }

                    // 等待下一个轮询周期
                    Thread.sleep(pollInterval);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("事件监听线程被中断");
                    break;
                } catch (Exception e) {
                    log.error("事件监听出错", e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     * 充值事件对象
     */
    public static class DepositEvent {
        private String txHash;
        private BigInteger blockNumber;
        private String user;
        private String token;
        private BigInteger amount;
        private Long timestamp;

        // Getters and Setters
        public String getTxHash() { return txHash; }
        public void setTxHash(String txHash) { this.txHash = txHash; }
        
        public BigInteger getBlockNumber() { return blockNumber; }
        public void setBlockNumber(BigInteger blockNumber) { this.blockNumber = blockNumber; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public BigInteger getAmount() { return amount; }
        public void setAmount(BigInteger amount) { this.amount = amount; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * 提现事件对象
     */
    public static class WithdrawalEvent {
        private String txHash;
        private BigInteger blockNumber;
        private String user;
        private String token;
        private BigInteger amount;
        private Long timestamp;

        // Getters and Setters
        public String getTxHash() { return txHash; }
        public void setTxHash(String txHash) { this.txHash = txHash; }
        
        public BigInteger getBlockNumber() { return blockNumber; }
        public void setBlockNumber(BigInteger blockNumber) { this.blockNumber = blockNumber; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public BigInteger getAmount() { return amount; }
        public void setAmount(BigInteger amount) { this.amount = amount; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
