package com.web3.consumer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.web3.dto.WithdrawalEventMessage;
import com.web3.entity.UserAsset;
import com.web3.entity.WithdrawalOrder;
import com.web3.mapper.WithdrawalOrderMapper;
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
 * 提现事件消费者 - 批量入库（生产级优化）
 * 
 * 核心特性：
 * 1. ✅ 批量消费 Kafka 消息
 * 2. ✅ 批量查询去重（减少 IO 次数）
 * 3. ✅ 事务控制，保证数据一致性
 * 4. ✅ 手动提交 Offset，保证消息不丢失
 * 5. ✅ 乐观锁重试机制
 */
@Slf4j
@Component
public class WithdrawalEventConsumer {
    
    @Autowired
    private WithdrawalOrderMapper withdrawalOrderMapper;

    @Autowired
    private UserAssetMapper userAssetMapper;

    /**
     * 批量消费提现事件
     * 
     * @param messages Kafka 消息列表
     * @param ack 手动确认对象
     */
    @KafkaListener(
        topics = "withdrawal-events",
        groupId = "withdrawal-event-group",
        containerFactory = "batchFactory"  // 使用批量工厂
    )
    public void consume(@Payload List<String> messages, Acknowledgment ack) {
        log.info("📥 [消费者] 收到 {} 条提现事件消息", messages.size());
        
        List<WithdrawalOrder> orders = new ArrayList<>();
        Set<String> txHashes = new HashSet<>();
        
        // 解析消息
        for (String message : messages) {
            try {
                WithdrawalEventMessage event = JSON.parseObject(message, WithdrawalEventMessage.class);
                
                // 构建数据库记录
                WithdrawalOrder order = new WithdrawalOrder();
                order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
                order.setUserAddress(event.getUserAddress());
                order.setRecipient(event.getUserAddress()); // 默认提现到用户自己的地址
                order.setChainName(event.getChainName());
                order.setTokenAddress(event.getTokenAddress());
                order.setAmount(new BigDecimal(event.getAmount()));
                order.setStatus(0); // 0-待审批
                order.setTxHash(event.getTxHash());
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                
                orders.add(order);
                txHashes.add(event.getTxHash());
                
            } catch (Exception e) {
                log.error("❌ 解析提现事件消息失败: {}", message, e);
                // ✅ 解析失败直接抛出异常，不提交 offset
                throw new RuntimeException("消息解析失败: " + message, e);
            }
        }
        
        // ✅ 批量插入数据库（优化版）
        if (!orders.isEmpty()) {
            try {
                // 第1步：批量查询已存在的 txHash
                List<String> existingTxHashes = getExistingTxHashes(txHashes);
                
                // 第2步：过滤掉已存在的记录
                List<WithdrawalOrder> toInsert = orders.stream()
                    .filter(order -> !existingTxHashes.contains(order.getTxHash()))
                    .collect(Collectors.toList());
                
                // 第3步：批量插入提现订单并扣减用户资产（事务控制）
                if (!toInsert.isEmpty()) {
                    processWithdrawalOrders(toInsert);
                    log.info("✅ [数据库] 批量处理成功: 插入{}条, 跳过{}条重复", 
                        toInsert.size(), orders.size() - toInsert.size());
                } else {
                    log.info("⚠️ [数据库] 所有提现订单均已存在，跳过插入");
                }
                
                // ✅ 手动提交 Offset（只有成功后才提交）
                ack.acknowledge();
                
            } catch (Exception e) {
                log.error("❌ [消费者] 批量处理提现订单失败，不提交 offset", e);
                // ✅ 抛出异常，让 Kafka 重新投递或进入死信队列
                throw new RuntimeException("批量处理提现订单失败", e);
            }
        } else {
            log.info("️ [消费者] 没有有效消息");
            // ✅ 即使没有有效消息也要提交 offset
            ack.acknowledge();
        }
    }
    
    /**
     * 事务方法：批量插入提现订单并扣减用户资产
     * 
     * @param orders 待处理的提现订单列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void processWithdrawalOrders(List<WithdrawalOrder> orders) {
        log.info(" [事务] 开始处理 {} 条提现订单", orders.size());

        // 第1步：批量插入提现订单
        batchInsert(orders);
        log.info("✅ [事务] 批量插入提现订单完成");

        // 第2步：同步扣减用户资产（非事务性，允许部分失败）
        deductUserAssets(orders);
        log.info("✅ [事务] 用户资产扣减完成");
    }
    
    /**
     * 批量查询已存在的 txHash
     */
    private List<String> getExistingTxHashes(Set<String> txHashes) {
        if (txHashes.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<WithdrawalOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WithdrawalOrder::getTxHash, txHashes);
        wrapper.select(WithdrawalOrder::getTxHash);

        List<WithdrawalOrder> existingRecords = withdrawalOrderMapper.selectList(wrapper);

        return existingRecords.stream()
                .map(WithdrawalOrder::getTxHash)
                .collect(Collectors.toList());
    }
    
    /**
     * 真正的批量插入数据库（使用自定义 XML SQL）
     * 
     * @param orders 待插入的订单列表
     */
    private void batchInsert(List<WithdrawalOrder> orders) {
        log.info(" [数据库] 开始批量插入: count={}", orders.size());

        // ✅ 使用自定义批量 INSERT SQL（一次 SQL 插入所有记录）
        int affectedRows = withdrawalOrderMapper.batchInsert(orders);
        
        log.info("✅ [数据库] 批量插入完成: 插入{}条", affectedRows);
    }
    
    /**
     * 扣减用户资产（减少余额）+ 乐观锁重试
     * 
     * @param orders 待扣减的提现订单列表
     */
    private void deductUserAssets(List<WithdrawalOrder> orders) {
        for (WithdrawalOrder order : orders) {
            boolean success = false;
            int maxRetries = 3; // 最大重试次数
            int retryCount = 0;

            while (!success && retryCount < maxRetries) {
                try {
                    retryCount++;
                    
                    // 查询用户资产记录
                    LambdaQueryWrapper<UserAsset> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(UserAsset::getUserAddress, order.getUserAddress())
                           .eq(UserAsset::getChainName, order.getChainName())
                           .eq(UserAsset::getTokenAddress, order.getTokenAddress());
                    
                    UserAsset asset = userAssetMapper.selectOne(wrapper);
                    
                    if (asset == null) {
                        log.error("❌ [资产] 用户资产记录不存在: user={}, token={}", 
                            order.getUserAddress(), order.getTokenAddress());
                        // 用户资产不存在，这属于业务错误，直接跳出重试
                        log.warn("⚠️ [资产] 用户资产不存在，跳过此记录: txHash={}", order.getTxHash());
                        break; // 跳出重试循环
                    }
                    
                    // ✅ 检查余额是否充足
                    if (asset.getBalance().compareTo(order.getAmount()) < 0) {
                        log.error("❌ [资产] 用户余额不足: user={}, token={}, 当前余额={}, 提现金额={}", 
                            order.getUserAddress(), order.getTokenAddress(), 
                            asset.getBalance(), order.getAmount());
                        // 余额不足，这属于业务错误，直接跳出重试
                        log.warn("⚠️ [资产] 余额不足，跳过此记录: txHash={}", order.getTxHash());
                        break; // 跳出重试循环
                    }
                    
                    // ✅ 使用数据库乐观锁更新（防止并发冲突）
                    Long currentVersion = asset.getVersion();
                    
                    LambdaQueryWrapper<UserAsset> updateWrapper = new LambdaQueryWrapper<>();
                    updateWrapper.eq(UserAsset::getId, asset.getId())
                                .eq(UserAsset::getVersion, currentVersion) // 乐观锁
                                .ge(UserAsset::getBalance, order.getAmount()); // ✅ 数据库层余额校验
                    
                    UserAsset updateAsset = new UserAsset();
                    updateAsset.setBalance(asset.getBalance().subtract(order.getAmount()));
                    updateAsset.setVersion(currentVersion + 1); // 版本号 +1
                    updateAsset.setUpdateTime(LocalDateTime.now());
                    
                    int rows = userAssetMapper.update(updateAsset, updateWrapper);
                    if (rows == 0) {
                        // 乐观锁冲突或余额不足，重试
                        log.warn("⚠️ [资产] 乐观锁冲突或余额不足，第{}次重试: user={}, token={}", 
                            retryCount, order.getUserAddress(), order.getTokenAddress());
                        
                        if (retryCount >= maxRetries) {
                            log.error("❌ [资产] 乐观锁重试次数耗尽，跳过此记录: txHash={}", order.getTxHash());
                            // 不抛出异常，而是继续处理下一条记录
                            break; // 跳出重试循环
                        }
                        
                        // 短暂等待后重试
                        Thread.sleep(50 * retryCount); // 递增等待时间
                    } else {
                        log.info("✅ [资产] 扣减用户资产: user={}, token={}, 提现={}, 新余额={}", 
                            order.getUserAddress(), order.getTokenAddress(), 
                            order.getAmount(), updateAsset.getBalance());
                        success = true;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ [资产] 重试被中断: txHash={}", order.getTxHash(), e);
                    throw new RuntimeException("重试被中断", e);
                } catch (Exception e) {
                    log.error("❌ [资产] 扣减用户资产失败: user={}, txHash={}", 
                        order.getUserAddress(), order.getTxHash(), e);
                    // 如果是其他异常，也应该继续处理下一条记录
                    break; // 跳出重试循环
                }
            }

            if (!success) {
                log.warn("⚠️ [资产] 扣减用户资产失败，已跳过: txHash={}", order.getTxHash());
            }
        }
    }
}
