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
 * @title DepositVault
 * @notice 充值金库合约，管理用户资产充值
 * @dev 支持多种 ERC20 代币充值，记录充值日志
 */
contract DepositVault is
    Initializable,
    UUPSUpgradeable,
    OwnableUpgradeable,
    AccessControlUpgradeable,
    ReentrancyGuardUpgradeable
{
    using SafeERC20Upgradeable for IERC20Upgradeable;

    // 角色定义
    bytes32 public constant OPERATOR_ROLE = keccak256("OPERATOR_ROLE");

    // 数据结构
    struct DepositRecord {
        address user;
        address token;
        uint256 amount;
        uint256 timestamp;
        uint256 blockNumber;
        bool processed;
    }

    // 状态变量
    mapping(address => mapping(address => uint256)) public userBalances; // user => token => balance
    mapping(bytes32 => DepositRecord) public deposits; // depositId => record
    uint256 public depositCount;

    // 支持的代币列表
    mapping(address => bool) public supportedTokens;
    address[] public tokenList;

    // 事件
    event DepositReceived(
        bytes32 indexed depositId,
        address indexed user,
        address indexed token,
        uint256 amount
    );
    
    event DepositProcessed(bytes32 indexed depositId, bool success);
    
    event TokenAdded(address indexed token);
    
    event TokenRemoved(address indexed token);

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
    }

    // ==============================================
    // 核心功能：充值
    // ==============================================
    function deposit(
        address token,
        uint256 amount
    ) external nonReentrant whenSupportedToken(token) {
        require(amount > 0, "Amount must be greater than 0");
        
        // 转移代币到金库
        IERC20Upgradeable(token).safeTransferFrom(msg.sender, address(this), amount);
        
        // 更新用户余额
        userBalances[msg.sender][token] += amount;
        
        // 记录充值日志
        bytes32 depositId = keccak256(abi.encodePacked(msg.sender, token, amount, block.timestamp, depositCount));
        deposits[depositId] = DepositRecord({
            user: msg.sender,
            token: token,
            amount: amount,
            timestamp: block.timestamp,
            blockNumber: block.number,
            processed: true
        });
        depositCount++;
        
        emit DepositReceived(depositId, msg.sender, token, amount);
        emit DepositProcessed(depositId, true);
    }

    // ==============================================
    // 核心功能：批量充值
    // ==============================================
    function batchDeposit(
        address[] calldata tokens,
        uint256[] calldata amounts
    ) external nonReentrant {
        require(tokens.length == amounts.length, "Arrays length mismatch");
        
        for (uint256 i = 0; i < tokens.length; i++) {
            require(supportedTokens[tokens[i]], "Token not supported");
            require(amounts[i] > 0, "Amount must be greater than 0");
            
            IERC20Upgradeable(tokens[i]).safeTransferFrom(msg.sender, address(this), amounts[i]);
            userBalances[msg.sender][tokens[i]] += amounts[i];
            
            bytes32 depositId = keccak256(abi.encodePacked(msg.sender, tokens[i], amounts[i], block.timestamp, depositCount));
            deposits[depositId] = DepositRecord({
                user: msg.sender,
                token: tokens[i],
                amount: amounts[i],
                timestamp: block.timestamp,
                blockNumber: block.number,
                processed: true
            });
            depositCount++;
            
            emit DepositReceived(depositId, msg.sender, tokens[i], amounts[i]);
        }
    }

    // ==============================================
    // 管理功能：添加支持的代币
    // ==============================================
    function addSupportedToken(address token) external onlyRole(OPERATOR_ROLE) {
        require(!supportedTokens[token], "Token already supported");
        require(token != address(0), "Invalid token address");
        
        supportedTokens[token] = true;
        tokenList.push(token);
        
        emit TokenAdded(token);
    }

    // ==============================================
    // 管理功能：移除支持的代币
    // ==============================================
    function removeSupportedToken(address token) external onlyRole(OPERATOR_ROLE) {
        require(supportedTokens[token], "Token not supported");
        
        supportedTokens[token] = false;
        
        // 从列表中移除
        for (uint256 i = 0; i < tokenList.length; i++) {
            if (tokenList[i] == token) {
                tokenList[i] = tokenList[tokenList.length - 1];
                tokenList.pop();
                break;
            }
        }
        
        emit TokenRemoved(token);
    }

    // ==============================================
    // 查询功能：获取用户余额
    // ==============================================
    function getUserBalance(address user, address token) external view returns (uint256) {
        return userBalances[user][token];
    }

    // ==============================================
    // 查询功能：获取充值记录
    // ==============================================
    function getDepositRecord(bytes32 depositId) external view returns (DepositRecord memory) {
        return deposits[depositId];
    }

    // ==============================================
    // 查询功能：获取支持的代币列表
    // ==============================================
    function getSupportedTokens() external view returns (address[] memory) {
        return tokenList;
    }

    // ==============================================
    // 修饰符：检查代币是否支持
    // ==============================================
    modifier whenSupportedToken(address token) {
        require(supportedTokens[token], "Token not supported");
        _;
    }

    // ==============================================
    // UUPS 升级授权
    // ==============================================
    function _authorizeUpgrade(address) internal override onlyOwner {}
}
