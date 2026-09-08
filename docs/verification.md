# v1.0.0 Docker 与完整 E2E 验收记录

本文只记录 2026-09-08 实际执行并观察到的结果，不记录密码、Token 或本机代理凭据。

## 环境

- macOS / Colima / Docker Compose v2
- Java 17 / Maven
- React + Vite + TypeScript + Nginx
- Spring Boot + PostgreSQL 16 + Redis 7.4

## 构建结果

实际执行：

```bash
mvn test
mvn clean package
npm --prefix frontend run build
docker compose down -v
docker compose up --build -d
docker compose ps
```

- `mvn test`：PASS，16 tests，0 failures，0 errors，0 skipped。
- `mvn clean package`：PASS。
- 前端 Vite production build：PASS。
- Compose 从空数据卷完成构建和初始化。
- `frontend`、`backend`、`postgres`、`redis`：全部 healthy。

## 浏览器 E2E

使用 `http://localhost:3000` 的实际页面完成：

| 验收项 | 实际结果 |
|---|---|
| 前端打开 | PASS，显示 Login 页面 |
| 验证码登录 | PASS，验证码 `123456` |
| 当前用户 | PASS，显示 `user_0001` 和对应手机号 |
| 创建品牌 | PASS，创建 `Harbor Tea` |
| 创建门店 | PASS，创建并关联品牌 |
| 编辑门店 | PASS，页面内编辑后名称持久化 |
| Consumer 门店列表 | PASS，显示真实后端门店 |
| Merchant Console 门店列表 | PASS |
| 创建促销 | PASS，库存 1，状态 ACTIVE |
| Consumer 显示促销 | PASS |
| 首次秒杀 | PASS，UI 显示 `Seckill result: PROCESSING` |
| 重复下单 | PASS，同用户 UI 显示 `DUPLICATE_ORDER` |
| 库存不足 | PASS，第二用户 UI 显示 `OUT_OF_STOCK` |
| 页面刷新 | PASS，Token 用户保持登录，显示 SOLD_OUT、库存 0、订单 1 |

## 角色与 My Vouchers 增量验收

从空卷重新构建后，在真实浏览器中完成：

| 验收项 | 实际结果 |
|---|---|
| MERCHANT 登录角色 | PASS，`/api/user/me` 驱动页面显示 MERCHANT |
| MERCHANT 默认页面 | PASS，默认进入 Merchant Console |
| MERCHANT 创建 Brand/Store/Voucher | PASS |
| USER 导航隔离 | PASS，只显示 Explore / My |
| USER 商户写权限 | PASS，POST Brand 返回 HTTP 403、`FORBIDDEN`、`Merchant role required` |
| USER A 秒杀 | PASS，显示 `Request accepted / Processing` |
| USER A My | PASS，落库后显示活动、门店、有效期和 `AVAILABLE` |
| USER B 库存不足 | PASS，显示 `OUT_OF_STOCK` |
| USER B 数据隔离 | PASS，My 显示 `No vouchers yet.` |
| EXPIRED 计算 | PASS，自动化测试覆盖 endTime 已过期场景 |

数据库角色证据：本地引导账号为 `MERCHANT`，两个消费者账号均为 `USER`。订单仅属于 USER A；USER B 无订单。

## Redis 证据

实际只读查询了 `MGET seckill:stock:1`、`XLEN stream.orders`、`XPENDING stream.orders order-group`、`EXISTS shop:detail:1` 和 `XRANGE stream.orders - +`。

- Redis 秒杀库存：`0`。
- `stream.orders` 长度：`1`。
- Stream 消息包含真实 `userId=1`、`voucherId=1` 和随机 requestId。
- 消费组 `order-group` Pending 数量：`0`。
- 门店详情缓存 `shop:detail:1` 存在：`1`。

## PostgreSQL 证据

实际查询品牌、门店、活动、订单和重复分组：

- `brand`：1 行，`Harbor Tea`。
- `business`：1 行，关联 `brand_id=1`，编辑后的名称已持久化。
- `voucher`：1 行，`business_id=1`，数据库库存 `0`。
- `voucher_order`：1 行，`user_id=1`、`voucher_id=1`。
- 订单数：1；distinct user 数：1。
- 重复 `(user_id, voucher_id)` 分组数：`0`。
- 表结构存在唯一约束 `uk_voucher_order_user_voucher`。

## Clean-volume 与数据初始化

最终验收开始前实际执行 `docker compose down -v`，随后执行 `docker compose up --build -d`。PostgreSQL 空卷自动执行 `schema.sql`，应用通过 schema validation 并健康启动。当前容器保留运行，包含上述演示数据，便于人工复核。

## 结论

完整前后端 Docker E2E 已通过：浏览器 UI、后端 API、Redis 预扣与 Stream、PostgreSQL 事务落库、数据库扣库存、ACK/Pending=0、重复订单和库存不足均有实际运行证据。未执行 git commit、push、merge 或 tag。

## 已知非阻塞事项

- 本地演示验证码固定，不适用于生产。
- 商家界面为最小功能闭环，未实现正式 RBAC。
- 本次未提交截图文件，README 提供了可人工截图的页面入口。
