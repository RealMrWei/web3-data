// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts-upgradeable/token/ERC20/IERC20Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/token/ERC20/utils/SafeERC20Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/security/ReentrancyGuardUpgradeable.sol";

/**
 * @title WithdrawalManager
 * @notice 提现管理合约，处理用户提现请求
 * @dev 支持提现审批、批量提现、提现限额等安全机制
 */
contract WithdrawalManager is
    Initializable,
    UUPSUpgradeable,
    OwnableUpgradeable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable
{
    using SafeERC20Upgradeable for IERC20Upgradeable;

    // 角色定义
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");
    bytes32 public constant APPROVER_ROLE = keccak256("APPROVER_ROLE");

    // 提现状态
    enum WithdrawalStatus {
        Pending,    // 待审批
        Approved,   // 已批准
        Rejected,   // 已拒绝
        Completed   // 已完成
    }

    // 提现记录
    struct WithdrawalRecord {
        address user;
        address token;
        uint256 amount;
        uint256 timestamp;
        WithdrawalStatus status;
        address approver;
        uint256 completedAt;
    }

    // 状态变量
    mapping(address => mapping(address => uint256)) public userBalances; // user => token => balance
    mapping(uint256 => WithdrawalRecord) public withdrawals; // withdrawalId => record
    uint256 public withdrawalCount;
    
    // 每日提现限额（每个用户）
    mapping(address => mapping(address => uint256)) public dailyWithdrawals; // user => token => amount
    mapping(address => uint256) public lastWithdrawalDate; // user => timestamp
    uint256 public dailyLimit; // 每日限额（在 initialize 中设置）
    
    // 事件
    event WithdrawalRequested(
        uint256 indexed withdrawalId,
        address indexed user,
        address indexed token,
        uint256 amount
    );
    
    event WithdrawalApproved(uint256 indexed withdrawalId, address indexed approver);
    
    event WithdrawalRejected(uint256 indexed withdrawalId, address indexed approver);
    
    event WithdrawalCompleted(uint256 indexed withdrawalId, uint256 timestamp);

    // ==============================================
    // 构造函数
    // @custom:oz-upgrades-unsafe-allow constructor
    // ==============================================
    constructor() {
        _disableInitializers();
    }

    // ==============================================
    // 初始化
    // ==============================================
    function initialize() public initializer {
        __UUPSUpgradeable_init();
        __Ownable_init();
        __AccessControl_init();
        __ReentrancyGuard_init();

        _transferOwnership(msg.sender);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(OPERATOR_ROLE, msg.sender);
        _grantRole(APPROVER_ROLE, msg.sender);
        
        // 设置默认每日限额
        dailyLimit = 10000 ether;
    }

    // ==============================================
    // 核心功能：请求提现
    // ==============================================
    function requestWithdrawal(
        address token,
        uint256 amount
    ) external nonReentrant {
        require(amount > 0, "Amount must be greater than 0");
        require(userBalances[msg.sender][token] >= amount, "Insufficient balance");
        require(_checkDailyLimit(msg.sender, token, amount), "Exceeds daily limit");
        
        // 创建提现记录
        uint256 withdrawalId = withdrawalCount++;
        withdrawals[withdrawalId] = WithdrawalRecord({
            user: msg.sender,
            token: token,
            amount: amount,
            timestamp: block.timestamp,
            status: WithdrawalStatus.Pending,
            approver: address(0),
            completedAt: 0
        });
        
        // 更新每日提现记录
        _updateDailyWithdrawal(msg.sender, token, amount);
        
        emit WithdrawalRequested(withdrawalId, msg.sender, token, amount);
    }

    // ==============================================
    // 核心功能：审批提现（Operator）
    // ==============================================
    function approveWithdrawal(uint256 withdrawalId) external onlyRole(APPROVER_ROLE) nonReentrant {
        WithdrawalRecord storage record = withdrawals[withdrawalId];
        require(record.status == WithdrawalStatus.Pending, "Withdrawal not pending");
        
        record.status = WithdrawalStatus.Approved;
        record.approver = msg.sender;
        
        emit WithdrawalApproved(withdrawalId, msg.sender);
    }

    // ==============================================
    // 核心功能：执行提现（Operator）
    // ==============================================
    function executeWithdrawal(uint256 withdrawalId) external onlyRole(OPERATOR_ROLE) nonReentrant {
        WithdrawalRecord storage record = withdrawals[withdrawalId];
        require(record.status == WithdrawalStatus.Approved, "Withdrawal not approved");
        
        // 扣除用户余额
        userBalances[record.user][record.token] -= record.amount;
        
        // 转移代币给用户
        IERC20Upgradeable(record.token).safeTransfer(record.user, record.amount);
        
        // 更新状态
        record.status = WithdrawalStatus.Completed;
        record.completedAt = block.timestamp;
        
        emit WithdrawalCompleted(withdrawalId, block.timestamp);
    }

    // ==============================================
    // 核心功能：拒绝提现
    // ==============================================
    function rejectWithdrawal(uint256 withdrawalId) external onlyRole(APPROVER_ROLE) {
        WithdrawalRecord storage record = withdrawals[withdrawalId];
        require(record.status == WithdrawalStatus.Pending, "Withdrawal not pending");
        
        record.status = WithdrawalStatus.Rejected;
        record.approver = msg.sender;
        
        emit WithdrawalRejected(withdrawalId, msg.sender);
    }

    // ==============================================
    // 管理功能：设置每日限额
    // ==============================================
    function setDailyLimit(uint256 newLimit) external onlyOwner {
        dailyLimit = newLimit;
    }

    // ==============================================
    // 辅助功能：充值到提现合约
    // ==============================================
    function depositToVault(address token, uint256 amount) external {
        require(amount > 0, "Amount must be greater than 0");
        IERC20Upgradeable(token).safeTransferFrom(msg.sender, address(this), amount);
        userBalances[msg.sender][token] += amount;
    }

    // ==============================================
    // 查询功能：获取提现记录
    // ==============================================
    function getWithdrawalRecord(uint256 withdrawalId) external view returns (WithdrawalRecord memory) {
        return withdrawals[withdrawalId];
    }

    // ==============================================
    // 查询功能：获取用户余额
    // ==============================================
    function getUserBalance(address user, address token) external view returns (uint256) {
        return userBalances[user][token];
    }

    // ==============================================
    // 查询功能：获取今日已提现金额
    // ==============================================
    function getTodayWithdrawal(address user, address token) external view returns (uint256) {
        uint256 today = _getToday();
        
        // 如果是新的一天，返回 0
        if (lastWithdrawalDate[user] != today) {
            return 0;
        }
        
        return dailyWithdrawals[user][token];
    }

    // ==============================================
    // 内部函数：检查每日限额
    // ==============================================
    function _checkDailyLimit(address user, address token, uint256 amount) internal view returns (bool) {
        uint256 today = _getToday();
        
        // 如果是新的一天，重置计数
        if (lastWithdrawalDate[user] != today) {
            return amount <= dailyLimit;
        }
        
        return (dailyWithdrawals[user][token] + amount) <= dailyLimit;
    }

    // ==============================================
    // 内部函数：更新每日提现记录
    // ==============================================
    function _updateDailyWithdrawal(address user, address token, uint256 amount) internal {
        uint256 today = _getToday();
        
        if (lastWithdrawalDate[user] != today) {
            lastWithdrawalDate[user] = today;
            // 新的一天，不需要清除，查询时会自动判断日期
        }
        
        dailyWithdrawals[user][token] += amount;
    }

    // ==============================================
    // 内部函数：获取今天的日期（天级别）
    // ==============================================
    function _getToday() internal view returns (uint256) {
        return block.timestamp / 1 days;
    }

    // ==============================================
    // UUPS 升级授权
    // ==============================================
    function _authorizeUpgrade(address) internal override onlyOwner {}
}
