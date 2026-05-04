# 合约地址配置检查清单

## ✅ 配置前准备

- [ ] 已部署智能合约到目标网络
- [ ] 已获得合约地址（从 `config/asset-contracts.json` 或控制台输出）
- [ ] 已准备好 RPC 节点地址（Infura/Alchemy 或其他节点提供商）

---

## 📝 配置步骤

### 步骤 1：确认合约地址

从以下位置获取合约地址：

```bash
# 查看部署结果
cat ../../config/asset-contracts.json
```

应该看到类似输出：
```json
{
  "network": "localhost",
  "deployer": "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
  "contracts": {
    "assetToken": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
    "depositVault": "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
    "withdrawalManager": "0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0"
  }
}
```

**记录以下地址：**
- AssetToken: `_______________________________`
- DepositVault: `_______________________________`
- WithdrawalManager: `_______________________________`

---

### 步骤 2：选择配置方式

#### ☑️ 方式 A：使用自动脚本（推荐）

**Windows:**
```bash
cd java\web3-asset-system
update-contract-address.bat
```

**Linux/Mac:**
```bash
cd java/web3-asset-system
chmod +x update-contract-address.sh
./update-contract-address.sh
```

✅ 脚本会自动：
- 读取 `config/asset-contracts.json`
- 提取合约地址
- 更新 `application.yml`

---

#### ☑️ 方式 B：手动编辑 application.yml

编辑文件：`java/web3-asset-system/src/main/resources/application.yml`

找到以下配置项并替换：

```yaml
web3:
  contracts:
    # 替换为你的 AssetToken 地址
    asset-token: 0x5FbDB2315678afecb367f032d93F642f64180aa3
    
    # 替换为你的 DepositVault 地址
    deposit-vault: 0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
    
    # 替换为你的 WithdrawalManager 地址
    withdrawal-manager: 0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0
```

---

#### ☑️ 方式 C：使用环境变量

1. 复制模板文件：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，填入实际地址：
```properties
ASSET_TOKEN_ADDRESS=0x5FbDB2315678afecb367f032d93F642f64180aa3
DEPOSIT_VAULT_ADDRESS=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
WITHDRAWAL_MANAGER_ADDRESS=0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0
```

3. 启动时加载环境变量：
```bash
# Linux/Mac
export $(cat .env | xargs)

# Windows PowerShell
Get-Content .env | ForEach-Object { Invoke-Expression $_ }
```

---

### 步骤 3：配置 RPC 节点

在 `application.yml` 中配置 RPC 地址：

```yaml
web3:
  chains:
    - name: localhost
      rpc-url: http://127.0.0.1:8545  # 本地测试网
      chain-id: 31337
      enabled: true
    
    - name: ethereum
      rpc-url: https://sepolia.infura.io/v3/YOUR_PROJECT_ID  # 替换为你的 Infura Project ID
      chain-id: 11155111
      enabled: false  # 暂时禁用
```

**获取 RPC 节点：**
- **Infura**: https://infura.io/ （注册后创建项目）
- **Alchemy**: https://www.alchemy.com/
- **QuickNode**: https://www.quicknode.com/

---

### 步骤 4：配置私钥（仅测试环境）

⚠️ **警告：生产环境必须使用 KMS！**

```yaml
web3:
  private-key: 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
```

或使用环境变量：
```properties
WEB3_PRIVATE_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
```

---

### 步骤 5：验证配置

#### 5.1 检查配置文件

```bash
# 查看配置
cat src/main/resources/application.yml | grep -A 5 "contracts:"
```

应该看到：
```yaml
contracts:
  asset-token: 0x5FbDB2315678afecb367f032d93F642f64180aa3
  deposit-vault: 0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
  withdrawal-manager: 0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0
```

#### 5.2 启动应用

```bash
mvn spring-boot:run
```

观察日志输出：
```
Web3j 多链管理器初始化完成
已配置的链数量: 2
链: ethereum - 当前区块: 18000000
链: localhost - 当前区块: 100
```

#### 5.3 测试 API

```bash
# 查询余额
curl "http://localhost:8080/api/assets/balance?chainName=localhost&userAddress=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"

# 预期响应
{"code":200,"data":"0"}
```

---

## 🔍 常见问题排查

### ❌ 问题 1：应用启动失败

**症状：**
```
Error creating bean with name 'web3jConfig'
Connection refused
```

**解决：**
- [ ] 检查 RPC 节点是否可访问
- [ ] 确认网络连接正常
- [ ] 验证 RPC URL 格式正确

---

### ❌ 问题 2：合约调用失败

**症状：**
```
Contract call failed: execution reverted
Invalid address
```

**解决：**
- [ ] 检查合约地址是否正确（40 个十六进制字符）
- [ ] 确认地址以 `0x` 开头
- [ ] 验证合约已部署到正确的网络

---

### ❌ 问题 3：余额查询返回 0

**可能原因：**
- [ ] 合约地址配置错误
- [ ] 用户未进行充值
- [ ] 查询的链与充值的链不一致

**验证步骤：**
1. 确认合约地址正确
2. 检查用户是否有充值记录
3. 验证 chainName 参数

---

### ❌ 问题 4：交易发送失败

**症状：**
```
insufficient funds for intrinsic transaction cost
replacement fee too low
```

**解决：**
- [ ] 检查账户余额是否充足
- [ ] 调整 Gas 配置（增加 maxFeePerGas）
- [ ] 等待 pending 交易确认

---

## 📋 配置验证清单

完成配置后，逐项检查：

- [ ] 合约地址已正确填写（3 个地址）
- [ ] RPC 节点地址已配置
- [ ] 私钥已配置（测试环境）
- [ ] 数据库连接已配置
- [ ] Redis 连接已配置
- [ ] Kafka 连接已配置（如使用）
- [ ] 应用成功启动无报错
- [ ] API 接口可以正常访问
- [ ] 能够查询到合约数据

---

## 🎯 下一步

配置完成后，可以：

1. **测试充值功能**
   ```bash
   curl -X POST http://localhost:8080/api/assets/deposit/native \
     -H "Content-Type: application/json" \
     -d '{
       "chainName": "localhost",
       "fromAddress": "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
       "privateKey": "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80",
       "amount": "1000000000000000000"
     }'
   ```

2. **查询余额**
   ```bash
   curl "http://localhost:8080/api/assets/balance?chainName=localhost&userAddress=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"
   ```

3. **查看日志**
   ```bash
   tail -f logs/web3-asset.log
   ```

---

## 📞 需要帮助？

- 查看详细文档：[CONFIG_GUIDE.md](CONFIG_GUIDE.md)
- 查看服务说明：[SERVICES.md](SERVICES.md)
- 查看项目总览：[README.md](../../README.md)
