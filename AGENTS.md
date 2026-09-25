# LiveStart Agent Guide

## 工作规则

- 全程使用中文。遵循 KISS，先调研，再确认方案、拆解、实施、验证。
- 做最小范围修改。保留工作区已有改动，不回退无关文件。
- 未经明确要求，不 commit、不 push。
- 不提交 `.ua/`、`target/`、`dist/` 或本地密钥。

## 项目入口

- Java 17、Spring Boot 3、Maven 多模块。模块清单见根目录 `pom.xml`。
- 核心后端：`admin` 账号、`engine` 订单库存、`pay-service` 支付退款、`distribution` 放票、`merchant-admin` 商户管理、`search` 搜索、`settlement` 结算、`gateway` 网关。
- 前端：`client` 用户端，`admin-dashboard` 管理端。
- 数据库：`sql/`；初始化顺序见 `README.md`；只读检查脚本为 `tools/verify_livestart_database.ps1`。

## 关键约束

- 仅保留手机号验证码登录，不恢复密码登录或注册接口。
- 网关先移除客户端身份头，再注入 `userId`、`username`、`realName`、`userType`。
- `userType=3` 是场地管理员，只能操作所属场馆数据；`userType=4` 是超级管理员。
- 场馆、艺人、风格写操作仅限超级管理员。权限同时在网关和服务层校验。
- 内部调用使用 `X-Livestart-Internal-Token`；生产环境禁止默认值 `change-me`。
- 默认支付库为 `live_start_pay`。仅 `pay-sharding` Profile 使用支付分片。
- 用户、订单、座位使用 ShardingSphere。路由变更必须同步迁移脚本。
- 数据库库存是真实来源，Redis 是缓存。缓存同步失败时删除缓存，禁止保留错误库存。
- 超时关单和退款库存回补必须幂等，并保留可重试补偿任务。
- 表结构变更写入 `sql/`，不得依赖业务代码临时建表。

## 操作日志

- 使用 `mzt-biz-log` 和 `@LogRecord`，写入 `live_start.t_operation_log`。
- 日志包含 `type`、`subType`、`bizNo`、操作人、成功或失败状态。
- 操作人来自网关注入的请求头；内部任务使用 `system`。
- 日志切面在业务事务结束后执行；日志落库使用独立事务。
- 更新、删除记录 `originalData`；创建、更新记录 `modifiedData`。
- 批量导入必须通过 Spring 代理逐条调用，保证每行事务和 `@LogRecord` 生效。

## 验证命令

```powershell
mvn test -DskipTests=false
mvn -pl services/merchant-admin -am clean test -DskipTests=false
npm --prefix admin-dashboard run build
npm --prefix client run build
.\tools\verify_livestart_database.ps1 -DatabasePassword '<本地密码>' -PayMode Auto
git diff --check
```

## Commit

- 格式：`<type>(module): <简短中文描述>`；无模块时省略括号。
- 类型沿用 README：`feat`、`fix`、`optimize`、`perf`、`docs`、`style`、`config`、`refactor`、`chore`。
- 保持原子提交。提交前汇报改动和验证结果，等待用户确认。
