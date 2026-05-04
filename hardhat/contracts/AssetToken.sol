// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts-upgradeable/token/ERC20/ERC20Upgradeable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";

/**
 * @title AssetToken
 * @notice 可升级的 ERC20 代币合约，用于模拟链上资产
 * @dev 支持 UUPS 代理模式，Owner 可以铸造和销毁代币
 */
contract AssetToken is
    Initializable,
    ERC20Upgradeable,
    UUPSUpgradeable,
    OwnableUpgradeable,
    AccessControlUpgradeable
{
    // 角色定义
    bytes32 public constant MINTER_ROLE = keccak256("MINTER_ROLE");
    bytes32 public constant BURNER_ROLE = keccak256("BURNER_ROLE");

    // 事件
    event TokensMinted(address indexed to, uint256 amount);
    event TokensBurned(address indexed from, uint256 amount);

    // ==============================================
    // 构造函数（禁用初始化）
    // @custom:oz-upgrades-unsafe-allow constructor
    // ==============================================
    constructor() {
        _disableInitializers();
    }

    // ==============================================
    // 初始化函数
    // ==============================================
    function initialize(
        string memory name,
        string memory symbol,
        uint256 initialSupply
    ) public initializer {
        __ERC20_init(name, symbol);
        __UUPSUpgradeable_init();
        __Ownable_init();
        __AccessControl_init();

        // 设置角色
        _transferOwnership(msg.sender);
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
        _grantRole(MINTER_ROLE, msg.sender);
        _grantRole(BURNER_ROLE, msg.sender);

        // 铸造初始供应量给部署者
        if (initialSupply > 0) {
            _mint(msg.sender, initialSupply);
        }
    }

    // ==============================================
    // 核心功能：铸造代币
    // ==============================================
    function mint(address to, uint256 amount) external onlyRole(MINTER_ROLE) {
        _mint(to, amount);
        emit TokensMinted(to, amount);
    }

    // ==============================================
    // 核心功能：销毁代币
    // ==============================================
    function burn(address from, uint256 amount) external onlyRole(BURNER_ROLE) {
        _burn(from, amount);
        emit TokensBurned(from, amount);
    }

    // ==============================================
    // UUPS 升级授权
    // ==============================================
    function _authorizeUpgrade(address newImplementation) internal override onlyOwner {}

    // ==============================================
    // 辅助函数：查询 decimals
    // ==============================================
    function decimals() public view virtual override returns (uint8) {
        return 18;
    }
}
