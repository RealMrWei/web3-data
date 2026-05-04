# Web3 链上资产管理系统

## 项目简介

**Web3 链上资产管理系统** - 模拟交易所架构，支持多链资产充值、提现、事件监听的全栈解决方案。

**技术栈：**
- **智能合约**: Solidity ^0.8.20, OpenZeppelin, UUPS 代理模式
- **后端服务**: Spring Boot 2.7.x, MyBatis-Plus, Web3j
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **消息队列**: Kafka
- **分布式锁**: ShedLock + Redis
- **多链支持**: Ethereum, BNB Chain, Arbitrum（可扩展）
- **开发工具**: Hardhat v2.x, ethers.js v5.x

---

## 📋 项目进度（2026-05-03）

### ✅ 今日完成

#### 1. **智能合约部署与联调**
- ✅ 修复 AssetToken 代理合约初始化问题（`initialize` + `unsafeAllow.constructor`）
- ✅ 编写并执行 `scripts/add-token-to-vault.js`，解决代币支持列表缺失问题
- ✅ 编写并执行 `scripts/diagnose-deposit.js`，实现充值全流程自动化诊断
- ✅ 修复 Java 配置文件地址不一致导致的 `Token not supported` 错误
- ✅ 更新 `test.http` 中的合约地址，确保测试请求指向最新部署的合约

#### 2. **后端业务逻辑验证**
- ✅ 成功通过 Java API 调用 DepositVault 完成 100 ATK 充值
- ✅ 验证链上余额同步：Vault 余额增加，用户钱包余额减少
- ✅ 发现并记录多合约架构下的余额校验经验（DepositVault vs WithdrawalManager）

#### 3. **提现流程预研**
- ✅ 编写 `scripts/test-withdrawal.js` 脚本，理清提现前置条件
- ✅ 确认提现需要先调用 `depositToVault` 将资金转入 WithdrawalManager 内部账本

---

### ⏳ 待完成/优化模块

#### 1. **业务功能补全**
- [x] 创建正式的业务控制器：`DepositController.java`（充值查询、申请）
- [x] 创建正式的业务控制器：`WithdrawalController.java`（提现申请、订单跟踪）
- [x] 创建资产查询接口：`AssetController.java`（实时余额、历史记录）
- [x] 完善全局异常处理器 `GlobalExceptionHandler.java`
- [x] 创建合约服务封装层：`DepositService.java`、`WithdrawalService.java`
- [x] 创建资产服务封装层：`AssetManagementService.java`、`AssetQueryService.java`

#### 2. **数据持久层完善**
- [x] 创建 MyBatis-Plus Mapper 接口（6个实体对应的Mapper）
- [x] 实现 MetaObjectHandler 自动填充处理器（createTime, updateTime）

#### 3. **事件监听与消息处理**
- [x] 实现充值事件监听器：`DepositEventListener.java`
- [x] 实现提现事件监听器：`WithdrawalEventListener.java`
- [x] 实现充值事件消费者：`DepositEventConsumer.java`
- [x] 实现提现事件消费者：`WithdrawalEventConsumer.java`
- [x] 完善 Kafka 配置与批量消费机制

#### 4. **分布式锁与集群支持**
- [x] 配置 ShedLock + Redis 分布式锁防止重复处理
- [x] 为事件监听器添加 `@SchedulerLock` 注解

#### 5. **安全性与健壮性增强**
- [ ] 私钥管理：从硬编码迁移至环境变量或 KMS
- [ ] 幂等性处理：API 层防重提交（防止用户短时间内多次点击）
- [ ] 监控告警：交易重试次数超过阈值时发送告警

#### 6. **合约升级演练**
- [ ] 编写 UserPointsV2 升级合约
- [ ] 实现 UUPS 升级流程并验证数据保留

---

### 💡 遇到的挑战与解决方案（简历亮点）

#### 挑战 1：代理合约初始化陷阱
- **问题**：直接部署实现合约导致角色权限缺失，交易回滚。
- **解决**：深入理解 OpenZeppelin Upgrades 插件机制，使用 `deployProxy` 配合 `initializer` 参数，并在构造函数中添加 `@custom:oz-upgrades-unsafe-allow` 注释。

#### 挑战 2：多合约状态同步一致性
- **问题**：充值成功后提现仍报余额不足，混淆了链上实际余额与合约内部记账。
- **解决**：梳理资金流向，明确 DepositVault（入金池）与 WithdrawalManager（出金池）的职责边界，设计跨合约转账或内部账本同步方案。

#### 挑战 3：配置驱动的地址管理
- **问题**：Hardhat 每次重启生成新地址，导致 Java 应用频繁连接失效。
- **解决**：建立以 `config/address.json` 为唯一权威源的配置同步机制，并编写自动化脚本实现“部署即更新”。

---

**项目状态：** 🟢 核心链路已跑通，进入 API 完善阶段  
**最后更新：** 2026-05-03  
**今日进度：** 智能合约联调完成 ✅ | 充值功能闭环验证 ✅ | 提现逻辑梳理完成 ✅ | 事件监听器完成 ✅ | API接口开发完成 ✅

---

### 🎯 项目目标

#### 核心功能规划

| 模块 | 功能 | 状态 |
|------|------|------|
| **智能合约** | 积分管理、充值、提现、多签审批 | ✅ 已完成 |
| **后端服务** | 多链交互、事件监听、资产管理 | ✅ 已完成 |
| **事件监听** | 区块链事件同步、Kafka 异步处理 | ✅ 已完成 |
| **REST API** | 充值查询、提现申请、资产查询 | ✅ 已完成 |
| **分布式锁** | ShedLock + Redis 防止集群重复处理 | ✅ 已完成 |
| **幂等性设计** | txHash 去重、乐观锁更新 | ⏳ 待开发 |
| **安全增强** | 私钥管理、环境变量配置 | ⏳ 待开发 |

#### 技术架构要点

1. **多链支持**
   - 通过配置文件驱动不同链的配置（RPC、ChainID）
   - Web3j 封装支持多链并行监听与管理

2. **事件监听**
   - 根据 ABI 文件生成对应的 Java 类进行事件解析
   - 使用数据库记录最后处理的区块号，支持断点续传
   - Kafka 异步处理事件消息

3. **集群部署**
   - ShedLock + Redis 分布式锁防止定时任务重复执行
   - Redis 记录已处理的 txHash 实现消息去重
   - Kafka 消费者组统一，手动提交模式

4. **安全性保障**
   - 私钥管理（生产环境使用 KMS）
   - 重放攻击防护（txHash 去重）
   - Gas 优化策略（动态 Gas 价格估算）

---

## 🚀 快速开始

### 智能合约部分

#### 1. 环境准备

```bash
cd hardhat
npm install
```

#### 2. 编译合约

```bash
npx hardhat compile
```

#### 3. 本地测试（推荐）

```bash
# 终端1 - 启动本地节点
npx hardhat node

# 终端2 - 部署合约
npx hardhat run scripts/deploy-assets.js --network localhost

# 终端3 - 运行测试
npx hardhat run scripts/test-assets.js --network localhost
```

---

### Spring Boot 后端部分

#### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Kafka 2.8+

#### 2. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE web3_asset CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 表结构由 MyBatis-Plus 自动创建（ddl-auto: update）
```

#### 3. 配置合约地址

**方式一：使用自动脚本（推荐）**

```bash
# Windows
cd java\web3-asset-system
update-contract-address.bat

# Linux/Mac
cd java/web3-asset-system
chmod +x update-contract-address.sh
./update-contract-address.sh
```

**方式二：手动配置**

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
web3:
  contracts:
    asset-token: 0xYourAssetTokenAddress
    deposit-vault: 0xYourDepositVaultAddress
    withdrawal-manager: 0xYourWithdrawalManagerAddress
```

详细配置说明请参考：[CONFIG_GUIDE.md](java/web3-asset-system/CONFIG_GUIDE.md)

#### 4. 启动项目

```bash
cd java/web3-asset-system
mvn spring-boot:run
```

---

## 📁 项目结构

```
web3-data/
├── hardhat/                          # 智能合约项目
│   ├── contracts/
│   │   ├── UserPointsV1.sol          # 可升级积分合约
│   │   ├── AssetToken.sol            # ERC20 代币合约
│   │   ├── DepositVault.sol          # 充值金库合约
│   │   └── WithdrawalManager.sol     # 提现管理器合约
│   ├── scripts/
│   │   ├── deploy-v1.js              # 积分合约部署脚本
│   │   ├── deploy-assets.js          # 资产合约部署脚本
│   │   ├── test-points.js            # 积分合约测试
│   │   └── test-assets.js            # 资产合约测试
│   ├── hardhat.config.js             # Hardhat 配置
│   └── package.json
│
├── java/                             # Java 后端项目
│   └── web3-asset-system/            # Spring Boot 工程
│       ├── pom.xml                   # Maven 依赖配置
│       └── src/main/
│           ├── java/com/web3/
│           │   ├── Web3AssetApplication.java
│           │   ├── config/           # 配置类
│           │   │   ├── Web3Properties.java
│           │   │   ├── Web3jConfig.java
│           │   │   ├── RedisConfig.java
│           │   │   ├── KafkaConfig.java
│           │   │   └── ShedLockConfig.java
│           │   ├── chain/            # 多链管理
│           │   │   └── MultiChainManager.java
│           │   ├── entity/           # 数据库实体
│           │   │   ├── ChainConfig.java
│           │   │   ├── UserAsset.java
│           │   │   ├── DepositRecord.java
│           │   │   ├── WithdrawalOrder.java
│           │   │   └── EventListenerOffset.java
│           │   ├── mapper/           # MyBatis-Plus Mapper（待开发）
│           │   ├── service/          # 业务服务（待开发）
│           │   ├── controller/       # REST API（待开发）
│           │   ├── kafka/            # Kafka 消息（待开发）
│           │   └── listener/         # 事件监听器（待开发）
│           └── resources/
│               ├── application.yml   # 应用配置
│               └── abi/              # 合约 ABI（待生成）
│
├── config/                           # 配置文件
│   └── address.json                  # 合约地址配置（自动生成）
└── README.md                         # 项目文档
```

---

## 🔧 技术要点

### 1. 智能合约

#### UUPS 代理模式
- 使用 `UUPSUpgradeable` 实现合约升级
- 通过 `_authorizeUpgrade()` 控制升级权限
- 部署时使用 `upgrades.deployProxy()`

#### 初始化规范（OpenZeppelin v5.x）
```
function initialize() public initializer {
    __Ownable_init();  // 不再接受参数
    _transferOwnership(msg.sender);  // 手动设置 owner
    // ... 其他初始化
}
```

#### 构造函数处理
```
// @custom:oz-upgrades-unsafe-allow constructor
constructor() {
    _disableInitializers();
}
```

---

### 2. Spring Boot 后端

#### MyBatis-Plus 优势
- ✅ 内置 CRUD，无需写 XML
- ✅ LambdaQueryWrapper 支持类型安全查询
- ✅ 自动填充字段（createTime, updateTime）
- ✅ 内置分页插件
- ✅ 乐观锁支持（@Version）

#### 示例代码
```
// 条件查询
LambdaQueryWrapper<UserAsset> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(UserAsset::getChainName, "ethereum")
       .eq(UserAsset::getUserAddress, userAddress);
List<UserAsset> assets = userAssetMapper.selectList(wrapper);

// 分页查询
Page<DepositRecord> page = new Page<>(1, 10);
depositRecordMapper.selectPage(page, wrapper);
```

---

### 3. 分布式锁（集群部署）

#### ShedLock 配置
```
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
}
```

#### 使用示例
```
@Scheduled(cron = "0 */1 * * * ?")
@SchedulerLock(name = "syncBlockEvents", 
               lockAtLeastFor = "50s",
               lockAtMostFor = "2m")
public void syncBlockEvents() {
    // 确保同一时间只有一个节点执行
}
```

---

### 4. 事件监听与幂等性

#### 偏移量管理
```
// 数据库记录最后处理的区块
UPDATE event_listener_offset 
SET last_processed_block = ?, version = version + 1
WHERE chain_name = ? AND version = ?;  // 乐观锁
```

#### Kafka 消息去重
```
// Redis 记录已处理的 txHash
if (redisTemplate.hasKey("processed:deposit:" + txHash)) {
    return;  // 跳过重复消息
}
redisTemplate.opsForValue().set(
    "processed:deposit:" + txHash, "1", 24, TimeUnit.HOURS
);
```

---

## 📝 明日计划

### 后端开发任务

- [ ] 完善提现申请业务逻辑（WithdrawalController -> WithdrawalService）
- [ ] 实现提现审批流程（审批状态变更、通知等）
- [ ] 测试后端充值功能与事件监听
- [ ] 测试后端提现功能与事件监听
- [ ] 完善 REST API 测试用例
- [ ] 生成合约 ABI Java 类（Web3j 代码生成）

### 智能合约任务

- [ ] 编写 UserPointsV2 升级合约（添加新功能）
- [ ] 实现合约升级流程
- [ ] 验证升级后数据保留

---

## 🎯 学习收获

### 核心技术掌握

#### 智能合约
1. **OpenZeppelin 可升级合约** - UUPS 代理模式实践
2. **权限管理** - Ownable + AccessControl 组合使用
3. **安全防护** - Pausable + ReentrancyGuard
4. **Hardhat 工作流** - 编译、部署、测试全流程

#### 后端开发
1. **Spring Boot 项目搭建** - 多模块配置管理
2. **MyBatis-Plus ORM** - 高效数据访问层
3. **Web3j 多链封装** - 配置驱动的链管理
4. **分布式系统设计** - ShedLock + Kafka + Redis

### 遇到的问题与解决

1. **OpenZeppelin v5.x API 变更** - `__Ownable_init()` 不再接受参数
2. **升级安全检查** - 构造函数需添加安全注释
3. **测试网 Gas 问题** - 优先使用本地网络测试
4. **Pending 交易阻塞** - 提高 Gas 价格或等待确认
5. **ORM 选型** - MyBatis-Plus vs JPA，选择前者更适合 Web3 场景
6. **集群部署** - 引入 ShedLock 分布式锁防止重复处理

---

## 📚 参考资料

- [OpenZeppelin Upgrades](https://docs.openzeppelin.com/upgrades-plugins/1.x/)
- [Hardhat Documentation](https://hardhat.org/docs)
- [ethers.js v5 Documentation](https://docs.ethers.io/v5/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MyBatis-Plus Documentation](https://baomidou.com/)
- [Web3j Documentation](https://docs.web3j.io/)
- [ShedLock Documentation](https://github.com/lukas-krecan/ShedLock)

---

**项目状态：** 🟢 进行中  
**最后更新：** 2026-04-30  
**今日进度：** 智能合约开发完成 ✅ | Spring Boot 项目搭建完成 ✅ | 核心配置类创建完成 ✅