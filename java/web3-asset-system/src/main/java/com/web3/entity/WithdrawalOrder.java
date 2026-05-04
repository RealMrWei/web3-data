package com.web3.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现订单实体类
 */
@Data
@TableName("withdrawal_order")
public class WithdrawalOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户钱包地址
     */
    private String userAddress;

    /**
     * 接收地址（提现目标地址）
     */
    private String recipient;

    /**
     * 链名称
     */
    private String chainName;

    /**
     * 代币合约地址
     */
    private String tokenAddress;

    /**
     * 提现金额
     */
    private BigDecimal amount;

    /**
     * 状态：0-待审批 1-已批准 2-已执行 3-已拒绝
     */
    private Integer status;

    /**
     * 交易哈希（执行后）
     */
    private String txHash;

    /**
     * 审批人地址
     */
    private String approverAddress;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime completedTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
