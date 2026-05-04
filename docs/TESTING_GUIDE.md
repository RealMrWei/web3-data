# Web3 资产系统 - 完整测试指南

## 📋 测试目标
验证双本地链环境下的充值、提现功能，以及动态链配置和合约地址管理。

---

## 第一步：启动双本地链

### 1.1 启动第一个本地链（端口 8545，链 ID 31337）

**打开终端窗口 1：**
```bash
cd d:\study\web3-data\hardhat
npx hardhat node --port 8545
```

✅ **预期输出：**
- 监听端口：`http://127.0.0.1:8545`
- 链 ID：`31337`
- 提供 20 个测试账户

⚠️ **保持此窗口运行，不要关闭！**

---

### 1.2 启动第二个本地链（端口 8546，链 ID 31338）

**打开终端窗口 2：**
```bash
cd d:\study\web3-data\hardhat
HARDHAT_CHAIN_ID=31338 npx hardhat node --port 8546
```

✅ **预期输出：**
- 监听端口：`http://127.0.0.1:8546`
- 链 ID：`31338`
- 提供 20 个测试账户

⚠️ **保持此窗口运行，不要关闭！**

---

## 第二步：部署合约到两个链

### 2.1 部署第一个链的所有合约

**打开终端窗口 3：**
```bash
cd d:\study\web3-data\hardhat

# 1. 部署 AssetToken 代币合约
npx hardhat run scripts/deploy-token.js --network localhost
```

📝 **记录输出信息（重要！）：**
```
✅ AssetToken 代理地址: 0x第一个链的AssetToken地址
```

**继续部署其他合约：**
```bash
# 2. 部署 DepositVault 和 WithdrawalManager
npx hardhat run scripts/deploy-assets.js --network localhost
```

📝 **记录输出信息（重要！）：**
```
✅ DepositVault 代理地址 = 0x第一个链的DepositVault地址
✅ WithdrawalManager 代理地址 = 0x第一个链的WithdrawalManager地址
```

---

### 2.2 部署第二个链的所有合约

**在终端窗口 3 继续执行：**

```bash
# 1. 部署 AssetToken 代币合约
npx hardhat run scripts/deploy-token.js --network localhost2
```

📝 **记录输出信息（重要！）：**
```
✅ AssetToken 代理地址: 0x第二个链的AssetToken地址
```

**继续部署其他合约：**
```bash
# 2. 部署 DepositVault 和 WithdrawalManager
npx hardhat run scripts/deploy-assets.js --network localhost2
```

📝 **记录输出信息（重要！）：**
```
✅ DepositVault 代理地址 = 0x第二个链的DepositVault地址
✅ WithdrawalManager 代理地址 = 0x第二个链的WithdrawalManager地址
```

---

## 第三步：添加代币支持并授权

### 3.1 第一个链添加代币到 Vault

**在终端窗口 3 执行：**
```bash
TOKEN_ADDRESS=0x第一个链的AssetToken地址 VAULT_ADDRESS=0x第一个链的DepositVault地址 npx hardhat run scripts/add-token-to-vault.js --network localhost
TOKEN_ADDRESS=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512 VAULT_ADDRESS=0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9 npx hardhat run scripts/test-deposit.js --network localhost

```

✅ **预期输出：**
```
Token added to vault successfully
```

---

### 3.2 第二个链添加代币到 Vault

**在终端窗口 3 执行：**
```bash
TOKEN_ADDRESS=0x第二个链的AssetToken地址 VAULT_ADDRESS=0x第二个链的DepositVault地址 npx hardhat run scripts/add-token-to-vault.js --network localhost2
```

✅ **预期输出：**
```
Token added to vault successfully
```

---

### 3.3 给测试账户铸造代币（用于充值测试）

**在终端窗口 3 执行（第一个链）：**
```bash
TOKEN_ADDRESS=0x第一个链的AssetToken地址 npx hardhat run scripts/mint-tokens.js --network localhost
```

✅ **预期输出：**
```
已为账户 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266 铸造 1000 个代币
已为账户 0x70997970C51812dc3A010C7d01b50e0d17dc79C8 铸造 1000 个代币
...
代币铸造完成！
```

**在终端窗口 3 执行（第二个链）：**
```bash
TOKEN_ADDRESS=0x第二个链的AssetToken地址 npx hardhat run scripts/mint-tokens.js --network localhost2
```

✅ **预期输出：**
```
代币铸造完成！
```

---

## 第四步：配置数据库

### 4.1 验证数据库表结构

```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SHOW TABLES;"
```

✅ **预期输出（8 个表）：**
- chain_config
- contract_address
- user_asset
- event_listener_offset
- deposit_record
- withdrawal_order
- withdrawal_record
- shedlock

---

### 4.2 插入链配置和合约地址

**替换下面的占位符为实际部署的合约地址，然后执行：**

```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset <<EOF

-- 1. 确认第一个链配置已存在
SELECT * FROM chain_config WHERE chain_name = 'localhost';

-- 2. 添加第二个链配置
INSERT INTO chain_config (chain_name, rpc_url, chain_id, status) 
VALUES ('localhost2', 'http://127.0.0.1:8546', 31338, 1)
ON DUPLICATE KEY UPDATE rpc_url = VALUES(rpc_url);

-- 3. 插入第一个链的合约地址
INSERT INTO contract_address (chain_name, contract_type, contract_address, status) 
VALUES 
('localhost', 'asset_token', '替换为第一个链的AssetToken地址', 1),
('localhost', 'deposit_vault', '替换为第一个链的DepositVault地址', 1),
('localhost', 'withdrawal_manager', '替换为第一个链的WithdrawalManager地址', 1);

-- 4. 插入第二个链的合约地址
INSERT INTO contract_address (chain_name, contract_type, contract_address, status) 
VALUES 
('localhost2', 'asset_token', '替换为第二个链的AssetToken地址', 1),
('localhost2', 'deposit_vault', '替换为第二个链的DepositVault地址', 1),
('localhost2', 'withdrawal_manager', '替换为第二个链的WithdrawalManager地址', 1);

-- 5. 验证数据
SELECT * FROM chain_config;
SELECT * FROM contract_address ORDER BY chain_name, contract_type;

EOF
```

---

## 第五步：启动 Java 应用

### 5.1 编译项目

```bash
cd d:\study\web3-data\java\web3-asset-system
mvn clean compile
```

✅ **预期结果：** BUILD SUCCESS

---

### 5.2 启动应用

```bash
mvn spring-boot:run
```

✅ **观察日志，确认以下信息：**
```
链配置服务初始化完成
合约地址配置服务初始化完成
Web3j 多链管理器初始化完成
初始化 Web3j 实例 - 链: localhost, RPC: http://127.0.0.1:8545
初始化 Web3j 实例 - 链: localhost2, RPC: http://127.0.0.1:8546
已配置的链数量: 2
链: localhost - 当前区块: XXX
链: localhost2 - 当前区块: YYY
开始同步充值事件
开始同步提现事件
```

⚠️ **保持此窗口运行！**

---

## 第六步：验证 API 接口

### 6.1 查询所有链配置

```bash
curl http://localhost:8080/api/chain/list | jq
```

✅ **预期输出：** 返回 2 个链配置

---

### 6.2 查询所有合约地址

```bash
curl http://localhost:8080/api/contract/list | jq
```

✅ **预期输出：** 返回 6 条合约地址记录（每个链 3 个合约）

---

### 6.3 查询指定链的合约地址

```bash
curl http://localhost:8080/api/contract/list/localhost | jq
```

✅ **预期输出：** 返回 localhost 链的 3 个合约地址

---

## 第七步：测试充值功能

### 7.1 准备测试账户

获取 Hardhat 提供的测试账户地址（从第一步的输出中复制）：
- 账户 0: `0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266`
- 账户 1: `0x70997970C51812dc3A010C7d01b50e0d17dc79C8`

---

### 7.2 模拟用户充值（手动调用合约）

**创建充值测试脚本 `scripts/test-deposit.js`：**

```javascript
const hre = require("hardhat");

async function main() {
  const [user] = await hre.ethers.getSigners();
  
  // 获取合约实例
  const AssetToken = await hre.ethers.getContractFactory("AssetToken");
  const DepositVault = await hre.ethers.getContractFactory("DepositVault");
  
  const tokenAddress = process.env.TOKEN_ADDRESS;
  const vaultAddress = process.env.VAULT_ADDRESS;
  
  if (!tokenAddress || !vaultAddress) {
    console.error("请设置 TOKEN_ADDRESS 和 VAULT_ADDRESS 环境变量");
    return;
  }
  
  const token = AssetToken.attach(tokenAddress);
  const vault = DepositVault.attach(vaultAddress);
  
  // 1. 授权 Vault 使用代币
  const approveAmount = hre.ethers.parseUnits("100", 18);
  const approveTx = await token.approve(vaultAddress, approveAmount);
  await approveTx.wait();
  console.log(`✓ 已授权 Vault 使用 ${hre.ethers.formatUnits(approveAmount, 18)} 个代币`);
  
  // 2. 调用 deposit 函数
  const depositAmount = hre.ethers.parseUnits("10", 18);
  const depositTx = await vault.deposit(tokenAddress, depositAmount);
  const receipt = await depositTx.wait();
  
  console.log(`✓ 充值成功！`);
  console.log(`  交易哈希: ${receipt.hash}`);
  console.log(`  区块号: ${receipt.blockNumber}`);
  console.log(`  充值金额: ${hre.ethers.formatUnits(depositAmount, 18)} 代币`);
  console.log(`  用户地址: ${user.address}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
```

**在终端窗口 3 执行（第一个链充值）：**
```bash
TOKEN_ADDRESS=0x第一个链的AssetToken地址 VAULT_ADDRESS=0x第一个链的DepositVault地址 npx hardhat run scripts/test-deposit.js --network localhost
```

📝 **记录交易哈希和区块号**

---

**在终端窗口 3 执行（第二个链充值）：**
```bash
TOKEN_ADDRESS=0x第二个链的AssetToken地址 VAULT_ADDRESS=0x第二个链的DepositVault地址 npx hardhat run scripts/test-deposit.js --network localhost2
```

📝 **记录交易哈希和区块号**

---

### 7.3 等待事件监听器处理

⏱️ **等待 60-90 秒**，让定时任务扫描到充值事件。

**观察 Java 应用日志：**
```
开始同步充值事件
检测到新的 Deposit 事件: txHash=0x...
发送充值事件到 Kafka: topic=deposit-events
```

---

### 7.4 验证充值结果

**查询用户资产表：**
```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT * FROM user_asset;"
```

✅ **预期结果：** 应该看到两条记录（两个链各一条）

**查询充值记录表：**
```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT * FROM deposit_record ORDER BY create_time DESC LIMIT 5;"
```

✅ **预期结果：** 应该看到两条充值记录

---

## 第八步：测试提现功能

### 8.1 创建提现订单

**使用 API 创建提现申请：**

```bash
curl -X POST http://localhost:8080/api/withdrawal/create \
  -H "Content-Type: application/json" \
  -d '{
    "userAddress": "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
    "recipient": "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
    "chainName": "localhost",
    "tokenAddress": "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
    "amount": 5000000000000000000
  }'
```

📝 **记录返回的订单号**

---

### 8.2 审批提现订单

```bash
curl -X POST http://localhost:8080/api/withdrawal/approve \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "上一步返回的订单号",
    "approverAddress": "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"
  }'
```

---

### 8.3 执行提现在链上

**创建提现执行脚本 `scripts/test-withdrawal.js`：**

```javascript
const hre = require("hardhat");

async function main() {
  const [operator] = await hre.ethers.getSigners();
  
  // 获取合约实例
  const WithdrawalManager = await hre.ethers.getContractFactory("WithdrawalManager");
  
  const withdrawalManagerAddress = process.env.WITHDRAWAL_MANAGER_ADDRESS;
  const recipient = process.env.RECIPIENT;
  const tokenAddress = process.env.TOKEN_ADDRESS;
  const amount = process.env.AMOUNT;
  
  if (!withdrawalManagerAddress || !recipient || !tokenAddress || !amount) {
    console.error("请设置所有必需的环境变量");
    return;
  }
  
  const withdrawalManager = WithdrawalManager.attach(withdrawalManagerAddress);
  
  // 调用 withdraw 函数
  const tx = await withdrawalManager.withdraw(recipient, tokenAddress, amount);
  const receipt = await tx.wait();
  
  console.log(`✓ 提现执行成功！`);
  console.log(`  交易哈希: ${receipt.hash}`);
  console.log(`  区块号: ${receipt.blockNumber}`);
  console.log(`  接收地址: ${recipient}`);
  console.log(`  提现金额: ${hre.ethers.formatUnits(amount, 18)} 代币`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
```

**在终端窗口 3 执行（第一个链提现）：**
```bash
WITHDRAWAL_MANAGER_ADDRESS=0x第一个链的WithdrawalManager地址 RECIPIENT=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266 TOKEN_ADDRESS=0x第一个链的AssetToken地址 AMOUNT=5000000000000000000 npx hardhat run scripts/test-withdrawal.js --network localhost
```

📝 **记录交易哈希**

---

### 8.4 等待事件监听器处理

⏱️ **等待 60-90 秒**，让定时任务扫描到提现事件。

**观察 Java 应用日志：**
```
开始同步提现事件
检测到新的 WithdrawalExecuted 事件: txHash=0x...
发送提现事件到 Kafka: topic=withdrawal-events
```

---

### 8.5 验证提现结果

**查询提现订单状态：**
```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT order_no, status, tx_hash FROM withdrawal_order ORDER BY create_time DESC LIMIT 1;"
```

✅ **预期结果：** status 应该为 2（已执行），tx_hash 应该有值

**查询用户资产余额变化：**
```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT user_address, chain_name, balance, pending_balance FROM user_asset WHERE user_address = '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266';"
```

✅ **预期结果：** 余额应该减少

---

## 第九步：测试动态链配置

### 9.1 添加第三条链（模拟）

```bash
curl -X POST http://localhost:8080/api/chain/add \
  -H "Content-Type: application/json" \
  -d '{
    "chainName": "test-chain",
    "rpcUrl": "http://127.0.0.1:8547",
    "chainId": 31339,
    "status": 1
  }'
```

✅ **预期响应：** `链配置添加并加载成功`

**观察 Java 日志：**
```
动态加载链配置 - 链: test-chain, RPC: http://127.0.0.1:8547
链 test-chain 加载成功 - 当前区块: XXX
```

---

### 9.2 验证新链已加载

```bash
curl http://localhost:8080/api/chain/list | jq
```

✅ **预期输出：** 返回 3 个链配置

---

### 9.3 禁用测试链

```bash
curl -X PUT http://localhost:8080/api/chain/disable/test-chain
```

✅ **预期响应：** `链配置已禁用并卸载`

---

## 第十步：测试合约地址管理

### 10.1 添加新合约地址

```bash
curl -X POST http://localhost:8080/api/contract/add \
  -H "Content-Type: application/json" \
  -d '{
    "chainName": "localhost",
    "contractType": "test_contract",
    "contractAddress": "0x0000000000000000000000000000000000000001",
    "abiVersion": "1.0",
    "status": 1
  }'
```

---

### 10.2 批量添加合约地址

```bash
curl -X POST http://localhost:8080/api/contract/batch-add \
  -H "Content-Type: application/json" \
  -d '[
    {
      "chainName": "localhost2",
      "contractType": "extra_token_1",
      "contractAddress": "0x0000000000000000000000000000000000000002",
      "status": 1
    },
    {
      "chainName": "localhost2",
      "contractType": "extra_token_2",
      "contractAddress": "0x0000000000000000000000000000000000000003",
      "status": 1
    }
  ]'
```

✅ **预期响应：** `批量添加完成 - 成功: 2, 失败: 0`

---

## 📊 测试结果汇总

### ✅ 成功标志

| 测试项 | 验证方法 | 预期结果 |
|--------|---------|---------|
| 双本地链启动 | 两个终端窗口正常运行 | 端口 8545 和 8546 监听中 |
| 合约部署 | 脚本执行成功 | 每个链有 3 个合约地址（含 AssetToken） |
| 代币授权 | add-token-to-vault 成功 | Vault 可以接收代币 |
| 代币铸造 | mint-tokens 成功 | 测试账户有足够余额 |
| 充值功能 | 调用 deposit 后查询数据库 | deposit_record 表有记录，user_asset 余额增加 |
| 提现功能 | 创建订单→审批→执行后查询 | withdrawal_order 状态为 2，余额减少 |
| 事件监听 | Java 日志显示事件捕获 | 60 秒内检测到事件并发送到 Kafka |
| 动态链配置 | 添加/禁用链 API | 无需重启即可生效 |
| 合约地址管理 | CRUD API 正常 | 数据库记录正确更新 |

---

## 🔧 常见问题排查

### 问题 1：Java 应用启动失败

**检查点：**
```bash
# 1. 确认 Docker 容器运行中
docker ps

# 2. 确认数据库连接正常
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT 1;"

# 3. 确认 Redis 运行中
docker exec -it web3-redis redis-cli ping
```

---

### 问题 2：充值事件未被捕获

**检查点：**
```bash
# 1. 查看事件监听偏移量
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT * FROM event_listener_offset;"

# 2. 确认 last_processed_block 小于充值交易的区块号

# 3. 手动触发一次同步（等待下一个 60 秒周期）
```

---

### 问题 3：合约地址查询失败

**检查点：**
```bash
# 确认 contract_address 表有数据
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset -e "SELECT * FROM contract_address;"

# 如果为空，重新执行第四步的 SQL
```

---

### 问题 4：Hardhat 链 ID 不匹配

**错误信息：**
```
HardhatError: HH101: Hardhat was set to use chain id 31338, but connected to a chain with id 31337.
```

**解决方案：**
使用环境变量启动第二个本地链：
```bash
HARDHAT_CHAIN_ID=31338 npx hardhat node --port 8546
```

---

## 🎯 下一步

测试完成后，你可以：
1. 尝试更多充值/提现场景
2. 测试多用户并发充值
3. 测试异常情况（如余额不足、无效地址等）
4. 优化事件监听频率和性能

祝测试顺利！
