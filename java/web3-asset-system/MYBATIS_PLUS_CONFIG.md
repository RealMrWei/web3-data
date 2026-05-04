# MyBatis-Plus Mapper 与自动填充配置完成说明

## ✅ 已完成内容

### 1. 创建的 Mapper 接口（4个）

#### ChainConfigMapper
```java
@Mapper
public interface ChainConfigMapper extends BaseMapper<ChainConfig> {
}
```
**功能**：链配置数据访问层，支持 CRUD 操作

---

#### UserAssetMapper
```java
@Mapper
public interface UserAssetMapper extends BaseMapper<UserAsset> {
}
```
**功能**：用户资产数据访问层，支持余额查询、资产列表等

---

#### DepositRecordMapper
```java
@Mapper
public interface DepositRecordMapper extends BaseMapper<DepositRecord> {
}
```
**功能**：充值记录数据访问层，已在消费者中使用

---

#### WithdrawalOrderMapper
```java
@Mapper
public interface WithdrawalOrderMapper extends BaseMapper<WithdrawalOrder> {
}
```
**功能**：提现订单数据访问层，已在消费者中使用

---

### 2. MyBatis-Plus 配置类

#### MyBatisPlusConfig.java
```java
@Configuration
public class MyBatisPlusConfig {
    
    // ✅ 分页插件（支持 MySQL）
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
    
    // ✅ 自动填充处理器
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.setMetaObjectHandler(new MyMetaObjectHandler());
        };
    }
}
```

**核心功能**：
1. **分页插件**：支持 `Page<T>` 对象进行分页查询
2. **乐观锁插件**：配合 `@Version` 注解实现乐观锁
3. **自动填充**：自动设置 [createTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\UserAsset.java#L43-L44)、[updateTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\UserAsset.java#L46-L47) 字段

---

### 3. MetaObjectHandler 自动填充规则

#### 插入时（INSERT）
```java
@Override
public void insertFill(MetaObject metaObject) {
    this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
}
```

**效果**：
- ✅ 插入记录时，[createTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\UserAsset.java#L43-L44) 和 [updateTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\UserAsset.java#L46-L47) 自动设置为当前时间
- ✅ 无需手动调用 `setCreateTime()` 和 `setUpdateTime()`

---

#### 更新时（UPDATE）
```java
@Override
public void updateFill(MetaObject metaObject) {
    this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
}
```

**效果**：
- ✅ 更新记录时，[updateTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\UserAsset.java#L46-L47) 自动更新为当前时间
- ✅ [createTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\UserAsset.java#L43-L44) 保持不变

---

## 📊 实体类自动填充配置汇总

| 实体类 | createTime | updateTime | 其他自动填充字段 |
|--------|-----------|-----------|----------------|
| **ChainConfig** | ✅ INSERT | ✅ INSERT_UPDATE | - |
| **UserAsset** | ✅ INSERT | ✅ INSERT_UPDATE | - |
| **DepositRecord** | ✅ INSERT | ✅ INSERT_UPDATE | - |
| **WithdrawalOrder** | ✅ INSERT | ✅ INSERT_UPDATE | [completedTime](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\WithdrawalOrder.java#L66-L67): UPDATE |
| **EventListenerOffset** | ❌ | ✅ INSERT_UPDATE | [version](file://d:\study\web3-data\java\web3-asset-system\src\main\java\com\web3\entity\EventListenerOffset.java#L50-L51): 乐观锁 |

---

## 🎯 使用示例

### 1. 插入记录（自动填充时间）
```java
DepositRecord record = new DepositRecord();
record.setTxHash("0xabc...");
record.setUserAddress("0x123...");
record.setAmount(new BigDecimal("1000000000000000000"));

// ✅ 无需手动设置 createTime 和 updateTime
depositRecordMapper.insert(record);

// 数据库中：
// create_time = 2024-01-01 12:00:00（自动填充）
// update_time = 2024-01-01 12:00:00（自动填充）
```

---

### 2. 更新记录（自动更新时间）
```java
DepositRecord record = depositRecordMapper.selectById(1);
record.setStatus(1); // 修改状态

// ✅ 无需手动设置 updateTime
depositRecordMapper.updateById(record);

// 数据库中：
// update_time = 2024-01-01 12:05:00（自动更新）
// create_time = 2024-01-01 12:00:00（保持不变）
```

---

### 3. 分页查询
```java
// ✅ 使用分页插件
Page<DepositRecord> page = new Page<>(1, 10); // 第1页，每页10条
LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(DepositRecord::getChainName, "ethereum");

Page<DepositRecord> result = depositRecordMapper.selectPage(page, wrapper);

System.out.println("总记录数: " + result.getTotal());
System.out.println("当前页数据: " + result.getRecords());
```

---

### 4. 乐观锁更新
```java
// EventListenerOffset 实体有 @Version 注解
EventListenerOffset offset = offsetMapper.selectById(1);
offset.setLastProcessedBlock(200);

// ✅ 自动处理 version 字段
// SQL: UPDATE event_listener_offset 
//      SET last_processed_block = 200, version = version + 1
//      WHERE id = 1 AND version = 0
offsetMapper.updateById(offset);
```

---

## 🔑 关键技术点

### 1. BaseMapper 继承
所有 Mapper 接口继承 `BaseMapper<T>`，自动获得以下方法：
- `selectById()` / `selectList()` / `selectPage()`
- `insert()` / `updateById()` / `deleteById()`
- `selectCount()` / `exists()`

---

### 2. LambdaQueryWrapper 类型安全
```java
// ✅ 编译期检查，字段名写错会报错
LambdaQueryWrapper<DepositRecord> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(DepositRecord::getTxHash, "0xabc...");

// ❌ 传统写法（字符串硬编码，容易出错）
QueryWrapper<DepositRecord> wrapper = new QueryWrapper<>();
wrapper.eq("tx_hash", "0xabc...");  // 字段名写错也不会报错
```

---

### 3. 自动填充原理
MyBatis-Plus 通过反射在 SQL 执行前拦截：
1. **插入时**：检查字段是否有 `@TableField(fill = FieldFill.INSERT)` 注解
2. **更新时**：检查字段是否有 `@TableField(fill = FieldFill.UPDATE)` 或 `INSERT_UPDATE` 注解
3. **调用 MetaObjectHandler**：自动设置字段值

---

## 📝 注意事项

### 1. 实体类必须添加注解
```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

**如果没有注解**：自动填充不会生效！

---

### 2. 字段类型必须匹配
```java
// ✅ 正确：LocalDateTime
private LocalDateTime createTime;

// ❌ 错误：Date 类型无法自动填充
private Date createTime;
```

---

### 3. 乐观锁需要 @Version 注解
```java
@Version
private Long version;
```

**作用**：
- ✅ 更新时自动递增 version
- ✅ WHERE 条件中自动添加 `AND version = ?`
- ✅ 防止并发更新冲突

---

## ✅ 验证清单

- [x] 创建了 4 个 Mapper 接口
- [x] 配置了 MyBatisPlusInterceptor（分页 + 乐观锁）
- [x] 配置了 MetaObjectHandler（自动填充）
- [x] 所有实体类已添加 `@TableField(fill = ...)` 注解
- [x] 编译通过，无语法错误

---

## 🚀 下一步建议

1. **测试自动填充功能**：启动应用，插入一条记录，验证时间字段是否自动填充
2. **创建 REST API**：实现充值/提现查询接口
3. **全局异常处理**：统一异常返回格式
