import { ref, onMounted } from 'vue';
import request from '../utils/request.js';

export default {
  name: 'MerchantSettlement',
  template: `
    <div class="merchant-settlement-view" style="display: flex; flex-direction: column; gap: 30px;">
      
      <!-- 结算对账控制中心 -->
      <div class="glass-panel" style="padding: 30px; border-radius: 20px;">
        <h3 style="font-size: 1.4rem; font-weight: 800; margin-bottom: 12px; display: flex; align-items: center; gap: 10px;">
          <ion-icon name="calculator-outline" style="color: var(--color-accent);"></ion-icon>
          主办方票房资金核算中心
        </h3>
        <p style="color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 25px; max-width: 800px;">
          对已支付和核销的电子票发起票房结算。系统底层对账中心将**多线程跨节点扫描 ds_order 库下的 16 张物理表 (t_order_item_0 ~ t_order_item_15)**，完成分库分表下的资金合并对账，提取 5% 平台佣金并结算给商户。
        </p>

        <!-- 选择演出触发核算 -->
        <div class="flex-between glow-card" style="padding: 20px; border-radius: 12px; border: 1px solid var(--border-color); background: rgba(0,0,0,0.15); flex-wrap: wrap; gap: 20px;">
          <div class="center-all" style="gap: 15px; flex-wrap: wrap;">
            <span style="font-size: 0.9rem; font-weight: 600;">核算演出场次：</span>
            <select 
              v-model="selectedEventId" 
              style="height: 40px; border-radius: 8px; padding: 0 15px; background: var(--input-bg); border: 1px solid var(--glass-border); color: var(--text-primary); font-weight: 600; min-width: 280px; font-size: 0.85rem;"
            >
              <option value="101">万能青年旅店 Modern Sky LAB</option>
              <option value="102">周杰伦 杭州奥体中心体育场</option>
              <option value="103">重塑雕像的权利 深圳 HOU Live</option>
              <option value="104">陈奕迅 广州大学城体育中心</option>
            </select>
          </div>
          
          <button 
            @click="triggerSettlement" 
            class="btn-primary" 
            style="height: 42px; padding: 0 25px; border-radius: 10px; font-size: 0.9rem;"
            :disabled="isSettling"
          >
            <ion-icon name="analytics-outline"></ion-icon>
            {{ isSettling ? '分表数据多线程核算对账中...' : '开始跨 16 张物理表核算票房' }}
          </button>
        </div>
      </div>

      <!-- 对账物理表扫描动画演示舱 -->
      <div v-if="isSettling" class="glass-panel" style="padding: 30px; border-radius: 20px; border: 1px dashed var(--color-accent); background: rgba(var(--color-accent-rgb), 0.01);">
        <div class="center-all" style="flex-direction: column; gap: 15px; margin-bottom: 25px;">
          <div class="skeleton-spinner" style="border-top-color: var(--color-success); width: 60px; height: 60px;"></div>
          <h4 class="text-gradient" style="font-size: 1.15rem; font-weight: 800; animation: pulse 1s infinite; letter-spacing: 0.05rem;">
            分布式核算对账引擎激活 - 正在穿透 16 张订单物理分表进行多线程核查
          </h4>
          <p style="color: var(--text-secondary); font-size: 0.8rem;">
            对账进度：已成功匹配并对账第 <span style="color: var(--color-success); font-weight: bold;">{{ scanIndex + 1 }}</span> / 16 张物理表
          </p>
        </div>

        <!-- 16张物理表扫描动态栅格 -->
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;">
          <div 
            v-for="(t, i) in 16" 
            :key="i"
            :class="['glass-panel center-all', { 'scan-pending': scanIndex < i, 'scan-scanning': scanIndex === i, 'scan-done': scanIndex > i }]"
            :style="{
              padding: '10px 15px',
              borderRadius: '10px',
              fontSize: '0.78rem',
              fontWeight: '600',
              fontFamily: 'monospace',
              border: '1px solid var(--glass-border)',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
              opacity: scanIndex < i ? 0.35 : 1,
              background: scanIndex === i ? 'rgba(var(--color-accent-rgb), 0.1)' : 
                          scanIndex > i ? 'rgba(0, 255, 204, 0.05)' : 'rgba(255,255,255,0.01)'
            }"
          >
            <div class="flex-between" style="width: 100%;">
              <span>t_order_item_{{ i }}</span>
              <ion-icon 
                :name="scanIndex < i ? 'ellipse-outline' : scanIndex === i ? 'sync-outline' : 'checkmark-circle-outline'"
                :style="{
                  color: scanIndex < i ? 'var(--text-secondary)' : scanIndex === i ? 'var(--color-accent)' : 'var(--color-success)',
                  animation: scanIndex === i ? 'spin 1s linear infinite' : 'none'
                }"
              ></ion-icon>
            </div>
            <span v-if="scanIndex > i" style="font-size: 0.68rem; color: var(--color-success); opacity: 0.9;">对账一致 ✓</span>
            <span v-else-if="scanIndex === i" style="font-size: 0.68rem; color: var(--color-accent);">正在抓取数据...</span>
            <span v-else style="font-size: 0.68rem; color: var(--text-secondary); opacity: 0.5;">待检索</span>
          </div>
        </div>
      </div>

      <!-- 结算单对账成果报表 -->
      <div v-if="settleResult && !isSettling" class="settle-result-panel" style="display: flex; flex-direction: column; gap: 30px;">
        
        <!-- 核心统计大卡片 -->
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px;">
          <!-- 1. 总销售额 -->
          <div class="glass-panel glow-card" style="padding: 24px; border-radius: 16px; border-left: 5px solid var(--color-accent);">
            <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 500;">票房总销售额</span>
            <h3 style="font-size: 2rem; font-weight: 800; color: var(--color-accent); margin-top: 10px; font-family: 'Outfit';">
              ¥ {{ settleResult.totalRevenue.toLocaleString() }}
            </h3>
            <p style="font-size: 0.72rem; color: var(--text-secondary); margin-top: 5px;">包含待支付、已支付全渠道交易流</p>
          </div>

          <!-- 2. 已核销出票 -->
          <div class="glass-panel glow-card" style="padding: 24px; border-radius: 16px; border-left: 5px solid var(--color-success);">
            <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 500;">核算已出电子票</span>
            <h3 style="font-size: 2rem; font-weight: 800; color: var(--color-success); margin-top: 10px; font-family: 'Outfit';">
              {{ settleResult.totalSold.toLocaleString() }} 张
            </h3>
            <p style="font-size: 0.72rem; color: var(--text-secondary); margin-top: 5px;">通过 RocketMQ 顺利落单的电子客票</p>
          </div>

          <!-- 3. 平台佣金 (5%) -->
          <div class="glass-panel glow-card" style="padding: 24px; border-radius: 16px; border-left: 5px solid var(--color-warning);">
            <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 500;">平台服务抽成 (5%)</span>
            <h3 style="font-size: 2rem; font-weight: 800; color: var(--color-warning); margin-top: 10px; font-family: 'Outfit';">
              ¥ {{ settleResult.commission.toLocaleString() }}
            </h3>
            <p style="font-size: 0.72rem; color: var(--text-secondary); margin-top: 5px;">平台流量管理与 API 安全保护费</p>
          </div>

          <!-- 4. 商户应结净额 -->
          <div class="glass-panel glow-card" style="padding: 24px; border-radius: 16px; border-left: 5px solid #a855f7;">
            <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 500;">商户应结算净额</span>
            <h3 style="font-size: 2rem; font-weight: 800; color: #a855f7; margin-top: 10px; font-family: 'Outfit';">
              ¥ {{ settleResult.netAmount.toLocaleString() }}
            </h3>
            <p style="font-size: 0.72rem; color: var(--text-secondary); margin-top: 5px;">扣除平台抽佣后的实结票房收入</p>
          </div>
        </div>

        <!-- 16 张分表的散列穿透细节 -->
        <div class="glass-panel" style="padding: 30px; border-radius: 20px;">
          <h4 style="font-size: 1.1rem; font-weight: 700; margin-bottom: 20px; display: flex; align-items: center; gap: 8px;">
            <ion-icon name="layers-outline" style="color: var(--color-accent);"></ion-icon>
            16 张物理订单分片表 (OrderItem) 散列穿透细节 (Sharding DB Table Penetration)
          </h4>
          
          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px;">
            <div 
              v-for="shard in settleResult.shards" 
              :key="shard.tableName"
              class="glow-card glass-panel"
              style="padding: 15px; border-radius: 12px; border: 1px solid var(--glass-border); display: flex; flex-direction: column; gap: 10px; background: rgba(255,255,255,0.01);"
            >
              <div class="flex-between">
                <span style="font-family: monospace; font-weight: 700; color: var(--text-primary);">{{ shard.tableName }}</span>
                <span class="chip" :style="{
                  fontSize: '0.65rem',
                  fontWeight: 600,
                  background: shard.dbShard === 'ds_order_0' ? 'rgba(32, 227, 178, 0.1)' : 'rgba(192, 38, 211, 0.1)',
                  color: shard.dbShard === 'ds_order_0' ? 'var(--color-success)' : '#c026d3'
                }">
                  {{ shard.dbShard }}
                </span>
              </div>
              <div class="flex-between" style="font-size: 0.8rem; color: var(--text-secondary);">
                <span>处理订单: <b style="color: var(--text-primary);">{{ shard.orders }}</b> 笔</span>
                <span>出票量: <b style="color: var(--text-primary);">{{ shard.tickets }}</b> 张</span>
              </div>
              <div class="flex-between" style="border-top: 1px dashed var(--glass-border); padding-top: 8px; margin-top: 2px;">
                <span style="font-size: 0.75rem; color: var(--text-secondary);">本表核算额:</span>
                <span style="font-weight: 700; color: var(--color-accent); font-size: 0.95rem;">¥ {{ shard.revenue.toLocaleString() }}</span>
              </div>
            </div>
          </div>
        </div>

      </div>

    </div>
  `,
  setup() {
    const selectedEventId = ref('101');
    const isSettling = ref(false);
    const settleResult = ref(null);
    const scanIndex = ref(-1);

    const triggerSettlement = async () => {
      isSettling.value = true;
      settleResult.value = null;
      scanIndex.value = -1;

      // 模拟多线程扫描 16 张分表的动态动画效果 (每次跳动 120ms，极具视觉冲击力)
      const timer = setInterval(() => {
        if (scanIndex.value < 15) {
          scanIndex.value += 1;
        } else {
          clearInterval(timer);
          // 扫描结束后，获取真实的/Mock的结算汇总数据
          fetchSettlementData();
        }
      }, 100);
    };

    const fetchSettlementData = async () => {
      try {
        const data = await request(`/api/settlement/trigger?eventId=${selectedEventId.value}`);
        settleResult.value = data;
      } catch (err) {
        alert('票房结算对账失败: ' + err.message);
      } finally {
        isSettling.value = false;
      }
    };

    return {
      selectedEventId,
      isSettling,
      settleResult,
      scanIndex,
      triggerSettlement
    };
  }
};
