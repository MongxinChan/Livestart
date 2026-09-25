# Livesatrt
LiveStart 是专门为演唱会、Livehouse 演出设计的在线抢票引擎。针对“开票瞬间流量激增万倍”的典型业务场景，系统旨在解决极短时间内海量用户涌入导致的系统瘫痪、库存超卖以及黄牛脚本刷票等核心问题。

## 上传规定
由于自己上传的时候，容易乱，因此这里有一些上传条例：
```text
<type>: <description>
```
### Type Definitions
| **类型 (Type)** | **关键词** | **适用场景**                                            |
| --------------- | ---------- | ------------------------------------------------------- |
| **feat**        | 新功能     | 新增特性、功能、UI 组件等                   |
| **fix**         | 修补       | 修复 Bug，如文字渲染位置偏移、点击失效等                |
| **optimize**    | **优化**   | **重构非 Bug 的代码、改进交互逻辑、精简代码结构**       |
| **perf**        | 性能       | 专门针对性能的优化      |
| **docs**        | 文档       | 修改 README、项目注释、技术文档                         |
| **style**       | 样式       | 不影响逻辑的格式调整（空格、缩进、变量重命名等）        |
| **config**      | 配置       | 修改 Vite/Webpack 配置、环境变量、依赖项 (package.json) |
| **refactor**    | 重构       | 代码架构的大改动（既不是新增功能也不是修复 Bug）        |
| **chore**       | 事务       | 构建流程更新、辅助工具变动（如 CI/CD 脚本）             |

**原子化***：尽量一次提交只处理一件事情。
**清晰描述**：<description> 建议简明扼要地概括改动点，例如：feat: 增加了票务核销功能

详细的设计条例请参考[飞书文档](https://jcncb5q6awco.feishu.cn/wiki/CvpdwtartiehGVky8gKc3s73nXf)

---

## 🎨 赛博多主题 Vue 3 高并发演示客户端 (Client)
为了让项目具备完美的毕业设计答辩演示效果，我们在项目根目录下新增了 [client](file:///./client) 目录。
- **视觉风格**：融合大麦与秀动，支持**赛博暗黑、高雅极简、大麦炽红、秀动荧光绿** 4 套配色皮肤一键重绘！
- **顶部高保真导航栏**：支持广场发现、订单中心、商户结算看板的多维视图无缝平滑路由！
- **抢票控制舱**：可交互式模拟防刷 Token 动态获取、RocketMQ 并发排队落库及超时关单库存自愈全生命周期。
- **商户结算看板**：可视化穿透对账扫描 `ds_order` 中 `t_order_item_0 ~ 15` 16张物理订单分表。
- **极速启动**：支持**免 NPM 依赖双击直接运行**（离线高真 Mock 模式）与 **Vite + 网关反向代理真实联调**双核自适应启动！

具体详细说明及开发操作请查阅：[client/README.md](./client/README.md)
## Docker 补充说明

当前仓库的 Docker 资源主要位于 `docker/xxl-job/docker-compose.yml`，用于启动 XXL-JOB Admin，方便本地联调 `distribution` 服务中的定时放票、开售提醒等任务调度能力。

项目目前**没有**提供整套微服务的一键 Compose 编排；前端、网关和各业务服务仍建议按本地开发方式分别启动。

如需具体启动方法、依赖要求和访问地址，请查看 `client/README.md` 中新增的 `Docker 说明` 章节。

## 数据库初始化

全新环境只需要执行整合入口 [`sql/00_livestart_full_schema.sql`](./sql/00_livestart_full_schema.sql)。该脚本会按依赖顺序重建 `live_start`、用户/订单/座位分片库、`xxl_job` 和默认单库支付库 `live_start_pay`，只能用于全新环境，禁止对已有数据的数据库执行。

```text
00_livestart_full_schema.sql
```

毕业设计默认使用整合脚本内的 `live_start_pay` 单库支付模式。需要支付分片和演示种子数据时，再执行 `02_livestart_optional_extras.sql`。

三个入口脚本由原有结构脚本按依赖顺序合并生成，避免初始化和升级逻辑分散在多个文件中。

脚本职责如下：

| 场景 | 执行脚本 |
| --- | --- |
| 全新环境 | `00_livestart_full_schema.sql` |
| 旧库迁移与增量修复 | `01_livestart_existing_db_upgrade.sql` |
| 支付分库分表与测试数据 | `02_livestart_optional_extras.sql`（按需执行） |

已有库升级时执行 `01_livestart_existing_db_upgrade.sql`。该文件会按顺序创建分片迁移存储过程，并补齐核销索引、核销记录、分销关联、结算通知和艺人钱包结构。
然后以 `FALSE` 参数查看迁移计划，再停写、备份并以 `TRUE` 参数执行：

```sql
CALL live_start.sp_migrate_order_shards_hash16_to_hash32(FALSE);
CALL live_start.sp_migrate_user_ticket_shards(FALSE);
CALL live_start.sp_migrate_user_shards(FALSE);
```

`02_livestart_optional_extras.sql` 同时包含支付分片结构和测试种子数据；默认单库支付环境不需要执行它。

数据库初始化或升级后可运行 `tools/verify_livestart_database.ps1` 做只读结构与分片检查。

## 本地验证码演示

当前手机号登录使用模拟短信，不调用收费短信服务。本地运行 `admin` 服务时设置
`LIVESTART_SMS_MOCK_LOG_ENABLED=true`，验证码会输出到服务日志；也可设置
`LIVESTART_SMS_MOCK_CODE=888888` 使用固定演示验证码。不要在公开环境使用固定验证码。
生产环境未配置真实短信通道时，发送验证码接口会明确返回失败。
