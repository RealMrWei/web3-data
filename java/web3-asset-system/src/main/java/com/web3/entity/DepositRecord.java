package com.web3.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录实体类
 */
@Data
@TableName("deposit_record")
public class DepositRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易哈希
     */
    private String txHash;

    /**
     * 用户钱包地址
     */
    private String userAddress;

    /**
     * 链名称
     */
    private String chainName;

    /**
     * 代币合约地址
     */
    private String tokenAddress;

    /**
     * 充值金额
     */
    private BigDecimal amount;

    /**
     * 状态：0-待确认 1-成功 2-失败
     */
    private Integer status;

    /**
     * 区块号
     */
    private Long blockNumber;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
