# REST API 接口文档

## 📋 概述

本文档描述了 Web3 链上资产管理系统的 REST API 接口，用于前端联调和测试。

**基础 URL**: `http://localhost:8080/api`

**统一返回格式**:
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

## 🔗 充值相关接口（DepositController）

### 1. 查询充值记录列表（分页）

**接口**: `GET /api/deposit/list`

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码（从1开始） |
| size | int | 否 | 10 | 每页大小 |
| chainName | string | 否 | - | 链名称（ethereum/bnb/arbitrum） |
| status | int | 否 | - | 状态（0-待确认 1-成功 2-失败） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "txHash": "0xabc...",
      "userAddress": "0x123...",
      "chainName": "ethereum",
      "tokenAddress": "0x456...",
      "amount": "1000000000000000000",
      "status": 1,
      "blockNumber": 12345,
      "createTime": "2024-01-01T12:00:00",
      "updateTime": "2024-01-01T12:00:00"
    }
  ],
  "total": 100,
  "current": 1,
  "size": 10,
  "pages": 10
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/deposit/list?page=1&size=10&chainName=ethereum&status=1"
```

---

### 2. 根据 txHash 查询单笔充值

**接口**: `GET /api/deposit/tx/{txHash}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| txHash | string | 交易哈希 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "txHash": "0xabc...",
    "userAddress": "0x123...",
    "chainName": "ethereum",
    "tokenAddress": "0x456...",
    "amount": "1000000000000000000",
    "status": 1,
    "blockNumber": 12345,
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/deposit/tx/0xabc123..."
```

---

### 3. 根据用户地址查询充值历史

**接口**: `GET /api/deposit/user/{userAddress}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| userAddress | string | 用户钱包地址 |

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页大小 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [...],
  "total": 50,
  "current": 1,
  "size": 10,
  "pages": 5
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/deposit/user/0x123...?page=1&size=10"
```

---

### 4. 统计充值数据

**接口**: `GET /api/deposit/statistics`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 否 | 链名称（可选） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 1000,
    "successCount": 950,
    "pendingCount": 30,
    "failedCount": 20
  }
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/deposit/statistics?chainName=ethereum"
```

---

## 💸 提现相关接口（WithdrawalController）

### 1. 查询提现订单列表（分页）

**接口**: `GET /api/withdrawal/list`

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页大小 |
| chainName | string | 否 | - | 链名称 |
| status | int | 否 | - | 状态（0-待审批 1-已批准 2-已完成 3-失败） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "orderNo": "WD20240101120000",
      "userAddress": "0x123...",
      "recipient": "0x456...",
      "chainName": "ethereum",
      "tokenAddress": "0x789...",
      "amount": "500000000000000000",
      "status": 2,
      "txHash": "0xabc...",
      "approverAddress": "0xadmin...",
      "createTime": "2024-01-01T12:00:00",
      "completedTime": "2024-01-01T12:05:00",
      "updateTime": "2024-01-01T12:05:00"
    }
  ],
  "total": 50,
  "current": 1,
  "size": 10,
  "pages": 5
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/withdrawal/list?page=1&size=10&status=2"
```

---

### 2. 根据订单号查询单笔提现

**接口**: `GET /api/withdrawal/order/{orderNo}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| orderNo | string | 订单号 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "WD20240101120000",
    "userAddress": "0x123...",
    "recipient": "0x456...",
    "chainName": "ethereum",
    "tokenAddress": "0x789...",
    "amount": "500000000000000000",
    "status": 2,
    "txHash": "0xabc...",
    "approverAddress": "0xadmin...",
    "createTime": "2024-01-01T12:00:00",
    "completedTime": "2024-01-01T12:05:00",
    "updateTime": "2024-01-01T12:05:00"
  }
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/withdrawal/order/WD20240101120000"
```

---

### 3. 根据用户地址查询提现历史

**接口**: `GET /api/withdrawal/user/{userAddress}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| userAddress | string | 用户钱包地址 |

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 10 | 每页大小 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [...],
  "total": 20,
  "current": 1,
  "size": 10,
  "pages": 2
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/withdrawal/user/0x123...?page=1&size=10"
```

---

### 4. 统计提现数据

**接口**: `GET /api/withdrawal/statistics`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 否 | 链名称（可选） |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCount": 500,
    "completedCount": 450,
    "pendingCount": 30,
    "failedCount": 20
  }
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/withdrawal/statistics?chainName=ethereum"
```

---

### 5. 用户申请提现（Request Withdrawal）

**接口**: `POST /api/withdrawal/requestWithdrawal`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称（localhost/ethereum/bnb等） |
| userAddress | string | 是 | 用户私钥（用于签名交易） |
| tokenAddress | string | 是 | 代币合约地址 |
| amount | BigInteger | 是 | 提现金额（wei单位） |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": "0xabc123..." // 交易哈希
}
```

**调用示例**:
``bash
curl -X POST "http://localhost:8080/api/withdrawal/requestWithdrawal" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "chainName=localhost&userAddress=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80&tokenAddress=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512&amount=1000000000000000000"
```

---

### 6. 管理员审批提现（Approve Withdrawal）

**接口**: `POST /api/withdrawal/approveWithdrawal`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称 |
| adminKey | string | 是 | 管理员私钥 |
| withdrawalId | BigInteger | 是 | 提现记录ID |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": "0xdef456..." // 交易哈希
}
```

**调用示例**:
``bash
curl -X POST "http://localhost:8080/api/withdrawal/approveWithdrawal" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "chainName=localhost&adminKey=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80&withdrawalId=1"
```

---

### 7. 获取下一个提现ID（Get Next Withdrawal ID）

**接口**: `GET /api/withdrawal/nextWithdrawalId`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称 |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": 5 // 下一个可用的提现ID
}
```

**调用示例**:
``bash
curl "http://localhost:8080/api/withdrawal/nextWithdrawalId?chainName=localhost"
```

---

### 8. 查询提现记录详情（Get Withdrawal Record）

**接口**: `GET /api/withdrawal/withdrawalRecord`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称 |
| withdrawalId | BigInteger | 是 | 提现记录ID |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": {
    "withdrawalId": 1,
    "status": 1, // 0-待审批 1-已批准 2-已完成 3-失败
    "user": "0x123...",
    "amount": "1000000000000000000",
    "token": "0x456..."
  }
}
```

**调用示例**:
``bash
curl "http://localhost:8080/api/withdrawal/withdrawalRecord?chainName=localhost&withdrawalId=1"
```

---

### 9. 执行直接提现（Execute Withdrawal）

**接口**: `POST /api/withdrawal/withdraw`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称 |
| userAddress | string | 是 | 用户地址 |
| privateKey | string | 是 | 用户私钥 |
| amount | BigInteger | 是 | 提现金额 |
| tokenAddress | string | 是 | 代币合约地址 |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": "0xghi789..." // 交易哈希
}
```

**调用示例**:
``bash
curl -X POST "http://localhost:8080/api/withdrawal/withdraw" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "chainName=localhost&userAddress=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266&privateKey=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80&amount=500000000000000000&tokenAddress=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"
```

---

## 🔐 ERC20 授权管理接口

### 1. 查询授权额度（Allowance）

**接口**: `GET /api/withdrawal/allowance`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称 |
| userAddress | string | 是 | 用户地址（owner） |
| tokenAddress | string | 是 | 代币合约地址 |
| withdrawContract | string | 是 | 提现合约地址（spender） |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": 1000000000000000000 // 授权额度（wei单位）
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/withdrawal/allowance?chainName=localhost&userAddress=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266&tokenAddress=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512&withdrawContract=0x5FC8d32690cc91D4c39d9d3abcBD16989F875707"
```

---

### 2. 模拟用户授权（Approve）

**接口**: `POST /api/withdrawal/approve`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chainName | string | 是 | 链名称 |
| tokenAddress | string | 是 | 代币合约地址 |
| privateKey | string | 是 | 用户私钥 |
| amount | BigInteger | 是 | 授权金额 |

**响应示例**:
``json
{
  "code": 200,
  "message": "success",
  "data": "0xjkl012..." // 交易哈希
}
```

**调用示例**:
``bash
curl -X POST "http://localhost:8080/api/withdrawal/approve" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "chainName=localhost&tokenAddress=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512&privateKey=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80&amount=1000000000000000000"
```

---

## 🧪 测试专用接口

### 1. 直接给提现合约打款（Test Direct Withdraw）

**接口**: `POST /api/test/directWithdraw`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tokenAddress | string | 是 | 代币合约地址 |
| amount | BigDecimal | 是 | 提现金额（十进制数值） |

**响应示例**:
``json
{
  "code": 200,
  "message": "直接提现交易已发送",
  "txHash": "0xmno345..."
}
```

**调用示例**:
``bash
curl -X POST "http://localhost:8080/api/test/directWithdraw" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "tokenAddress=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512&amount=100.5"
```

**注意**: 此接口仅用于测试环境，生产环境应禁用。

---

## ✅ 接口清单总结

| 模块 | 接口数量 | 功能 |
|------|---------|------|
| **充值** | 4 | 列表查询、单笔查询、用户历史、统计 |
| **提现** | 13 | 列表查询、单笔查询、用户历史、统计、申请、审批、执行、授权管理等 |
| **事件监听** | 3 | 全部偏移量、单链状态、状态摘要 |
| **测试接口** | 1 | 直接提现测试 |
| **总计** | **21** | - |

---

## 🚀 启动测试

### 1. 启动应用
```bash
cd d:\study\web3-data\java\web3-asset-system
mvn spring-boot:run
```

### 2. 验证接口
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 测试充值接口
curl http://localhost:8080/api/deposit/list?page=1&size=5
```

### 3. 查看日志
```bash
# 观察控制台输出，确认接口调用正常
```
