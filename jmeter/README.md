# ⚡ LiveStart 全栈微服务高并发压测指南

本目录包含针对 **LiveStart 售票微服务系统** 的 JMeter 性能高压测试脚本，旨在评估系统在极端高并发（如秒杀抢票、集中登录）场景下的吞吐量、响应延时以及并发扣减库存的锁冲突情况。

---

## 📂 文件清单说明
- [livestart_stress_test.jmx](./livestart_stress_test.jmx): JMeter XML 压测脚本模板，支持直接导入 JMeter GUI 进行配置修改。
- [users_scenario3.csv](./users_scenario3.csv): 场景三并发用户参数化文件，提供压测所需的手机号、用户ID、用户名、观演人ID与订票 SKU ID 的对应关系。

---

## 🎯 覆盖的压测场景

### 场景一：C端验证码登录与自动注册 (高并发写)
- **请求接口**：`POST /api/live-start/admin/v1/user/login/code`，请求体为 JSON：`{"phone":"...","code":"..."}`
- **压测意图**：模拟大批用户同时输入验证码登录，如果是新用户则会在后台自动触发**隐式自动注册**（插入数据库 `t_user` 与 `t_user_profile` 表），检验 Redis 校验及主从表入库瓶颈。
- **验证码规则**：压测变量 `${SMS_CODE}` 默认值为 `888888`。本地压测前需将 admin 服务环境变量 `LIVESTART_SMS_MOCK_CODE` 设为同一值；生产环境会忽略固定验证码配置。

### 场景二：C端演出发现列表查询 (高并发读)
- **请求接口**：`GET /api/engine/event/list`
- **压测意图**：高并发聚合读场景。由于该接口涉及通过 OpenFeign 远程调用 `merchant-admin` 获取演出及票档列表，并涉及局部缓存装配场馆名，可验证系统的**远程服务 RPC 调用及缓存聚合吞吐极限**。

### 场景三：C端用户全生命周期抢票混合全链路 (多阶段事务 + 身份穿透)
这是一个高度还原生产环境中真实用户抢票流程的复杂混合链路测试场景，按步骤顺序模拟如下：
1. **步骤 1 (拉取常用观演人)**: `GET /api/live-start/admin/v1/visitor/list`
   - **压测意图**：模拟用户在抢票前确认观演人。该接口触发后台对常用实名身份证信息的对称解密，是高并发下的 **CPU 密集型解密算力指标** 测试。
2. **步骤 2 (获取 URL Path Token)**: `GET /api/engine/order/token?skuId=${skuId}`
   - **压测意图**：获取限时 5 秒的动态防抢跑 `pathToken`，利用 **JSON Extractor** 动态提取并保存至变量 `${pathToken}`。
3. **步骤 3 (提交订单抢票下单)**: `POST /api/engine/order/create/${pathToken}`
   - **压测意图**：带上 Path Token 提交订票参数（含 `visitorIds`），触发 Redis 库存预扣与 RocketMQ 异步事务订单创建。
4. **步骤 4 (刷新我的订单列表)**: `GET /api/engine/order/page?current=1&size=10`
   - **压测意图**：模拟用户在提交订单后自动轮询/手动刷新订单列表。此接口触发对分库分表（7个物理库）的多库聚合查询，是典型的高并发读测试。

#### 💡 关键设计：身份穿透 Header
- 购票引擎与常用观演人接口依赖 `UserTransmitFilter` 拦截器从 HTTP Header 中获取鉴权身份（`userId`、`username`、`phone`）。
- 压测脚本的 `HTTP Header 鉴权穿透配置` 中自动从 `users_scenario3.csv` 数据源读取并注入这三个 Header，**支持直连微服务压测，完美避免未登录阻碍**。

---

## ⚙️ 压测参数配置与网络拓扑切换

您可以使用 JMeter 打开 [livestart_stress_test.jmx](./livestart_stress_test.jmx) 并在 **“用户定义的变量 (User Defined Variables)”** 中灵活修改如下变量：
- `HOST`: 目标主机地址（默认 `localhost`）。
- `ADMIN_PORT`: 用户微服务的端口，直连模式默认为 `8002`。
- `ENGINE_PORT`: 购票引擎的端口，直连模式默认为 `8004`。

> 💡 **切换为“网关模式”**：如果您开启了统一网关（如网关端口为 `8888` 或 `9001`），只需将 `ADMIN_PORT` 和 `ENGINE_PORT` 同时修改为您的网关端口，脚本中的全部路径都将自动转发路由，极度灵活。

---

## 🚀 性能测试执行指南

为了在进行高并发压力测试时避免 JMeter 本身 GUI 界面渲染造成的内存和 CPU 消耗，**强烈建议使用 CLI 命令行非 GUI 模式运行压测**。

### 步骤一：扩充数据源
秒杀计划默认每个 CSV 用户只使用一次，不需要为每一次请求生成一个用户。字段格式为：
`phone,userId,username,visitorId,skuId`。

建议按测试目的准备数据：吞吐测试准备 100-500 个真实用户；需要验证用户限购、分片路由或用户隔离时，再准备更多真实用户。仓库中的 5000 行 CSV 约 435 KB，文件本身不是主要耗时，真实用户预置才是耗时来源。

预置用户使用：

```powershell
.\tools\seed_registered_users.ps1 -UserCount 5000 -Parallelism 8 -VisitorsPerUser 1 -ContinueOnUserError
```

### 步骤二：CLI 压测执行命令
在压测机器的命令行中（切换到 `stress-test` 目录），运行以下命令：
```bash
# 启动非 GUI 命令行压测，并将结果保存至 report 报告目录
jmeter -n -t livestart_stress_test.jmx -l jmeter_result.jtl -e -o ./html_report
```

通过 `tools\run_jmeter_seckill_mq.ps1` 启动时，会关闭结果树和正常响应体落盘，避免监听器占用内存；失败样本仍保留响应体，便于定位接口错误。

### 步骤三：查看分析 HTML 报告
压测完成后，您可直接用浏览器打开 `./html_report/index.html` 网页，分析包括：
- **APDEX (应用性能指数)**
- **TPS (每秒事务吞吐量)**
- **响应时间分布图 (Response Times Over Time)**
- **错误率分布 (Error Kinds)**
