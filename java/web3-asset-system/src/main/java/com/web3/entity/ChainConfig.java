package com.web3.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链配置实体类
 */
@Data
@TableName("chain_config")
public class ChainConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链名称：ethereum, bnb, arbitrum
     */
    private String chainName;

    /**
     * RPC 节点地址
     */
    private String rpcUrl;

    /**
     * 链 ID
     */
    private Long chainId;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
