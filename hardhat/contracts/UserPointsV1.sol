// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/security/PausableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/security/ReentrancyGuardUpgradeable.sol";

contract UserPointsV1 is
    Initializable,
    UUPSUpgradeable,
    OwnableUpgradeable,
    AccessControlUpgradeable,
    PausableUpgradeable,
    ReentrancyGuardUpgradeable
{
    // 角色
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");

    // 数据结构
    struct UserPoint {
        uint256 totalPoints;
        uint256 lastUpdate;
    }

    mapping(address => UserPoint) public users;

    // 事件
    event PointsAdded(
        address indexed operator,
        address indexed user,
        uint256 points,
        uint256 newTotalPoints
    );

    event PointsReset(
        address indexed operator,
        address indexed user,
        uint256 oldPoints
    );

    event PointsSpent(
        address indexed operator,
        address indexed user,
        uint256 points,
        uint256 newTotalPoints
    );

    // ==============================================
    // 可升级合约固定写法：空构造 + 禁用初始化
    // @custom:oz-upgrades-unsafe-allow constructor
    // ==============================================
    constructor() {
        _disableInitializers();
    }

    // ==============================================
    // 可升级初始化方法（代替 constructor）
    // ==============================================
    function initialize() public initializer {
        __Ownable_init();
        __UUPSUpgradeable_init();
        __AccessControl_init();
        __Pausable_init();
        __ReentrancyGuard_init();

        // 设置所有者
        _transferOwnership(msg.sender);
        
        // 设置管理员角色
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(OPERATOR_ROLE, msg.sender);
    }

    // ==============================================
    // 你原来的业务逻辑 100% 完全保留
    // ==============================================
    function addPoints(
        address user,
        uint256 points
    ) external onlyRole(OPERATOR_ROLE) whenNotPaused nonReentrant {
        UserPoint storage u = users[user];
        u.totalPoints += points;
        u.lastUpdate = block.timestamp;
        emit PointsAdded(msg.sender, user, points, u.totalPoints);
    }

    // 查看用户积分
    function getUserPoints(address user) external view returns (uint256 totalPoints, uint256 lastUpdate) {
        UserPoint storage u = users[user];
        return (u.totalPoints, u.lastUpdate);
    }

    // 积分清零（仅操作员可调用）
    function resetPoints(
        address user
    ) external onlyRole(OPERATOR_ROLE) whenNotPaused nonReentrant {
        UserPoint storage u = users[user];
        uint256 oldPoints = u.totalPoints;
        u.totalPoints = 0;
        u.lastUpdate = block.timestamp;
        emit PointsReset(msg.sender, user, oldPoints);
    }

    // 消耗积分（仅操作员可调用）
    function spendPoints(
        address user,
        uint256 points
    ) external onlyRole(OPERATOR_ROLE) whenNotPaused nonReentrant {
        require(points > 0, "Points must be greater than 0");
        
        UserPoint storage u = users[user];
        require(u.totalPoints >= points, "Insufficient points");
        
        u.totalPoints -= points;
        u.lastUpdate = block.timestamp;
        emit PointsSpent(msg.sender, user, points, u.totalPoints);
    }

    // 暂停 / 解暂停
    function pause() external onlyOwner {
        _pause();
    }

    function unpause() external onlyOwner {
        _unpause();
    }

    // ==============================================
    // UUPS 升级权限（必需）
    // ==============================================
    function _authorizeUpgrade(address) internal override onlyOwner {}
}
