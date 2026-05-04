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

## 👂 事件监听数据查询（EventListenerController）

### 1. 查询所有链的监听偏移量

**接口**: `GET /api/event-listener/offsets`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "chainName": "ethereum",
      "lastProcessedBlock": 12345,
      "isSynced": true,
      "version": 100,
      "updateTime": "2024-01-01T12:00:00"
    },
    {
      "id": 2,
      "chainName": "bnb",
      "lastProcessedBlock": 23456,
      "isSynced": true,
      "version": 80,
      "updateTime": "2024-01-01T12:00:00"
    }
  ]
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/event-listener/offsets"
```

---

### 2. 根据链名称查询监听状态

**接口**: `GET /api/event-listener/offset/{chainName}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| chainName | string | 链名称 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "chainName": "ethereum",
    "lastProcessedBlock": 12345,
    "isSynced": true,
    "version": 100,
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/event-listener/offset/ethereum"
```

---

### 3. 获取监听状态摘要（简化版）

**接口**: `GET /api/event-listener/status`

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "chainName": "ethereum",
      "lastProcessedBlock": 12345,
      "isSynced": true,
      "updateTime": "2024-01-01T12:00:00"
    },
    {
      "chainName": "bnb",
      "lastProcessedBlock": 23456,
      "isSynced": true,
      "updateTime": "2024-01-01T12:00:00"
    }
  ]
}
```

**调用示例**:
```bash
curl "http://localhost:8080/api/event-listener/status"
```

---

## ⚠️ 异常处理

### 统一错误响应格式

```json
{
  "code": 400,
  "message": "参数校验失败",
  "errors": {
    "field1": "错误信息1",
    "field2": "错误信息2"
  }
}
```

### 常见错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 🧪 测试用例

### 1. 测试充值记录查询
```bash
# 查询所有充值记录
curl "http://localhost:8080/api/deposit/list?page=1&size=5"

# 查询以太坊链的成功充值
curl "http://localhost:8080/api/deposit/list?chainName=ethereum&status=1"

# 查询特定交易
curl "http://localhost:8080/api/deposit/tx/0xabc123..."

# 查询用户充值历史
curl "http://localhost:8080/api/deposit/user/0x123...?page=1&size=10"

# 统计充值数据
curl "http://localhost:8080/api/deposit/statistics"
```

---

### 2. 测试提现订单查询
```bash
# 查询所有提现订单
curl "http://localhost:8080/api/withdrawal/list?page=1&size=5"

# 查询已完成的提现
curl "http://localhost:8080/api/withdrawal/list?status=2"

# 查询特定订单
curl "http://localhost:8080/api/withdrawal/order/WD20240101120000"

# 查询用户提现历史
curl "http://localhost:8080/api/withdrawal/user/0x123...?page=1&size=10"

# 统计提现数据
curl "http://localhost:8080/api/withdrawal/statistics"
```

---

### 3. 测试事件监听状态
```bash
# 查询所有链的监听状态
curl "http://localhost:8080/api/event-listener/offsets"

# 查询以太坊链的监听状态
curl "http://localhost:8080/api/event-listener/offset/ethereum"

# 获取简化状态摘要
curl "http://localhost:8080/api/event-listener/status"
```

---

## 📊 前端集成示例

### Vue.js 示例
```javascript
// 查询充值记录
async function getDeposits(page = 1, size = 10) {
  const response = await fetch(`/api/deposit/list?page=${page}&size=${size}`);
  const result = await response.json();
  
  if (result.code === 200) {
    return {
      list: result.data,
      total: result.total,
      pages: result.pages
    };
  } else {
    throw new Error(result.message);
  }
}

// 统计充值数据
async function getDepositStatistics() {
  const response = await fetch('/api/deposit/statistics');
  const result = await response.json();
  
  if (result.code === 200) {
    return result.data;
  } else {
    throw new Error(result.message);
  }
}
```

---

### React 示例
```javascript
// 查询提现订单
async function getWithdrawals(page = 1, size = 10) {
  const response = await fetch(`/api/withdrawal/list?page=${page}&size=${size}`);
  const result = await response.json();
  
  if (result.code === 200) {
    return {
      list: result.data,
      total: result.total,
      pages: result.pages
    };
  } else {
    throw new Error(result.message);
  }
}

// 查询监听状态
async function getListenerStatus() {
  const response = await fetch('/api/event-listener/status');
  const result = await response.json();
  
  if (result.code === 200) {
    return result.data;
  } else {
    throw new Error(result.message);
  }
}
```

---

## ✅ 接口清单总结

| 模块 | 接口数量 | 功能 |
|------|---------|------|
| **充值** | 4 | 列表查询、单笔查询、用户历史、统计 |
| **提现** | 4 | 列表查询、单笔查询、用户历史、统计 |
| **事件监听** | 3 | 全部偏移量、单链状态、状态摘要 |
| **总计** | **11** | - |

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
