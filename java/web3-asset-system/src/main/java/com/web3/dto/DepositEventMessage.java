package com.web3.dto;

import lombok.Data;

/**
 * 充值事件消息 DTO - 用于 Kafka 传输
 */
@Data
public class DepositEventMessage {
    
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
     * 充值金额（字符串格式，避免精度丢失）
     */
    private String amount;
    
    /**
     * 区块号
     */
    private Long blockNumber;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}
