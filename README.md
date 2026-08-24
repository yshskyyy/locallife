# Local Review Platform

基于 Java 17、Spring Boot 3.5.14、PostgreSQL 和 Redis 的本地点评单体项目。当前 Docker Compose 交付保持 Phase 0 核心业务逻辑不变，包含验证码登录、商户缓存、Redis Lua 秒杀及 Redis Stream 异步下单。

当前工程版本：`1.0.0`。只有完成完整运行态验收后才创建并推送 Git 标签 `v1.0.0`。

## 技术栈

- Java 17、Spring Boot 3.5.14、Spring MVC、Spring Data JPA
- PostgreSQL 16、Redis 7.4
- Redis Lua、Redis Stream、Maven、Docker Compose
- JUnit 5、Mockito

## 当前架构

```text
HTTP Client
    |
Spring Boot 单体应用
    |-- LoginInterceptor / UserContext
    |-- 用户、商户、评价、优惠券与订单业务
    |-- Redis Lua 秒杀准入 -> Redis Stream -> 异步消费者
    |                         |
    |-- PostgreSQL <----------+ 事务扣库存并写订单
    |
    +-- Redis：Token、缓存、秒杀库存、一人一单、Stream
```

这是刻意保持边界清晰的初始稳定单体版本，不包含微服务拆分。核心业务包括：验证码登录与真实用户创建、Token 会话、商户与评价、商户缓存，以及带库存校验和一人一单约束的异步秒杀下单。

## 环境要求

- Docker Engine 或 Docker Desktop
- Docker Compose v2
- 首次构建时能够访问 Maven Central 和 Docker 镜像仓库
- 建议至少预留 2 GB 可用内存

本地直接运行测试还需要 Java 17 和 Maven 3.9+；只使用容器启动时不需要在宿主机安装 Java、Maven、PostgreSQL 或 Redis。

## 一键启动

可选但推荐：先创建本地环境文件并替换其中的占位密码。

```bash
cp .env.example .env
```

`.env` 已加入 `.gitignore`，不要提交真实密码或密钥。若不创建 `.env`，Compose 会使用仅供本机演示的默认占位密码。

构建并启动全部服务：

```bash
docker compose up --build -d
```

查看容器状态，三个服务最终都应为 `healthy`：

```bash
docker compose ps
```

查看后端启动日志：

```bash
docker compose logs -f backend
```

默认端口：

| 服务 | 宿主机地址 | 容器内地址 |
|---|---|---|
| 后端 | `localhost:8080` | `backend:8080` |
| PostgreSQL | `localhost:5432` | `postgres:5432` |
| Redis | `localhost:6379` | `redis:6379` |

后端容器通过服务名 `postgres` 和 `redis` 连接依赖，不使用 `localhost`。

## 数据库初始化

首次创建 `postgres_data` 数据卷时，PostgreSQL 官方镜像自动执行：

```text
src/main/resources/schema.sql
```

脚本通过 `/docker-entrypoint-initdb.d/001-schema.sql` 只读挂载。应用同时使用 `spring.jpa.hibernate.ddl-auto=validate` 校验表结构，不会由 Hibernate 自动修改数据库。

注意：PostgreSQL 只会在空数据目录第一次初始化时执行该脚本。修改已有环境的表结构时，应增加正式 Migration，而不是修改数据卷中的历史结果。需要重新验证全新数据库时使用下文的“清除本地数据”命令。

## 功能验证

### 1. 检查 Redis 与后端

```bash
curl http://localhost:8080/test/redis
```

预期返回统一成功响应。

### 2. 发送验证码并登录

Phase 0 开发验证码固定为 `123456`，仅用于本地演示。

```bash
curl -X POST 'http://localhost:8080/api/user/code?phone=0499000001'

curl -X POST 'http://localhost:8080/api/user/login' \
  -H 'Content-Type: application/json' \
  -d '{"phone":"0499000001","code":"123456"}'
```

从登录响应中复制 `data.token`：

```bash
curl http://localhost:8080/api/user/me \
  -H 'Authorization: Bearer <TOKEN>'
```

### 3. 创建并查询商户缓存

```bash
curl -X POST http://localhost:8080/api/businesses \
  -H 'Authorization: Bearer <TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"name":"Docker Cafe","category":"Cafe","address":"Shanghai","rating":4.8,"longitude":121.4737,"latitude":31.2304}'

curl http://localhost:8080/api/businesses/<BUSINESS_ID> \
  -H 'Authorization: Bearer <TOKEN>'
```

### 4. 验证秒杀

项目当前没有优惠券管理接口，可通过 PostgreSQL 创建一条本地测试优惠券：

```bash
docker compose exec postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "INSERT INTO voucher(title, stock, begin_time, end_time) VALUES ('"'"'Compose Seckill'"'"', 1, CURRENT_TIMESTAMP - INTERVAL '"'"'1 minute'"'"', CURRENT_TIMESTAMP + INTERVAL '"'"'1 hour'"'"');"'
```

查询 ID 后发起秒杀：

```bash
curl -X POST http://localhost:8080/api/voucher/seckill/<VOUCHER_ID> \
  -H 'Authorization: Bearer <TOKEN>'
```

接口返回 `PROCESSING` 后，可检查数据库订单：

```bash
docker compose exec postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id, user_id, voucher_id, create_time FROM voucher_order ORDER BY id DESC;"'
```

## 运行测试

宿主机已安装 Java 17 和 Maven 时：

```bash
mvn test
mvn package
```

Docker 多阶段构建也会在独立 Maven 构建阶段生成可执行 Jar；现有测试应在构建镜像前单独执行，以便失败时快速反馈。

实际验收命令、结果及尚未通过的项目记录在 [`docs/verification.md`](docs/verification.md)。该文档只记录真实执行结果。

## 停止

停止容器并保留 PostgreSQL、Redis 数据：

```bash
docker compose down
```

停止并删除本项目数据卷：

```bash
docker compose down -v
```

`down -v` 会永久删除 Compose 中的 PostgreSQL 和 Redis 数据，只应在确认不需要本地数据时执行。

## 故障排查

### 端口被占用

若本机已有 PostgreSQL、Redis 或其他 8080 服务，可在 `.env` 中修改：

```dotenv
POSTGRES_PORT=15432
REDIS_PORT=16379
BACKEND_PORT=18080
```

这些只改变宿主机映射端口；容器内仍使用 `postgres:5432` 和 `redis:6379`。

### 后端一直不健康

```bash
docker compose ps
docker compose logs --tail=200 postgres redis backend
```

重点检查：

- PostgreSQL 初始化脚本是否执行成功；
- `.env` 中数据库与 Redis 密码是否一致；
- Maven Central 或镜像仓库是否可访问；
- 后端日志是否出现 JPA Schema validation 错误。

### 修改密码后无法连接

PostgreSQL 环境变量仅在数据卷第一次初始化时生效。若修改 `.env` 密码但继续使用旧数据卷，旧密码仍保存在数据库中。开发环境可在确认数据不需要后执行：

```bash
docker compose down -v
docker compose up --build -d
```

### 强制检查 Compose 配置

```bash
docker compose config --quiet
```

该命令可发现 YAML、变量替换和服务依赖配置错误。

## 已知限制

- 验证码固定为 `123456`，只适合本地学习演示，没有接入短信服务。
- 当前是单体应用，没有 Nacos、Gateway、OpenFeign、服务拆分或分布式事务框架。
- 秒杀接口返回异步受理状态，未提供基于 `requestId` 的订单结果查询接口。
- Redis Stream 使用单个固定消费者名，适合单实例演示；多实例部署前需要生成唯一消费者名并完善消息可观测性。
- 死信记录和 Redis 预占补偿已实现，但没有死信管理后台或人工重放接口。
- 数据库初始化使用幂等 SQL；后续正式演进表结构时应引入 Flyway 或 Liquibase 版本化 Migration。
