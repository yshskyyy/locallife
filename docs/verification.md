# v1.0.0 验收记录

本文只记录实际执行过的命令和结果。密码、Token、手机号等运行数据均不记录或使用脱敏占位符。

## 验收环境

- 日期：2026-08-24
- Java：17
- 构建工具：Maven
- 容器运行时：Docker Engine（Colima）

## 已完成检查

### Compose 静态配置

```bash
docker compose config --quiet
docker compose config --services
```

结果：配置校验通过，解析出的服务为 `postgres`、`redis`、`backend`。后端数据库和 Redis 地址分别使用 Compose 服务名 `postgres`、`redis`。

### 自动化测试与打包

```bash
mvn test
mvn package
```

结果：两条命令均为 `BUILD SUCCESS`。共执行 9 个测试，失败 0、错误 0、跳过 0。

覆盖的测试类：

- `AuthServiceTest`
- `LoginInterceptorTest`
- `CacheClientTest`
- `VoucherOrderTransactionServiceTest`

## 容器验收状态

执行过以下隔离项目启动命令，使用了非默认宿主机端口和临时测试密码：

```bash
COMPOSE_PROJECT_NAME=phase0_delivery_verify \
POSTGRES_PORT=25432 REDIS_PORT=26379 BACKEND_PORT=28080 \
POSTGRES_PASSWORD=<REDACTED> REDIS_PASSWORD=<REDACTED> \
docker compose up --build -d
```

结果：未进入容器创建阶段。Docker Hub 的 `registry-1.docker.io` 请求超时，`redis:7.4-alpine` 等基础镜像无法拉取。随后单独执行 `docker compose pull postgres redis` 和通过 `curl` 检查 Docker Registry，均因连接超时失败。本次失败未创建该 Compose 项目的容器、网络或数据卷。

因此，下列项目目前不得标记为已通过，需在镜像仓库网络恢复后重新执行：

- 全新数据卷启动及三个服务 `healthy`
- PostgreSQL 初始化表结构
- 容器环境下登录、Token 恢复用户上下文
- 商户查询写入 Redis 缓存
- 秒杀返回 `PROCESSING`、Stream 消费、订单落库与库存扣减
- Pending 为 0、消息 ACK、重复下单和库存不足验证
- `docker compose down -v` 清理及第二次空数据复现

## 待执行命令

网络恢复后，从仓库根目录执行：

```bash
docker compose up --build -d
docker compose ps
docker compose logs --tail=200 postgres redis backend
```

按 README 的登录、商户和秒杀步骤验收后执行：

```bash
docker compose down -v
docker compose up --build -d
docker compose ps
```

第二轮验收结束后：

```bash
docker compose down -v
```

只有完成上述真实运行验证后，才应把相应条目更新为“通过”。
