# LiveStart 本地工具

这个目录只保留可复用的本地联调和压测辅助脚本。大批量 CSV、JMeter HTML 报告、一次性失败日志不建议提交到仓库。

## 生成压测用户

生成已注册用户、常用观演人和 JMeter CSV：

```powershell
.\tools\seed_registered_users.ps1 -UserCount 1000 -StartIndex 1 -ContinueOnUserError
```

默认输出：

- `jmeter\users_bulk_registered_1000.csv`
- `jmeter\users_scenario3_registered_1000.csv`

如果手机号已被占用，可以调整 `-StartIndex` 或 `-PhonePrefix`。

## 最小下单压测

运行最小下单链路：

```powershell
.\tools\run_jmeter_order_minimal.ps1
```

可通过 `-JmxPath` 复用同一个 runner 指向其他下单场景 JMX，例如超时取消：

```powershell
.\tools\run_jmeter_order_minimal.ps1 `
  -JmxPath .\jmeter\livestart_order_timeout_cancel_fixed.jmx `
  -Scenario3UsersCsvPath .\jmeter\users_order_smoke.csv `
  -ResultFile .\jmeter\results-order-timeout-cancel.jtl `
  -ReportDir .\jmeter\html_report_order_timeout_cancel
```

## 秒杀 MQ 压测

运行 MQ 秒杀下单链路：

```powershell
.\tools\run_jmeter_seckill_mq.ps1
```

## RocketMQ 本地启动

如果希望 `engine`、`distribution`、`settlement` 走真实 RocketMQ 链路，可以先启动本地 RocketMQ：

```powershell
.\tools\start_rocketmq_dev.ps1
```

脚本默认假设 RocketMQ 位于：

```text
F:\Tool\rocketmq-all-5.3.2-source-release\distribution\target\rocketmq-5.3.2\rocketmq-5.3.2
```

本地 broker 配置由 `rocketmq-dev-broker.conf` 提供。

## 提交建议

建议提交：

- `tools\README.md`
- `tools\seed_registered_users.ps1`
- `tools\run_jmeter_order_minimal.ps1`
- `tools\run_jmeter_seckill_mq.ps1`
- `tools\start_rocketmq_dev.ps1`
- `tools\rocketmq-dev-broker.conf`

按需提交：

- `tools\preflight_mq_stock_check.ps1`

不建议提交：

- 大批量生成的 `jmeter\users_*_1000.csv`
- 大批量生成的 `jmeter\users_*_5000.csv`
- JMeter HTML 报告目录
- 一次性失败日志
