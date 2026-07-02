# Middleware

Zyn 项目中间件服务，基于 Docker Compose 统一管理。

## 组件

| 组件 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| PostgreSQL | postgis/postgis:16-3.4-alpine | 5432 | 数据库 |
| Redis | redis:7.4-alpine | 6379 | 缓存 |
| Kafka | apache/kafka:3.7.1 | 9092 | 消息队列 |
| Elasticsearch | 自定义构建（含 IK/Pinyin/STConvert） | 9200 | 全文检索 |
| INFINI Console | infinilabs/console:1.30.2-2396 | 9000 | ES 可视化管理 |
| SeaweedFS | chrislusf/seaweedfs:4.29 | 8333 | S3 对象存储 |

## 快速开始

### 1. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填写实际密码
```

### 2. 配置 SeaweedFS S3 认证

```bash
cp seaweedfs/s3.conf.example seaweedfs/s3.conf
# 编辑 s3.conf，填写实际 accessKey / secretKey
```

### 3. 构建自定义镜像

```bash
# 构建 Elasticsearch（含 IK/Pinyin/STConvert 插件）
docker-compose build elasticsearch
```

### 4. 启动所有服务

```bash
# 推荐：使用启动脚本（自动修复权限）
sudo ./start.sh

# 或手动启动
docker-compose up -d
```

### 5. 权限说明

Docker 挂载的宿主机目录默认由 root 创建，容器内非 root 用户无法写入。`start.sh` 会自动处理以下目录权限：

| 服务 | 目录 | UID |
|------|------|-----|
| PostgreSQL | postgres/logs | 999 |
| Redis | redis/logs | 999 |
| Kafka | kafka/data | 1000 |
| Elasticsearch | elasticsearch/data, elasticsearch/logs | 1000 |

手动修复命令：

```bash
sudo chown -R 999:999   postgres/logs redis/logs
sudo chown -R 1000:1000 kafka/data elasticsearch/data elasticsearch/logs
```

### 5. 验证服务状态

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看单个服务日志
docker-compose logs -f postgres
docker-compose logs -f redis
docker-compose logs -f kafka
docker-compose logs -f elasticsearch
```

## 验证各组件

### PostgreSQL

```bash
# 连接数据库
docker-compose exec postgres psql -U postgres -d zyn_base

# 检查 PostGIS 扩展
SELECT PostGIS_Version();
```

### Redis

```bash
docker-compose exec redis redis-cli -a 'Zyn@secure#99' ping
# 返回 PONG 表示正常
```

### Kafka

```bash
# 创建 Topic
docker-compose exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test --partitions 3 --replication-factor 1

# 生产消息
docker-compose exec kafka kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test

# 消费消息
docker-compose exec kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
```

### Elasticsearch

```bash
# 集群健康检查
curl http://localhost:9200/_cluster/health

# 查看已安装插件（含 IK 分词器）
curl http://localhost:9200/_cat/plugins

# 测试中文分词
curl -X POST "http://localhost:9200/_analyze" -H "Content-Type: application/json" -d '{"analyzer": "ik_smart", "text": "中华人民共和国"}'
```

### SeaweedFS (S3)

```bash
# 检查 S3 服务
curl http://localhost:8333

# 查看 Bucket
aws --endpoint-url http://localhost:8333 s3 ls
```

### INFINI Console

```bash
# 访问管理界面
open http://localhost:9000

# 登录账号：admin / 见 .env POSTGRES_PASSWORD（统一密码）
# 添加 ES 集群：http://elasticsearch:9200
```

## 常用命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 停止并清除数据卷
docker-compose down -v

# 重启单个服务
docker-compose restart redis

# 查看资源占用
docker stats

# 重建镜像并重启
docker-compose up -d --build
```

## 数据目录

所有数据持久化在各组件同名目录下：

```
middleware/
├── postgres/
│   ├── data/       # 数据库文件
│   └── logs/       # 日志
├── redis/
│   ├── data/       # AOF/RDB 持久化
│   └── logs/       # 日志
├── kafka/
│   └── data/       # 消息/分区数据
├── elasticsearch/
│   ├── data/       # 索引数据
│   ├── logs/       # 日志
│   └── plugins/    # IK/Pinyin/STConvert 插件包
├── seaweedfs/
│   ├── master/     # 元数据
│   ├── volume/     # 文件数据
│   └── s3.conf     # S3 认证配置
└── infini-console/
    ├── config/     # 配置文件
    ├── data/       # 运行数据
    └── logs/       # 日志
```

## 清理数据

```bash
# 清空所有组件数据（谨慎操作）
rm -rf postgres/data redis/data kafka/data elasticsearch/data seaweedfs/master seaweedfs/volume

# 重新启动
docker-compose up -d
```

## 凭证信息

所有组件使用统一密码，详见 `.env` 文件。

| 组件 | 用户名 | 密码 |
|------|--------|------|
| PostgreSQL | postgres | 见 .env POSTGRES_PASSWORD |
| Redis | - | 见 .env REDIS_PASSWORD |
| Elasticsearch | elastic | 见 .env ELASTIC_PASSWORD |
| SeaweedFS S3 | 见 .env S3_ACCESS_KEY | 见 .env S3_SECRET_KEY |
| INFINI Console | admin | 见 .env POSTGRES_PASSWORD（统一密码） |
