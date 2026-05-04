# Web3 链上资产管理系统 - 详细测试指南

## 1. 环境准备

### 1.1 依赖安装
```bash
# Node.js 18+ (推荐使用 nvm 管理)
node --version

# Java 8+ (JDK 1.8+)
java -version

# Maven 3.6+
mvn -version

# Docker & Docker Compose
docker --version
docker-compose --version

# Git
git --version
```

### 1.2 项目克隆与初始化
```bash
git clone <repository-url>
cd web3-data

# 初始化子项目
cd hardhat
npm install

cd ../java/web3-asset-system
mvn clean compile
```

## 2. 启动本地区块链节点（双链配置）

### 2.1 启动本地区块链节点
```bash
# 终端1 - 启动第一链（localhost）
cd hardhat
npx hardhat node

# 终端2 - 启动第二链（localhost2）- 可选，仅用于多链测试
# 如果需要测试多链功能，可以启动另一个 Hardhat 节点在不同端口
npx hardhat node --port 8546
```

### 2.2 验证节点状态
```bash
# 检查节点是否正常运行
curl -X POST http://127.0.0.1:8545 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}'
```

## 3. 智能合约部署与授权流程

### 3.1 部署基础合约
```bash
# 在 hardhat 目录下执行
cd hardhat

# 部署所有合约（包括 AssetToken, DepositVault, WithdrawalManager）
npx hardhat run scripts/deploy-assets.js --network localhost

# 记录输出的合约地址，类似如下：
# AssetToken Proxy: 0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
# AssetToken Implementation: 0x5FbDB2315678afecb367f032d93F642f64180aa3
# DepositVault Proxy: 0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9
# DepositVault Implementation: 0x9fE46736679d2D9a65F0992F17f33c1cA5C5465c
# WithdrawalManager Proxy: 0x5FC8d32690cc91D4c39d9d3abcBD16989F875707
# WithdrawalManager Implementation: 0x0DCd6537866C3d273e50333988328046f58dB9e5
```

### 3.2 配置合约地址
```bash
# 部署完成后，地址会自动保存到 config/address.json
cat config/address.json
```

### 3.3 添加代币到充值金库白名单
```bash
# 将 AssetToken 添加到 DepositVault 的白名单
npx hardhat run scripts/add-token-to-vault.js --network localhost
```

### 3.4 为测试账户铸造代币
```bash
# 为指定账户铸造代币（用于测试）
npx hardhat run scripts/mint-tokens.js --network localhost
```

## 4. 数据库初始化与表结构录入

### 4.1 启动 MySQL 服务
```bash
# 使用 Docker Compose 启动 MySQL、Redis、Kafka
cd d:\study\web3-data
docker-compose up -d mysql redis kafka
```

### 4.2 初始化数据库表结构
```bash
# 确保 MySQL 已启动并配置好数据库
mysql -u root -p
# 输入密码: root123456

# 创建数据库
CREATE DATABASE web3_asset CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'web3_user'@'%' IDENTIFIED BY 'web3_password';
GRANT ALL PRIVILEGES ON web3_asset.* TO 'web3_user'@'%';
FLUSH PRIVILEGES;
EXIT;

# 验证数据库连接
mysql -u web3_user -p web3_asset
```

### 4.3 更新合约地址配置
```bash
# 进入 Java 项目目录
cd d:\study\web3-data\java\web3-asset-system

# 运行合约地址更新脚本（Windows）
update-contract-address.bat

# 或在 Linux/Mac 上
chmod +x update-contract-address.sh
./update-contract-address.sh
```

## 5. 充值功能测试

### 5.1 启动 Java 应用
```bash
# 确保数据库、Redis、Kafka 已启动
cd d:\study\web3-data\java\web3-asset-system
mvn spring-boot:run
```

### 5.2 执行充值测试
```bash
# 在另一个终端窗口执行
cd hardhat

# 执行充值诊断脚本（自动检测和修复常见问题）
npx hardhat run scripts/diagnose-deposit.js --network localhost

# 或执行手动充值测试
npx hardhat run scripts/test-deposit.js --network localhost
```

### 5.3 验证充值结果
```bash
# 检查应用日志，应能看到类似以下内容：
# - 充值事件监听器捕获交易
# - 交易哈希被发送到 Kafka
# - 数据库中插入充值记录
# - 用户资产表中余额更新
```

## 6. 提现功能测试

### 6.1 执行提现测试
```bash
# 首先进行提现流程测试
cd hardhat
npx hardhat run scripts/test-withdrawal.js --network localhost

# 这将执行以下步骤：
# 1. 用户授权 WithdrawalManager 使用代币
# 2. 将代币存入 WithdrawalManager（内部记账）
# 3. 用户发起提现请求
# 4. 生成提现记录和事件
```

### 6.2 批准并执行提现
```bash
# 作为操作员批准并执行提现
npx hardhat run scripts/approve-and-execute-withdrawal.js --network localhost

# 这将执行以下步骤：
# 1. 检查提现记录状态
# 2. 操作员批准提现请求
# 3. 操作员执行提现，将代币转给用户
```

### 6.3 验证提现结果
```bash
# 检查应用日志，应能看到类似以下内容：
# - 提现事件监听器捕获交易
# - 交易哈希被发送到 Kafka
# - 数据库中插入提现订单记录
# - 用户资产表中余额更新
```

## 7. 事件监听器功能验证

### 7.1 验证充值事件监听
```bash
# 在 Java 应用日志中查找以下内容：
# - "找到 X 条充值事件"
# - "发送充值事件到 Kafka"
# - "成功发送 X 条充值事件到 Kafka"

# 检查数据库中 deposit_record 表是否新增记录
SELECT * FROM deposit_record ORDER BY create_time DESC LIMIT 10;
```

### 7.2 验证提现事件监听
```bash
# 在 Java 应用日志中查找以下内容：
# - "找到 X 条提现事件"
# - "发送提现事件到 Kafka"
# - "成功发送 X 条提现事件到 Kafka"

# 检查数据库中 withdrawal_order 表是否新增记录
SELECT * FROM withdrawal_order ORDER BY create_time DESC LIMIT 10;
```

## 8. API 功能测试

### 8.1 测试充值查询接口
```bash
# 使用 curl 或 Postman 测试
curl -X GET "http://localhost:8080/api/deposits/user/0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266?chain=localhost&limit=10&page=1"
```

### 8.2 测试资产查询接口
```bash
# 查询用户资产
curl -X GET "http://localhost:8080/api/assets/user/0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"
```

## 9. 常见问题排查

### 9.1 合约地址不匹配
```bash
# 重新部署合约并更新配置
cd hardhat
npx hardhat run scripts/deploy-assets.js --network localhost
cd ../java/web3-asset-system
# 重新运行更新脚本
```

### 9.2 事件监听器未触发
- 检查 [application.yml](file:///d:/study/web3-data/java/web3-asset-system/src/main/resources/application.yml) 中的合约地址配置
- 确认区块链节点正在运行
- 检查数据库中 [chain_config](file:///d:/study/web3-data/java/web3-asset-system/src/main/java/com/web3/entity/ChainConfig.java#L13-L45) 和 [contract_address](file:///d:/study/web3-data/java/web3-asset-system/src/main/java/com/web3/entity/ContractAddress.java#L15-L51) 表的配置

### 9.3 Kafka 连接问题
- 确认 Kafka 服务正在运行
- 检查网络连接和防火墙设置
- 查看应用日志中的连接错误信息

### 9.4 数据库连接问题
- 确认 MySQL 服务正在运行
- 检查 [application.yml](file:///d:/study/web3-data/java/web3-asset-system/src/main/resources/application.yml) 中的数据库连接配置
- 验证数据库用户名密码是否正确

## 10. 集群部署注意事项

### 10.1 分布式锁配置
- 确保所有节点共享同一个 Redis 实例
- 验证 ShedLock 配置正确，防止多个节点重复处理事件

### 10.2 Kafka 消费者组
- 确保所有节点使用相同的消费者组名称
- 验证手动提交偏移量配置正确

### 10.3 数据库连接池
- 调整连接池大小以适应集群规模
- 验证数据库最大连接数设置足够支持所有节点