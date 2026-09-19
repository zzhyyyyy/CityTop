# CityTop Canal 数据一致性服务

`CanalSync` 是 CityTop 仓库中的独立 Spring Boot 进程。它不参与用户请求，也不替换现有的 Cache-Aside、Redis Lua 或 RocketMQ 链路，而是监听 MySQL binlog，为 Redis 缓存补上一条异步最终一致性通道。

## 1. 本地部署结构

```text
远程 muzhiyuan 服务器
├── MySQL 127.0.0.1:3306
└── Redis 127.0.0.1:6379
          ▲
          │ SSH 本地端口转发
          │
本机
├── 127.0.0.1:13306 ──> 远程 MySQL
├── 127.0.0.1:16379 ──> 远程 Redis
├── E:\canal\server-1.1.8
│   └── Canal Server 127.0.0.1:11111
└── CityTop\CanalSync
    └── citytop-canal-sync 独立进程
```

敏感参数只保存在本机 `E:\canal\runtime.env.ps1`，不会进入 Git。MySQL 和 Redis 没有为本次接入开放公网端口，而是通过 SSH 隧道访问。

## 2. 数据处理策略

### 2.1 普通店铺缓存

当 `tb_shop` 发生 `INSERT`、`UPDATE` 或 `DELETE`：

```text
MySQL 提交事务
  → binlog
  → Canal Server
  → CanalSync 写入 cache_sync_event
  → DEL cache:shop:{id}
  → 延迟再次 DEL
  → 事件标记 SUCCESS
```

只删除缓存，不使用 binlog 内容拼装并覆盖缓存。下一次业务查询仍通过原有 Cache-Aside 逻辑从 MySQL 读取最新数据并重建 Redis。

第二次删除用于覆盖以下竞态：某次缓存未命中的旧查询已经从 MySQL 读到旧值，随后数据库事务提交并触发 Canal 删除缓存，但该旧查询最后才把旧值写回 Redis。

### 2.2 秒杀库存

`tb_seckill_voucher` 和 `tb_voucher_order` 的变更只产生库存对账信号：

```text
读取 MySQL stock
  ↕ 比较
读取 Redis seckill:stock:{voucherId}
```

不自动用 MySQL 库存覆盖 Redis。因为秒杀采用“Redis 预扣 → RocketMQ 在途 → MySQL 落单”，两者暂时不相等可能只是正常的在途订单；贸然回写会把已经预扣的库存加回去并造成超卖风险。

当前实现会指数退避重试。多次仍不一致时，事件进入 `MANUAL_REQUIRED`，由人工结合 RocketMQ 积压、失败订单和补偿记录判断。

## 3. 可靠性设计

- Canal Client 使用“不自动 ACK”模式：整批事件处理成功后才向 Canal Server ACK；异常时回滚批次。
- 每个行事件根据 binlog 文件、偏移量、表、操作类型、行序号和主键生成 SHA-256 `event_id`。
- `cache_sync_event.event_id` 是主键，`INSERT IGNORE` 保证 Canal 重放不会重复创建任务。
- 审计事件先落 MySQL，再操作 Redis，避免 Redis 失败后任务丢失。
- 初始 `PENDING` 事件带有到期时间。即使进程在“事件落库”和“删除缓存”之间崩溃，定时扫描也会接管，不会因重放幂等判断而永久搁置。
- `WAIT_VERIFY` 和 `RETRY` 事件由定时任务继续处理，重试间隔采用指数退避。
- 达到最大次数后标记为 `MANUAL_REQUIRED`，不会无限重试拖垮 Redis 或 MySQL。

这套机制提供的是“至少一次消费 + 业务幂等 + 可恢复补偿”，不是 MySQL 与 Redis 的强一致分布式事务。

## 4. 主要源码

| 文件 | 作用 |
| --- | --- |
| `CanalSyncApplication.java` | 独立服务入口，启用定时任务 |
| `CanalClientLifecycle.java` | 建立 Canal 连接、拉取批次、ACK、失败回滚与重连 |
| `CanalEventProcessor.java` | 解析行事件，生成幂等键，执行店铺缓存失效或创建秒杀对账信号 |
| `CacheReconciliationJob.java` | 扫描到期任务，执行延迟删除、库存比较、退避重试和人工介入标记 |
| `CacheSyncEventRepository.java` | 保存与更新同步审计事件，查询 MySQL 秒杀库存 |
| `CanalProperties.java` | Canal、重试和延迟复核参数模型 |
| `cache_sync_event.sql` | 同步审计表建表语句 |
| `application.yml` | 非敏感默认配置和环境变量入口 |

## 5. 审计状态

| 状态 | 含义 |
| --- | --- |
| `PENDING` | binlog 事件已持久化，直接处理尚未确认完成 |
| `WAIT_VERIFY` | 已执行第一次动作，等待延迟复核或库存对账 |
| `RETRY` | 本次处理失败，等待下一次退避重试 |
| `SUCCESS` | 缓存失效完成或库存对账通过 |
| `MANUAL_REQUIRED` | 超过最大尝试次数，需要人工判断 |

排查时优先查看：

```sql
SELECT event_id, table_name, row_key, cache_key, sync_mode,
       status, retry_count, last_error, updated_at
FROM cache_sync_event
ORDER BY updated_at DESC
LIMIT 20;
```

## 6. 本机启停

首次构建：

```powershell
cd E:\code\Java-Project\CityTop\CanalSync
mvn clean package
```

日常操作：

```powershell
E:\canal\start-all.bat
E:\canal\status.bat
E:\canal\stop-all.bat
```

`start-all.bat` 会依次完成：

1. 建立 MySQL 和 Redis 的 SSH 隧道；
2. 启动本地 Canal Server；
3. 启动 CanalSync；
4. 等待 `citytop` 订阅真正建立，而不仅仅检查进程和端口。

运行日志位于 `E:\canal\logs`。Canal Server 和 CanalSync 固定使用 `E:\canal\runtime\jdk8`，避免 Windows 下新版本 JDK 与旧版 Netty/NIO 的兼容问题。

## 7. 环境变量

代码仓库不保存密码。生产或本机运行时通过以下变量注入：

| 变量 | 用途 |
| --- | --- |
| `CITYTOP_DB_URL` | 审计服务访问 MySQL 的 JDBC 地址 |
| `CITYTOP_DB_USERNAME` / `CITYTOP_DB_PASSWORD` | 最小权限审计账户 |
| `CITYTOP_REDIS_HOST` / `CITYTOP_REDIS_PORT` / `CITYTOP_REDIS_PASSWORD` | Redis 地址与认证信息 |
| `CANAL_SERVER_HOST` / `CANAL_SERVER_PORT` | Canal Server 地址 |
| `CANAL_DESTINATION` | Canal destination，默认 `citytop` |
| `CANAL_SUBSCRIPTION` | 表订阅正则 |
| `CANAL_VERIFY_DELAY_MILLIS` | 延迟复核间隔 |
| `CANAL_RETRY_MAX_ATTEMPTS` | 最大尝试次数 |
| `CANAL_RETRY_BASE_DELAY_SECONDS` | 指数退避基准秒数 |

## 8. 测试

```powershell
cd E:\code\Java-Project\CityTop\CanalSync
mvn test
```

测试覆盖：

- 店铺事件删除缓存并创建延迟复核；
- 秒杀表事件只创建对账信号，不直接改 Redis；
- binlog 重放被审计主键幂等拦截；
- 延迟删除任务成功结束；
- 秒杀库存不一致时只重试，不覆盖 Redis。

## 9. 当前边界

- 当前为单实例 Canal Server 和单实例 CanalSync，适合本地学习与开发验证，不是生产高可用部署。
- Canal 只能处理“已成功提交到 MySQL 并写入 binlog”的变更，无法替代业务事务、RocketMQ 可靠投递或秒杀订单补偿。
- `MANUAL_REQUIRED` 目前通过数据库查询和日志发现；生产化时应接入监控告警。
- binlog 保留时间必须长于 Canal 可能的最长停机时间，否则需要重新建立位点并执行全量核对。
