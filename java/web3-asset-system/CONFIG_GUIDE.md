# 合约地址配置指南

## 📋 目录

1. [快速开始](#快速开始)
2. [配置方式](#配置方式)
3. [获取合约地址](#获取合约地址)
4. [多链配置](#多链配置)
5. [环境变量配置](#环境变量配置)
6. [常见问题](#常见问题)

---

## 🚀 快速开始

### 方式一：使用自动脚本（推荐）

#### Windows 用户：
```bash
# 1. 部署合约到本地测试网
cd d:\study\web3-data\hardhat
npx hardhat node                    # 启动本地节点（新终端）
npx hardhat run scripts/deploy-assets.js --network localhost

# 2. 更新 Java 项目配置
cd ..\java\web3-asset-system
update-contract-address.bat

# 3. 启动应用
mvn spring-boot:run
```

#### Linux/Mac 用户：
```bash
# 1. 部署合约
cd hardhat
npx hardhat node                    # 新终端
npx hardhat run scripts/deploy-assets.js --network localhost

# 2. 更新配置
cd ../java/web3-asset-system
chmod +x update-contract-address.sh
./update-contract-address.sh

# 3. 启动应用
mvn spring-boot:run
```

---

## ⚙️ 配置方式

### 1. 直接修改 application.yml

编辑 `src/main/resources/application.yml`，找到以下配置项并替换为实际地址：

```yaml
web3:
  contracts:
    asset-token: 0xYourAssetTokenAddress
    deposit-vault: 0xYourDepositVaultAddress
    withdrawal-manager: 0xYourWithdrawalManagerAddress
```

### 2. 使用环境变量（推荐生产环境）

创建 `.env` 文件（复制自 `.env.example`）：

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入实际地址：

```properties
ASSET_TOKEN_ADDRESS=0xYourAssetTokenAddress
DEPOSIT_VAULT_ADDRESS=0xYourDepositVaultAddress
WITHDRAWAL_MANAGER_ADDRESS=0xYourWithdrawalManagerAddress
```

启动时加载环境变量：

```bash
# Linux/Mac
export $(cat .env | xargs)
mvn spring-boot:run

# Windows PowerShell
Get-Content .env | ForEach-Object { Invoke-Expression $_ }
mvn spring-boot:run
```

### 3. JVM 参数传入

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DASSET_TOKEN_ADDRESS=0x... -DDEPOSIT_VAULT_ADDRESS=0x..."
```

---

## 🔍 获取合约地址

### 方法一：从 Hardhat 部署结果中获取

部署完成后，合约地址保存在：

- **配置文件**: `config/asset-contracts.json`
- **控制台输出**: 部署脚本会打印所有合约地址

示例输出：
```
═══════════════════════════════════════════
 部署完成！
═══════════════════════════════════════════
📋 合约地址汇总:
  AssetToken:         0x5FbDB2315678afecb367f032d93F642f64180aa3
  DepositVault:       0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
  WithdrawalManager:  0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0
```

### 方法二：从 Etherscan 查询

如果部署到测试网或主网，可以在 Etherscan 上查看：

- **Sepolia**: https://sepolia.etherscan.io/address/{合约地址}
- **BNB Testnet**: https://testnet.bscscan.com/address/{合约地址}
- **Arbitrum Sepolia**: https://sepolia.arbiscan.io/address/{合约地址}

### 方法三：从交易记录中获取

```javascript
// Hardhat 控制台
const receipt = await deploymentTransaction.wait();
console.log("合约地址:", receipt.contractAddress);
```

---

## 🌐 多链配置

### 配置不同网络的合约地址

每个网络需要单独部署合约，配置示例：

```yaml
web3:
  chains:
    # Ethereum Sepolia
    - name: ethereum
      rpc-url: https://sepolia.infura.io/v3/YOUR_PROJECT_ID
      chain-id: 11155111
      enabled: true
    
    # BNB Chain Testnet
    - name: bnb
      rpc-url: https://data-seed-prebsc-1-s1.binance.org:8545
      chain-id: 97
      enabled: false  # 暂时禁用
    
    # Arbitrum Sepolia
    - name: arbitrum
      rpc-url: https://sepolia-rollup.arbitrum.io/rpc
      chain-id: 421614
      enabled: false
  
  # 注意：合约地址是全局的，如果需要支持多链不同地址
  # 建议扩展配置结构或使用数据库存储
  contracts:
    asset-token: 0x...
    deposit-vault: 0x...
    withdrawal-manager: 0x...
```

### 部署到不同网络

```bash
# 部署到 Sepolia
npx hardhat run scripts/deploy-assets.js --network sepolia

# 部署到 BSC Testnet
npx hardhat run scripts/deploy-assets.js --network bscTestnet

# 部署到 Arbitrum Sepolia
npx hardhat run scripts/deploy-assets.js --network arbitrumSepolia
```

---

## 🔐 环境变量配置

### 完整的环境变量列表

```properties
# ============================================
# 合约地址
# ============================================
ASSET_TOKEN_ADDRESS=0x...
DEPOSIT_VAULT_ADDRESS=0x...
WITHDRAWAL_MANAGER_ADDRESS=0x...

# ============================================
# RPC 节点
# ============================================
ETH_RPC_URL=https://sepolia.infura.io/v3/YOUR_PROJECT_ID
BNB_RPC_URL=https://data-seed-prebsc-1-s1.binance.org:8545
ARBITRUM_RPC_URL=https://sepolia-rollup.arbitrum.io/rpc

# ============================================
# 私钥（生产环境必须使用 KMS）
# ============================================
WEB3_PRIVATE_KEY=0x...

# ============================================
# 数据库
# ============================================
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/web3_asset
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root

# ============================================
# Redis
# ============================================
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=

# ============================================
# Kafka
# ============================================
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### Docker 环境变量配置

```yaml
version: '3.8'
services:
  web3-asset:
    image: web3-asset-system:latest
    environment:
      - ASSET_TOKEN_ADDRESS=0x...
      - DEPOSIT_VAULT_ADDRESS=0x...
      - WITHDRAWAL_MANAGER_ADDRESS=0x...
      - WEB3_PRIVATE_KEY=${PRIVATE_KEY}  # 从 .env 文件读取
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/web3_asset
      - SPRING_REDIS_HOST=redis
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

## ❓ 常见问题

### Q1: 如何验证合约地址是否正确配置？

**A:** 启动应用后，查看日志输出：

```
Web3j 多链管理器初始化完成
已配置的链数量: 2
链: ethereum - 当前区块: 18000000
链: localhost - 当前区块: 100
```

或者调用 API 测试：

```bash
curl http://localhost:8080/api/assets/balance?chainName=localhost&userAddress=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
```

### Q2: 合约地址配置错误会有什么表现？

**A:** 
- 调用合约方法时报错：`Contract call failed`
- 日志中出现：`execution reverted` 或 `invalid address`
- 查询余额返回 0 或异常

### Q3: 如何在运行时动态切换合约地址？

**A:** 当前配置是静态的，如需动态切换，建议：
1. 将合约地址存储在数据库中
2. 创建配置刷新接口
3. 使用 Spring Cloud Config 进行配置管理

### Q4: 生产环境如何保护私钥？

**A:** 
- ✅ 使用 AWS KMS、HashiCorp Vault 等密钥管理服务
- ✅ 通过环境变量注入，不要硬编码在代码中
- ✅ 使用 `.gitignore` 忽略 `.env` 文件
- ❌ 不要将私钥提交到 Git 仓库

### Q5: 本地测试网的合约地址每次重启都会变吗？

**A:** 是的，Hardhat 本地节点重启后会重置状态。建议：
- 使用持久化的本地节点
- 或者每次重启后重新部署并更新配置

---

## 📞 技术支持

如遇到问题，请检查：

1. **合约是否已部署**: 确认 `config/asset-contracts.json` 存在
2. **地址格式是否正确**: 必须以 `0x` 开头，40 个十六进制字符
3. **网络连接是否正常**: 确保 RPC 节点可访问
4. **日志信息**: 查看 `logs/web3-asset.log` 中的详细错误

---

## 🔗 相关文档

- [Hardhat 部署文档](../../hardhat/README.md)
- [服务使用说明](SERVICES.md)
- [Web3j 官方文档](https://docs.web3j.io/)
