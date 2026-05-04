package com.web3.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 合约地址配置实体类
 */
@Data
@TableName("contract_address")
public class ContractAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链名称：localhost, bnb, arbitrum
     */
    private String chainName;

    /**
     * 合约类型：deposit_vault, withdrawal_manager, asset_token
     */
    private String contractType;

    /**
     * 合约地址
     */
    private String contractAddress;

    /**
     * ABI 版本
     */
    private String abiVersion;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
