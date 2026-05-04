package com.web3.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件监听偏移量实体类 - 用于断点续传
 */
@Data
@TableName("event_listener_offset")
public class EventListenerOffset {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链名称
     */
    private String chainName;

    /**
     * 事件类型（DEPOSIT/WITHDRAWAL）
     */
    private String eventType;

    /**
     * 合约地址
     */
    private String contractAddress;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 最后处理的区块号
     */
    private Long lastProcessedBlock;

    /**
     * 最后处理的交易哈希
     */
    private String lastProcessedTx;

    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
