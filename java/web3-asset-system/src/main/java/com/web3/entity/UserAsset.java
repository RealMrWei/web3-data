package com.web3.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户资产实体类
 */
@Data
@TableName("user_asset")
public class UserAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 链上余额
     */
    private BigDecimal balance;

    /**
     * 待确认余额（充值中）
     */
    private BigDecimal pendingBalance;

    /**
     * 乐观锁版本号
     */
    // @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
