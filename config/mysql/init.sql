-- Web3 资产管理系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS web3_asset CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE web3_asset;

-- ============================================
-- 1. 链配置表
-- ============================================
CREATE TABLE IF NOT EXISTS chain_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_name VARCHAR(50) NOT NULL UNIQUE COMMENT '链名称：ethereum, bnb, arbitrum',
    rpc_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT 'RPC 节点地址',
    chain_id BIGINT NOT NULL COMMENT '链 ID',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_chain_name (chain_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='链配置表';

-- ============================================
-- 2. 用户资产表
-- ============================================
CREATE TABLE IF NOT EXISTS user_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_address VARCHAR(100) NOT NULL COMMENT '用户钱包地址',
    chain_name VARCHAR(50) NOT NULL COMMENT '链名称',
    token_address VARCHAR(100) NOT NULL DEFAULT '' COMMENT '代币合约地址',
    balance DECIMAL(65, 0) DEFAULT 0 COMMENT '链上余额',
    pending_balance DECIMAL(65, 0) DEFAULT 0 COMMENT '待确认余额（充值中）',
    version BIGINT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_chain_token (user_address, chain_name, token_address),
    INDEX idx_user_address (user_address),
    INDEX idx_chain_name (chain_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资产表';

-- ============================================
-- 3. 事件监听偏移量表
-- ============================================
CREATE TABLE IF NOT EXISTS event_listener_offset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_name VARCHAR(50) NOT NULL COMMENT '链名称',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型（DEPOSIT/WITHDRAWAL）',
    contract_address VARCHAR(100) NOT NULL DEFAULT '' COMMENT '合约地址',
    event_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '事件名称',
    last_processed_block BIGINT DEFAULT 0 COMMENT '最后处理的区块号',
    last_processed_tx VARCHAR(100) DEFAULT NULL COMMENT '最后处理的交易哈希',
    version BIGINT DEFAULT 0 COMMENT '乐观锁版本号',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_chain_event (chain_name, event_type, contract_address, event_name),
    INDEX idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件监听偏移量表';

-- ============================================
-- 4. 充值记录表
-- ============================================
CREATE TABLE IF NOT EXISTS deposit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_hash VARCHAR(100) NOT NULL UNIQUE COMMENT '交易哈希',
    chain_name VARCHAR(50) NOT NULL COMMENT '链名称',
    user_address VARCHAR(100) NOT NULL COMMENT '用户地址',
    token_address VARCHAR(100) NOT NULL COMMENT '代币地址',
    amount DECIMAL(65, 0) NOT NULL COMMENT '充值金额',
    block_number BIGINT NOT NULL COMMENT '区块号',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已处理，2-失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_address (user_address),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值记录表';

-- ============================================
-- 5. 提现订单表（业务订单）
-- ============================================
CREATE TABLE IF NOT EXISTS withdrawal_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(100) NOT NULL UNIQUE COMMENT '订单号',
    user_address VARCHAR(100) NOT NULL COMMENT '用户钱包地址',
    recipient VARCHAR(100) NOT NULL DEFAULT '' COMMENT '接收地址（提现目标地址）',
    chain_name VARCHAR(50) NOT NULL COMMENT '链名称',
    token_address VARCHAR(100) NOT NULL DEFAULT '' COMMENT '代币合约地址',
    amount DECIMAL(65, 0) NOT NULL COMMENT '提现金额',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待审批 1-已批准 2-已执行 3-已拒绝',
    tx_hash VARCHAR(100) DEFAULT NULL COMMENT '交易哈希（执行后）',
    approver_address VARCHAR(100) DEFAULT NULL COMMENT '审批人地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_address (user_address),
    INDEX idx_status (status),
    INDEX idx_order_no (order_no),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现订单表';

-- ============================================
-- 6. 提现记录表（链上事件）
-- ============================================
CREATE TABLE IF NOT EXISTS withdrawal_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tx_hash VARCHAR(100) NOT NULL UNIQUE COMMENT '交易哈希',
    chain_name VARCHAR(50) NOT NULL COMMENT '链名称',
    user_address VARCHAR(100) NOT NULL COMMENT '用户地址',
    token_address VARCHAR(100) NOT NULL COMMENT '代币地址',
    amount DECIMAL(65, 0) NOT NULL COMMENT '提现金额',
    block_number BIGINT NOT NULL COMMENT '区块号',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-已处理，2-失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_address (user_address),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提现记录表';

-- ============================================
-- 7. ShedLock 分布式锁表
-- ============================================
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    INDEX idx_lock_until (lock_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ShedLock 分布式锁表';

-- ============================================
-- 8. 合约地址配置表
-- ============================================
CREATE TABLE IF NOT EXISTS contract_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain_name VARCHAR(50) NOT NULL COMMENT '链名称：localhost, bnb, arbitrum',
    contract_type VARCHAR(50) NOT NULL COMMENT '合约类型：deposit_vault, withdrawal_manager, asset_token',
    contract_address VARCHAR(100) NOT NULL DEFAULT '' COMMENT '合约地址',
    abi_version VARCHAR(20) DEFAULT '1.0' COMMENT 'ABI 版本',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_chain_type (chain_name, contract_type),
    INDEX idx_chain_name (chain_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合约地址配置表';

-- ============================================
-- 9. 插入初始数据
-- ============================================

-- 插入本地测试链配置
INSERT INTO chain_config (chain_name, rpc_url, chain_id, status) 
VALUES ('localhost', 'http://127.0.0.1:8545', 31337, 1)
ON DUPLICATE KEY UPDATE rpc_url = VALUES(rpc_url), chain_id = VALUES(chain_id);

-- ============================================
-- 10. 合约地址配置表（可选，部署合约后再插入）
-- ============================================
-- 注意：这些是示例数据，实际使用时需要替换为真实部署的合约地址
-- INSERT INTO contract_address (chain_name, contract_type, contract_address, status) 
-- VALUES 
-- ('localhost', 'deposit_vault', '0x5FbDB2315678afecb367f032d93F642f64180aa3', 1),
-- ('localhost', 'withdrawal_manager', '0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512', 1),
-- ('localhost', 'asset_token', '0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0', 1);
