package com.web3.dto;

import lombok.Data;

/**
 * 提现事件消息 DTO - 用于 Kafka 传输
 */
@Data
public class WithdrawalEventMessage {
    
    /**
     * 链名称
     */
    private String chainName;
    
    /**
     * 交易哈希
     */
    private String txHash;
    
    /**
     * 用户地址
     */
    private String userAddress;
    
    /**
     * 代币合约地址
     */
    private String tokenAddress;
    
    /**
     * 提现金额（字符串格式，避免精度丢失）
     */
    private String amount;
    
    /**
     * 提现ID（合约中的 withdrawalId）
     */
    private String withdrawalId;
    
    /**
     * 区块号
     */
    private Long blockNumber;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}
