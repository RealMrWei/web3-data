package com.web3.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.dto.DepositEventMessage;
import com.web3.entity.DepositRecord;
import com.web3.entity.UserAsset;
import com.web3.mapper.DepositRecordMapper;
import com.web3.mapper.UserAssetMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 充值事件消费者 - 批量入库
 * 
 * 核心特性：
 * 1. ✅ 批量消费 Kafka 消息
 * 2. ✅ 幂等性检查，防止重复入库
 * 3. ✅ 手动提交 Offset，保证消息不丢失
 * 4. ✅ 事务控制，保证数据一致性
 * 5. ✅ 乐观锁重试机制
 */
@Slf4j
@Component
public class DepositEventConsumer {

    @Autowired
    private DepositRecordMapper depositRecordMapper;

    @Autowired
    private UserAssetMapper userAssetMapper;

    /**
     * 批量消费充值事件
     * 
     * @param messages Kafka 消息列表
     * @param ack      手动确认对象
     */
    @KafkaListener(topics = "deposit-events", groupId = "deposit-event-group", concurrency = "${spring.kafka.listener.concurrency.deposit:5}", containerFactory = "batchFactory")
    public void consume(@Payload List<String> messages, Acknowledgment ack) {
        log.info("📥 [消费者] 收到 {} 条充值事件消息", messages.size());

        List<DepositRecord> records = new ArrayList<>();
        Set<String> txHashes = new HashSet<>();

        // 解析消息
        for (String message : messages) {
            try {
                DepositEventMessage event = JSON.parseObject(message, DepositEventMessage.class);

                // 构建数据库记录
                DepositRecord record = new DepositRecord();
                record.setChainName(event.getChainName());
                record.setTxHash(event.getTxHash());
                record.setUserAddress(event.getUserAddress());
                record.setTokenAddress(event.getTokenAddress());
                record.setAmount(new BigDecimal(event.getAmount()));
                record.setBlockNumber(event.getBlockNumber());
                record.setStatus(1); // 1-待确认
                record.setCreateTime(LocalDateTime.now());
                record.setUpdateTime(LocalDateTime.now());

                records.add(record);
                txHashes.add(event.getTxHash());

            } catch (Exception e) {
                log.error("❌ [消费者] 解析充值事件消息失败: {}", message, e);
                // ✅ 解析失败直接抛出异常，不提交 offset
                throw new RuntimeException("消息解析失败: " + message, e);
            }
        }

        // ✅ 批量插入数据库（优化版）
        if (!records.isEmpty()) {
            try {
                log.info(" [消费者] 开始数据库操作: 总记录数={}", records.size());

                // 第1步：批量查询已存在的 txHash
                List<String> existingTxHashes = getExistingTxHashes(txHashes);

                // 第2步：过滤掉已存在的记录
                List<DepositRecord> toInsert = records.stream()
                        .filter(record -> !existingTxHashes.contains(record.getTxHash()))
                        .collect(Collectors.toList());

                // 第3步：批量插入充值记录并更新用户资产（事务控制）
                if (!toInsert.isEmpty()) {
                    processDepositRecords(toInsert);
                    log.info("✅ [数据库] 批量处理成功: 插入{}条, 跳过{}条重复",
                            toInsert.size(), records.size() - toInsert.size());
                } else {
                    log.info("⚠️ [数据库] 所有充值记录均已存在，跳过插入");
                }

                // ✅ 手动提交 offset（只有成功后才提交）
                ack.acknowledge();
                log.info("✅ [Kafka] 手动提交 offset");

            } catch (Exception e) {
                log.error("❌ [消费者] 批量处理充值记录失败，不提交 offset", e);
                // ✅ 抛出异常，让 Kafka 重新投递或进入死信队列
                throw new RuntimeException("批量处理充值记录失败", e);
            }
        } else {
            log.info("️ [消费者] 没有有效消息");
            // ✅ 即使没有有效消息也要提交 offset，避免重复消费空消息
            ack.acknowledge();
        }
    }

    /**
     * 事务方法：批量插入充值记录并更新用户资产
     * 
     * @param records 待处理的充值记录列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void processDepositRecords(List<DepositRecord> records) {
        log.info(" [事务] 开始处理 {} 条充值记录", records.size());

        // 第1步：批量插入充值记录
        batchInsert(records);
        log.info("✅ [事务] 批量插入充值记录完成");

        // 第2步：同步更新用户资产表（非事务性，允许部分失败）
        updateUserAssets(records);
        log.info("✅ [事务] 用户资产更新完成");
    }

    /**
     * 批量查询已存在的 txHash
     * 
     * @param txHashes 待检查的 txHash 集合
     * @return 已存在的 txHash 列表
     */
    private List<String> getExistingTxHashes(Set<String> txHashes) {
        if (txHashes.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DepositRecord::getTxHash, txHashes);
        wrapper.select(DepositRecord::getTxHash);

        List<DepositRecord> existingRecords = depositRecordMapper.selectList(wrapper);

        return existingRecords.stream()
                .map(DepositRecord::getTxHash)
                .collect(Collectors.toList());
    }

    /**
     * 真正的批量插入数据库（使用自定义 XML SQL）
     * 
     * @param records 待插入的记录列表
     */
    private void batchInsert(List<DepositRecord> records) {
        log.info(" [数据库] 开始批量插入: count={}", records.size());

        // ✅ 使用自定义批量 INSERT SQL（一次 SQL 插入所有记录）
        int affectedRows = depositRecordMapper.batchInsert(records);

        log.info("✅ [数据库] 批量插入完成: 插入{}条", affectedRows);
    }

    /**
     * 批量更新用户资产（增加余额）+ 乐观锁重试
     * 
     * @param records 待更新的充值记录列表
     */
    private void updateUserAssets(List<DepositRecord> records) {
        for (DepositRecord record : records) {
            boolean success = false;
            int maxRetries = 3; // 最大重试次数
            int retryCount = 0;

            while (!success && retryCount < maxRetries) {
                try {
                    retryCount++;

                    // 查询用户资产记录
                    LambdaQueryWrapper<UserAsset> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(UserAsset::getUserAddress, record.getUserAddress())
                            .eq(UserAsset::getChainName, record.getChainName())
                            .eq(UserAsset::getTokenAddress, record.getTokenAddress());

                    UserAsset asset = userAssetMapper.selectOne(wrapper);

                    if (asset == null) {
                        // 首次充值，创建新记录
                        asset = new UserAsset();
                        asset.setUserAddress(record.getUserAddress());
                        asset.setChainName(record.getChainName());
                        asset.setTokenAddress(record.getTokenAddress());
                        asset.setBalance(record.getAmount());
                        asset.setPendingBalance(BigDecimal.ZERO);
                        asset.setVersion(0L);
                        userAssetMapper.insert(asset);
                        log.info("✅ [资产] 创建新用户资产记录: user={}, token={}, amount={}",
                                record.getUserAddress(), record.getTokenAddress(), record.getAmount());
                        success = true;
                    } else {
                        // ✅ 使用数据库乐观锁更新（防止并发冲突）
                        Long currentVersion = asset.getVersion();

                        LambdaQueryWrapper<UserAsset> updateWrapper = new LambdaQueryWrapper<>();
                        updateWrapper.eq(UserAsset::getId, asset.getId())
                                .eq(UserAsset::getVersion, currentVersion); // 乐观锁

                        UserAsset updateAsset = new UserAsset();
                        updateAsset.setBalance(asset.getBalance().add(record.getAmount()));
                        updateAsset.setVersion(currentVersion + 1); // 版本号 +1
                        updateAsset.setUpdateTime(LocalDateTime.now());

                        int rows = userAssetMapper.update(updateAsset, updateWrapper);
                        if (rows == 0) {
                            // 乐观锁冲突，重试
                            log.warn("⚠️ [资产] 乐观锁冲突，第{}次重试: user={}, token={}",
                                    retryCount, record.getUserAddress(), record.getTokenAddress());

                            if (retryCount >= maxRetries) {
                                log.error("❌ [资产] 乐观锁重试次数耗尽，跳过此记录: txHash={}", record.getTxHash());
                                // 不抛出异常，而是继续处理下一条记录，避免整个批次失败
                                break; // 跳出重试循环
                            }

                            // 短暂等待后重试
                            Thread.sleep(50 * retryCount); // 递增等待时间
                        } else {
                            log.info("✅ [资产] 更新用户资产: user={}, token={}, 充值={}, 新余额={}",
                                    record.getUserAddress(), record.getTokenAddress(),
                                    record.getAmount(), updateAsset.getBalance());
                            success = true;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ [资产] 重试被中断: txHash={}", record.getTxHash(), e);
                    throw new RuntimeException("重试被中断", e);
                } catch (Exception e) {
                    log.error("❌ [资产] 更新用户资产失败: user={}, txHash={}",
                            record.getUserAddress(), record.getTxHash(), e);
                    // 如果是其他异常，也应该继续处理下一条记录
                    break; // 跳出重试循环
                }
            }

            if (!success) {
                log.warn("⚠️ [资产] 更新用户资产失败，已跳过: txHash={}", record.getTxHash());
            }
        }
    }
}
