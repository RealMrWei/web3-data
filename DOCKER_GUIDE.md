# Docker 环境配置指南

## 前置要求

确保已安装并运行 **Docker Desktop**。

## 快速启动

### 1. 启动所有服务

```bash
cd d:\study\web3-data
docker-compose up -d
```

首次启动会下载镜像，可能需要几分钟时间。

### 2. 查看服务状态

```bash
docker-compose ps
```

### 3. 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f redis
docker-compose logs -f kafka
```

### 4. 停止服务

```bash
docker-compose down
```

### 5. 清理数据（谨慎使用）

```bash
# 停止并删除容器、网络，但保留数据卷
docker-compose down

# 完全清理（包括数据卷）
docker-compose down -v
```

## 服务连接信息

### MySQL
- **主机**: localhost
- **端口**: 3306
- **数据库**: web3_asset
- **用户名**: web3_user
- **密码**: web3_password
- **Root 密码**: root123456

### Redis
- **主机**: localhost
- **端口**: 6379
- **密码**: redis123456

### Kafka
- **主机**: localhost
- **端口**: 9092
- **Zookeeper**: localhost:2181

## 配置文件更新

启动 Docker 服务后，需要更新 `application.yml` 中的连接配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/web3_asset?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: web3_user
    password: web3_password
  
  redis:
    host: localhost
    port: 6379
    password: redis123456
  
  kafka:
    bootstrap-servers: localhost:9092
```

## 常见问题

### 1. 端口被占用

如果提示端口已被占用，修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "3307:3306"  # 将宿主机的 3307 映射到容器的 3306
```

### 2. MySQL 连接失败

等待 MySQL 完全启动（通常需要 30-60 秒）：

```bash
docker-compose logs mysql | grep "ready for connections"
```

### 3. Kafka 启动缓慢

Kafka 依赖 Zookeeper，启动较慢，请等待 1-2 分钟。

### 4. 数据持久化

所有数据保存在 `./data` 目录下：
- `./data/mysql` - MySQL 数据
- `./data/redis` - Redis 数据
- `./data/kafka` - Kafka 数据

## 验证服务

### 测试 MySQL

```bash
docker exec -it web3-mysql mysql -uweb3_user -pweb3_password web3_asset
```

### 测试 Redis

```bash
docker exec -it web3-redis redis-cli -a redis123456
```

### 测试 Kafka

```bash
# 创建测试 Topic
docker exec -it web3-kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# 列出所有 Topic
docker exec -it web3-kafka kafka-topics --list --bootstrap-server localhost:9092
```
