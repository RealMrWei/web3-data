# Web3 资产管理系统 - 核心服务说明

## 📋 目录结构

```
src/main/java/com/web3/service/
├── BaseContractService.java      # 多链合约交互基类
├── DepositService.java           # 充值服务
├── WithdrawalService.java        # 提现服务
├── AssetQueryService.java        # 资产查询服务
├── EventListenerService.java     # 事件监听服务
└── AssetManagementService.java   # 统一资产管理服务（Facade）
```

## 🎯 核心服务介绍

### 1. BaseContractService（基类）

**功能**：提供通用的合约交互能力
- ✅ 合约只读方法调用（eth_call）
- ✅ 已签名交易发送
- ✅ ERC20 approve/transfer 函数编码
- ✅ ERC20 余额查询
- ✅ 代币精度查询

**使用示例**：
```java
@Service
public class MyService extends BaseContractService {
    public MyService(MultiChainManager multiChainManager) {
        super(multiChainManager);
    }
    
    // 继承所有基础方法
}
```

---

### 2. DepositService（充值服务）

**功能**：处理用户充值操作

#### 主要方法：

| 方法 | 说明 | 参数 |
|------|------|------|
| `getUserBalance()` | 查询用户余额 | chainName, userAddress |
| `depositNative()` | 原生币充值 | chainName, fromAddress, privateKey, amount |
| `depositERC20()` | ERC20代币充值 | chainName, tokenAddress, fromAddress, privateKey, amount |
| `batchDeposit()` | 批量充值 | chainName, fromAddress, privateKey, deposits |

**使用示例**：
```java
// 原生币充值
String txHash = depositService.depositNative(
    "ethereum",
    "0xUserAddress...",
    "0xPrivateKey...",
    new BigInteger("1000000000000000000") // 1 ETH in wei
);

// ERC20充值
String txHash = depositService.depositERC20(
    "ethereum",
    "0xTokenAddress...",
    "0xUserAddress...",
    "0xPrivateKey...",
    new BigInteger("1000000000000000000") // 1 Token
);
```

---

### 3. WithdrawalService（提现服务）

**功能**：处理用户提现请求和执行

#### 主要方法：

| 方法 | 说明 | 参数 |
|------|------|------|
| `requestNativeWithdrawal()` | 申请原生币提现 | chainName, userAddress, amount |
| `requestERC20Withdrawal()` | 申请ERC20提现 | chainName, tokenAddress, userAddress, amount |
| `executeWithdrawal()` | 执行提现（管理员） | chainName, recipient, privateKey, amount, isNative, tokenAddress |
| `batchExecuteWithdrawals()` | 批量执行提现 | chainName, privateKey, withdrawals |
| `getWithdrawableBalance()` | 查询可提现余额 | chainName, userAddress |

**使用示例**：
```java
// 申请提现
String requestId = withdrawalService.requestNativeWithdrawal(
    "ethereum",
    "0xUserAddress...",
    new BigInteger("500000000000000000") // 0.5 ETH
);

// 执行提现（需要管理员权限）
String txHash = withdrawalService.executeWithdrawal(
    "ethereum",
    "0xRecipientAddress...",
    "0xAdminPrivateKey...",
    new BigInteger("500000000000000000"),
    true,  // isNative
    null   // tokenAddress
);
```

---

### 4. AssetQueryService（资产查询服务）

**功能**：提供多维度资产查询

#### 主要方法：

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `getTotalBalance()` | 查询用户所有链的总资产 | Map<String, BigInteger> |
| `getUserBalanceOnChain()` | 查询单链余额 | BigInteger |
| `getERC20TokenBalance()` | 查询ERC20代币余额 | BigInteger |
| `getMultipleTokenBalances()` | 查询多个代币余额 | Map<String, BigInteger> |
| `getTotalDeposited()` | 查询金库总锁仓量 | BigInteger |
| `getDepositHistory()` | 查询充值历史 | List<DepositRecord> |
| `getWithdrawalHistory()` | 查询提现历史 | List<WithdrawalRecord> |

**使用示例**：
```java
// 查询用户所有链的余额
Map<String, BigInteger> balances = assetQueryService.getTotalBalance("0xUserAddress...");
// 返回: {"ethereum": 1000000000000000000, "bnb": 2000000000000000000}

// 查询特定代币余额
BigInteger balance = assetQueryService.getERC20TokenBalance(
    "ethereum",
    "0xTokenAddress...",
    "0xUserAddress..."
);
```

---

### 5. EventListenerService（事件监听服务）

**功能**：监听链上事件并同步状态

#### 主要方法：

| 方法 | 说明 | 参数 |
|------|------|------|
| `listenDepositEvents()` | 监听充值事件 | chainName, fromBlock, toBlock |
| `listenWithdrawalEvents()` | 监听提现事件 | chainName, fromBlock, toBlock |
| `startContinuousListening()` | 启动持续监听 | chainName, lastBlock, pollInterval |

**使用示例**：
```java
// 监听指定区块范围的事件
List<DepositEvent> deposits = eventListenerService.listenDepositEvents(
    "ethereum",
    BigInteger.valueOf(18000000),  // fromBlock
    BigInteger.valueOf(18000100)   // toBlock
);

// 启动持续监听（后台线程）
eventListenerService.startContinuousListening(
    "ethereum",
    BigInteger.valueOf(18000000),  // 起始区块
    15000                          // 轮询间隔 15秒
);
```

---

### 6. AssetManagementService（统一管理服务）

**功能**：整合所有服务，提供高层次 API（Facade 模式）

#### 主要功能分类：

**充值相关**：
- `depositNative()` - 原生币充值
- `depositERC20()` - ERC20充值
- `batchDeposit()` - 批量充值

**提现相关**：
- `requestNativeWithdrawal()` - 申请原生币提现
- `requestERC20Withdrawal()` - 申请ERC20提现
- `executeWithdrawal()` - 执行提现
- `batchExecuteWithdrawals()` - 批量执行提现

**查询相关**：
- `getTotalBalance()` - 查询总资产
- `getBalanceOnChain()` - 查询单链余额
- `getTokenBalance()` - 查询代币余额
- `getDepositHistory()` - 查询充值历史
- `getWithdrawalHistory()` - 查询提现历史

**事件监听**：
- `listenDeposits()` - 监听充值事件
- `listenWithdrawals()` - 监听提现事件
- `startContinuousListening()` - 启动持续监听

**使用示例**：
```java
@Autowired
private AssetManagementService assetManagementService;

// 充值
String txHash = assetManagementService.depositNative(
    "ethereum", "0xUser...", "0xKey...", new BigInteger("1000000000000000000")
);

// 查询余额
Map<String, BigInteger> balances = assetManagementService.getTotalBalance("0xUser...");

// 申请提现
String requestId = assetManagementService.requestNativeWithdrawal(
    "ethereum", "0xUser...", new BigInteger("500000000000000000")
);
```

---

## 🔧 REST API 接口

### 充值接口

#### 1. 原生币充值
```http
POST /api/assets/deposit/native
Content-Type: application/json

{
  "chainName": "ethereum",
  "fromAddress": "0xUserAddress...",
  "privateKey": "0xPrivateKey...",
  "amount": "1000000000000000000"
}
```

#### 2. ERC20代币充值
```http
POST /api/assets/deposit/erc20
Content-Type: application/json

{
  "chainName": "ethereum",
  "tokenAddress": "0xTokenAddress...",
  "fromAddress": "0xUserAddress...",
  "privateKey": "0xPrivateKey...",
  "amount": "1000000000000000000"
}
```

### 提现接口

#### 1. 申请提现
```http
POST /api/assets/withdrawals
Content-Type: application/json

{
  "chainName": "ethereum",
  "userAddress": "0xUserAddress...",
  "recipient": "0xRecipientAddress...",
  "amount": "500000000000000000",
  "tokenAddress": null  // null表示原生币
}
```

### 查询接口

#### 1. 查询总资产
```http
GET /api/assets/total-balance?userAddress=0xUserAddress...
```

#### 2. 查询单链余额
```http
GET /api/assets/balance?chainName=ethereum&userAddress=0xUserAddress...
```

#### 3. 查询充值历史
```http
GET /api/assets/deposits?chainName=ethereum&userAddress=0xUserAddress&pageNum=1&pageSize=10
```

#### 4. 查询提现历史
```http
GET /api/assets/withdrawals?userAddress=0xUserAddress&pageNum=1&pageSize=10
```

---

## ⚙️ 配置说明

### application.yml 配置示例

```yaml
web3:
  # 私钥（生产环境应使用 KMS）
  private-key: "0xYourPrivateKey..."
  
  # 合约地址
  contracts:
    asset-token: "0xAssetTokenAddress..."
    deposit-vault: "0xDepositVaultAddress..."
    withdrawal-manager: "0xWithdrawalManagerAddress..."
  
  # Gas配置
  gas:
    gas-limit: 300000
    max-fee-per-gas: 50000000000  # 50 gwei
    max-priority-fee-per-gas: 1500000000  # 1.5 gwei
  
  # 多链配置
  chains:
    - name: ethereum
      rpc-url: "https://mainnet.infura.io/v3/YOUR_PROJECT_ID"
      chain-id: 1
      native-currency: ETH
      enabled: true
    
    - name: bnb
      rpc-url: "https://bsc-dataseed.binance.org/"
      chain-id: 56
      native-currency: BNB
      enabled: true
    
    - name: arbitrum
      rpc-url: "https://arb1.arbitrum.io/rpc"
      chain-id: 42161
      native-currency: ETH
      enabled: true
```

---

## 🚀 快速开始

### 1. 启动应用
```bash
cd java/web3-asset-system
mvn spring-boot:run
```

### 2. 测试充值
```bash
curl -X POST http://localhost:8080/api/assets/deposit/native \
  -H "Content-Type: application/json" \
  -d '{
    "chainName": "ethereum",
    "fromAddress": "0xUserAddress...",
    "privateKey": "0xPrivateKey...",
    "amount": "1000000000000000000"
  }'
```

### 3. 查询余额
```bash
curl http://localhost:8080/api/assets/total-balance?userAddress=0xUserAddress...
```

---

## 📝 注意事项

### 安全性
1. **私钥管理**：生产环境必须使用 KMS（密钥管理服务），不要硬编码私钥
2. **权限控制**：提现执行需要管理员权限，应添加身份验证
3. **金额校验**：前端传入的金额需要进行有效性校验

### 性能优化
1. **批量操作**：支持批量充值和提现，减少交易次数
2. **事件监听**：使用轮询方式监听事件，避免频繁 RPC 调用
3. **缓存策略**：余额查询可以加入 Redis 缓存

### 错误处理
1. **交易失败**：捕获异常并记录日志，返回友好的错误信息
2. **重试机制**：对于网络超时等临时错误，可以实现自动重试
3. **事务一致性**：数据库操作和链上交易需要保证一致性

---

## 🔗 相关文档

- [Web3j 官方文档](https://docs.web3j.io/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [Hardhat 文档](https://hardhat.org/)
- [OpenZeppelin 文档](https://docs.openzeppelin.com/)

---

## 📞 技术支持

如有问题，请查看：
1. 应用日志：`logs/web3-asset.log`
2. 合约 ABI 文件：`src/main/resources/abi/*.abi.json`
3. 配置文件：`src/main/resources/application.yml`
