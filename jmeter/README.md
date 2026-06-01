# 🔥 Livestart 压测指南

## 前置条件

1. **安装 JMeter**：下载 [Apache JMeter](https://jmeter.apache.org/download_jmeter.cgi) 并解压
2. **启动基础设施**：MySQL、Redis、RocketMQ 均需正常运行
3. **启动 engine 服务**：确保 `Livestart-engine` 运行在 `localhost:8004`

## 压测前数据准备

在 Redis 中预热票种库存（模拟 XXL-JOB 预热完成后的状态）：

```bash
# 连接 Redis，为测试票种设置 1000 张库存
redis-cli SET "engine:stock:sku:1" 1000
```

确保数据库中存在对应的 `t_ticket_sku` 记录（id=1），且 `remaining_stock >= 1000`。

## 执行压测

### GUI 模式（观察实时图表）

```powershell
# 进入 JMeter 安装目录
jmeter -t d:\02_Workspace\Projects\Livestart\jmeter\livestart_stress_test.jmx
```

### 命令行模式（生成 HTML 报告，推荐）

```powershell
jmeter -n -t d:\02_Workspace\Projects\Livestart\jmeter\livestart_stress_test.jmx -l result.jtl -e -o report/

# -n: 非 GUI 模式
# -l: 结果日志文件
# -e -o: 生成 HTML 报告到 report/ 目录
```

## 压测场景说明

| 场景 | 线程数 | Ramp-Up | 循环 | 说明 |
|------|--------|---------|------|------|
| 抢票洪峰 | 500 | 1s | 1 | 模拟500人同一秒内抢票 |

## 关键指标解读

| 指标 | 含义 | 预期值 |
|------|------|--------|
| **Throughput (TPS)** | 每秒处理的请求数 | > 1000 req/s（异步下单后） |
| **Average RT** | 平均响应时间 | < 50ms（Redis Lua + MQ 投递） |
| **Error %** | 错误率 | 库存售罄后的正常拒绝不算错误 |
| **99th pct RT** | P99 响应时间 | < 200ms |

## 零超卖验证

压测完成后，执行以下 SQL 验证：

```sql
-- 统计总下单票数
SELECT COUNT(*) AS total_tickets FROM t_order_item_0
UNION ALL SELECT COUNT(*) FROM t_order_item_1
-- ... 按分表数量扩展

-- 对比初始库存，确认总票数 <= 初始库存
SELECT remaining_stock FROM t_ticket_sku WHERE id = 1;
```

## 限流验证

将线程数改为 100、循环次数改为 10（即每个用户发 10 次请求），观察：
- 被限流的请求应返回 HTTP 429
- Summary Report 中 Error % 应该 > 80%（大部分被限流）
