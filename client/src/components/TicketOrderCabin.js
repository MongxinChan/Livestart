import { ref, onMounted } from 'vue';
import request, { state } from '../utils/request.js';

export default {
  name: 'TicketOrderCabin',
  props: {
    selectedEvent: {
      type: Object,
      default: null
    }
  },
  emits: ['back-to-square'],
  template: `
    <div class="order-cabin-view" style="display: grid; grid-template-columns: 1fr; gap: 30px;">
      
      <!-- 抢票控制舱（在选定演出时显示） -->
      <div v-if="selectedEvent" class="glass-panel" style="padding: 30px; border-radius: 20px;">
        <!-- 返回头部 -->
        <div @click="$emit('back-to-square')" class="flex-between" style="cursor: pointer; color: var(--color-accent); font-weight: 600; margin-bottom: 25px; width: fit-content; gap: 6px;">
          <ion-icon name="arrow-back-outline"></ion-icon>
          返回演出广场
        </div>

        <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 40px;">
          <!-- 演出海报及主要信息 -->
          <div style="display: flex; flex-direction: column; gap: 18px;">
            <div style="width: 100%; height: 280px; border-radius: 12px; overflow: hidden; border: 1px solid var(--border-color); box-shadow: var(--neon-glow);">
              <img :src="selectedEvent.cover" style="width:100%; height:100%; object-fit: cover;" />
            </div>
            <h3 style="font-size: 1.2rem; font-weight: 700; line-height: 1.4;">{{ selectedEvent.title }}</h3>
            <div style="font-size: 0.85rem; color: var(--text-secondary); display: flex; flex-direction: column; gap: 6px;">
              <span class="center-all" style="justify-content: flex-start; gap: 6px;"><ion-icon name="calendar-outline"></ion-icon> {{ selectedEvent.date }}</span>
              <span class="center-all" style="justify-content: flex-start; gap: 6px;"><ion-icon name="location-outline"></ion-icon> {{ selectedEvent.venue }}</span>
            </div>
          </div>

          <!-- 抢票规格与操作舱 -->
          <div style="display: flex; flex-direction: column; justify-content: space-between;">
            <div>
              <h4 style="font-size: 1rem; font-weight: 600; margin-bottom: 15px; display: flex; align-items: center; gap: 8px;">
                <ion-icon name="ticket-outline" style="color: var(--color-accent);"></ion-icon> 选择票档规格
              </h4>
              
              <!-- 规格选择列表 -->
              <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 25px;">
                <div 
                  v-for="sku in selectedEvent.skus" 
                  :key="sku.id"
                  @click="activeSku = sku"
                  :class="['glow-card', { 'active-sku': activeSku && activeSku.id === sku.id }]"
                  :style="{
                    cursor: 'pointer',
                    padding: '12px 20px',
                    borderRadius: '12px',
                    border: activeSku && activeSku.id === sku.id ? '2px solid var(--color-accent)' : '1px solid var(--glass-border)',
                    background: activeSku && activeSku.id === sku.id ? 'rgba(var(--color-accent-rgb), 0.08)' : 'rgba(255,255,255,0.01)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between'
                  }"
                >
                  <div>
                    <span style="font-weight: 600; font-size: 0.9rem;">{{ sku.name }}</span>
                    <span v-if="sku.stock <= 0" class="chip" style="background: rgba(255,0,0,0.15); color: #ff3b30; margin-left: 10px; font-size: 0.65rem;">L1 标记已售罄</span>
                    <span v-else class="chip" style="background: rgba(var(--color-success-rgb), 0.1); color: var(--color-success); margin-left: 10px; font-size: 0.65rem;">余 {{ sku.stock }} 张</span>
                  </div>
                  <div style="font-weight: 800; color: var(--color-accent); font-size: 1.1rem;">
                    ¥ {{ sku.price }}
                  </div>
                </div>
              </div>

              <!-- 购买数量 -->
              <div class="flex-between" style="margin-bottom: 30px;">
                <span style="font-size: 0.9rem; font-weight: 600;">购买数量：</span>
                <div class="center-all" style="gap: 15px;">
                  <button @click="ticketCount = Math.max(1, ticketCount - 1)" class="btn-secondary" style="width: 32px; height: 32px; border-radius: 50%;">-</button>
                  <span style="font-weight: 700; font-size: 1.1rem;">{{ ticketCount }}</span>
                  <button @click="ticketCount = Math.min(6, ticketCount + 1)" class="btn-secondary" style="width: 32px; height: 32px; border-radius: 50%;">+</button>
                  <span style="font-size: 0.75rem; color: var(--text-secondary); opacity: 0.8;">(每人限购 6 张)</span>
                </div>
              </div>
            </div>

            <!-- 抢票控制板 (核心动画交互演示) -->
            <div class="order-action-panel glass-panel" style="padding: 20px; border-radius: 14px; border: 1px dashed var(--border-color); background: rgba(0,0,0,0.1);">
              
              <!-- 1. 默认可操作状态 -->
              <div v-if="grabStatus === 'idle'">
                <div class="flex-between" style="margin-bottom: 15px;">
                  <span style="font-size: 0.85rem; color: var(--text-secondary);">合计总额:</span>
                  <span style="font-size: 1.5rem; font-weight: 800; color: var(--color-accent);">¥ {{ (activeSku ? activeSku.price : 0) * ticketCount }}</span>
                </div>
                <button 
                  @click="triggerGrab" 
                  class="btn-primary" 
                  style="width: 100%; height: 48px; font-size: 1rem; border-radius: 12px;"
                  :disabled="!activeSku || activeSku.stock <= 0"
                >
                  <ion-icon name="flash-outline"></ion-icon>
                  {{ activeSku && activeSku.stock <= 0 ? '该规格已被 L1 本地极速拦截' : '立即开始高并发极速抢票' }}
                </button>
              </div>

              <!-- 2. 安全 Token 计算拉取动画 -->
              <div v-else-if="grabStatus === 'fetching_token'" class="center-all" style="flex-direction: column; gap: 15px; padding: 10px 0;">
                <div class="skeleton-spinner" style="border-top-color: var(--color-accent);"></div>
                <div style="text-align: center;">
                  <h5 style="font-size: 0.9rem; font-weight: 600; color: var(--color-accent); margin-bottom: 5px;">
                    🛡️ 安全网关检测防护中
                  </h5>
                  <p style="font-size: 0.75rem; color: var(--text-secondary); line-height: 1.4;">
                    正在校验 UserId / SkuId 频次，向后端拉取防重放 Dynamic Path Token...
                  </p>
                </div>
              </div>

              <!-- 3. Token 获取成功，下单并发削峰排队动画 -->
              <div v-else-if="grabStatus === 'grabbing'" class="center-all" style="flex-direction: column; gap: 15px; padding: 10px 0;">
                <div class="progress-bar-wrap" style="width: 100%; height: 8px; background: rgba(255,255,255,0.05); border-radius: 10px; overflow: hidden; border: 1px solid var(--border-color);">
                  <div class="progress-fill" :style="{ width: grabProgress + '%', height: '100%', background: 'var(--logo-gradient)', transition: 'width 0.4s ease' }"></div>
                </div>
                <div style="text-align: center; width: 100%;">
                  <div class="flex-between" style="margin-bottom: 6px;">
                    <span style="font-size: 0.8rem; font-weight: 600; color: var(--color-success); display: flex; align-items: center; gap: 4px;">
                      <ion-icon name="key-outline"></ion-icon> Token 校验成功
                    </span>
                    <span style="font-size: 0.7rem; color: var(--text-secondary); font-family: monospace;">{{ pathToken.substring(0,18) }}...</span>
                  </div>
                  <h5 style="font-size: 0.9rem; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; display: flex; align-items: center; justify-content: center; gap: 6px;">
                    <ion-icon name="pulse-outline" style="animation: heartbeat 1s infinite;"></ion-icon> RocketMQ 异步排队落库中 ({{ grabProgress }}%)
                  </h5>
                  <p style="font-size: 0.75rem; color: var(--text-secondary); line-height: 1.4;">
                    大流量削峰中，本地正在进行 Redis 扣减限额判定与本地事务隔离排队...
                  </p>
                </div>
              </div>

              <!-- 4. 成功出单 -->
              <div v-else-if="grabStatus === 'success'" class="center-all" style="flex-direction: column; gap: 12px; text-align: center;">
                <ion-icon name="checkmark-circle-outline" style="font-size: 3rem; color: var(--color-success);"></ion-icon>
                <div>
                  <h5 style="font-size: 1rem; font-weight: 700; color: var(--color-success); margin-bottom: 4px;">抢票成功！排队已落库</h5>
                  <p style="font-size: 0.75rem; color: var(--text-secondary);">
                    您的订单号：<span style="font-family: monospace; color: var(--text-primary); font-weight: 600;">{{ createdOrderNo }}</span>
                  </p>
                  <p style="font-size: 0.75rem; color: var(--text-secondary); margin-top: 4px;">
                    请在 15 分钟内前往订单中心完成模拟支付，超时库存将自动释放归还！
                  </p>
                </div>
                <div class="center-all" style="gap: 10px; width: 100%; margin-top: 10px;">
                  <button @click="resetGrab" class="btn-secondary" style="flex: 1; height: 34px; font-size: 0.8rem; border-radius: 8px;">再抢一单</button>
                  <button @click="viewOrders" class="btn-primary" style="flex: 1; height: 34px; font-size: 0.8rem; border-radius: 8px;">查看我的订单</button>
                </div>
              </div>

              <!-- 5. 失败拦截 (如售罄等) -->
              <div v-else-if="grabStatus === 'failed'" class="center-all" style="flex-direction: column; gap: 12px; text-align: center;">
                <ion-icon name="close-circle-outline" style="font-size: 3rem; color: var(--color-accent);"></ion-icon>
                <div>
                  <h5 style="font-size: 1rem; font-weight: 700; color: var(--color-accent); margin-bottom: 4px;">抢票拦截触发！</h5>
                  <p style="font-size: 0.78rem; color: #ff3b30; font-weight: 600; line-height: 1.4;">
                    {{ grabError }}
                  </p>
                </div>
                <button @click="resetGrab" class="btn-primary" style="width: 100%; height: 36px; font-size: 0.8rem; border-radius: 8px; margin-top: 5px;">
                  重新选择规格
                </button>
              </div>

            </div>

          </div>
        </div>
      </div>

      <!-- 订单中心板块（全屏/单独展示区域） -->
      <div class="glass-panel" style="padding: 30px; border-radius: 20px;">
        <h3 style="font-size: 1.3rem; font-weight: 800; margin-bottom: 25px; display: flex; align-items: center; gap: 10px;">
          <ion-icon name="receipt-outline" style="color: var(--color-accent);"></ion-icon> 我的购票订单中心
        </h3>

        <!-- 订单不存在列表 -->
        <div v-if="orders.length === 0" class="center-all" style="flex-direction: column; padding: 60px 0; gap: 15px; border: 1px dashed var(--glass-border); border-radius: 12px;">
          <ion-icon name="cube-outline" style="font-size: 3.5rem; color: var(--text-secondary); opacity: 0.4;"></ion-icon>
          <span style="color: var(--text-secondary); font-size: 0.9rem;">您目前还没有购票订单哦，快去演出广场挑选抢购吧！</span>
        </div>

        <!-- 订单列表网格 -->
        <div v-else style="display: flex; flex-direction: column; gap: 16px;">
          <div 
            v-for="order in orders" 
            :key="order.orderNo"
            class="order-item-card glow-card glass-panel"
            style="padding: 20px; border-radius: 14px; display: flex; flex-direction: column; gap: 15px;"
          >
            <!-- 头部状态栏 -->
            <div class="flex-between" style="border-bottom: 1px solid var(--glass-border); padding-bottom: 12px;">
              <span style="font-size: 0.8rem; color: var(--text-secondary);">
                流水单号: <span style="font-family: monospace; color: var(--text-primary); font-weight: 500;">{{ order.orderNo }}</span>
              </span>
              <span class="chip" :style="{
                background: order.status === 1 ? 'rgba(255, 183, 3, 0.15)' : 
                            order.status === 2 ? 'rgba(0, 255, 204, 0.15)' : 'rgba(255,255,255,0.05)',
                color: order.status === 1 ? '#ffb703' : 
                       order.status === 2 ? 'var(--color-success)' : 'var(--text-secondary)'
              }">
                {{ order.statusDesc }}
              </span>
            </div>

            <!-- 中部信息 -->
            <div style="display: grid; grid-template-columns: 3fr 1fr 1fr; align-items: center; gap: 20px;">
              <div>
                <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 6px;">{{ order.title }}</h4>
                <p style="font-size: 0.8rem; color: var(--text-secondary);">
                  席位规格：<span style="color: var(--text-primary); font-weight: 500;">{{ order.skuName }} (¥ {{ order.price }} × {{ order.count }}张)</span>
                </p>
              </div>
              <div style="text-align: right;">
                <span style="font-size: 0.75rem; color: var(--text-secondary);">订单总额:</span>
                <p style="font-size: 1.25rem; font-weight: 800; color: var(--color-accent);">¥ {{ order.totalAmount }}</p>
              </div>
              <div style="text-align: right; font-size: 0.8rem; color: var(--text-secondary);">
                下单时间:<br/>{{ order.createTime }}
              </div>
            </div>

            <!-- 核销码展示 (已支付状态下) -->
            <div v-if="order.status === 2 && order.checkCode" class="glass-panel" style="padding: 12px; border-radius: 10px; background: rgba(0, 255, 204, 0.03); border: 1px dashed rgba(0, 255, 204, 0.25); display: flex; align-items: center; gap: 15px;">
              <ion-icon name="qr-code-outline" style="font-size: 2.2rem; color: var(--color-success);"></ion-icon>
              <div>
                <span style="font-size: 0.75rem; color: var(--text-secondary);">入场核销电子票码:</span>
                <p style="font-family: monospace; font-size: 1.05rem; font-weight: 700; color: var(--color-success);">{{ order.checkCode }}</p>
              </div>
              <span class="chip" style="margin-left: auto; background: rgba(0, 255, 204, 0.1); color: var(--color-success); font-size: 0.7rem;">出票成功/待入场核销</span>
            </div>

            <!-- 底部按钮区 -->
            <div class="flex-between" style="border-top: 1px solid var(--glass-border); padding-top: 12px; margin-top: 5px;">
              <span style="font-size: 0.75rem; color: var(--text-secondary); opacity: 0.8;">
                分布式锁: @NoDuplicateSubmit | 消息幂等: @NoMQDuplicateConsume 强效护航
              </span>
              
              <div class="center-all" style="gap: 10px;">
                <!-- 待支付订单 ➔ 模拟支付回调 / 取消订单 -->
                <template v-if="order.status === 1">
                  <button 
                    @click="cancelOrder(order.orderNo)" 
                    class="btn-secondary" 
                    style="height: 32px; font-size: 0.8rem; padding: 0 14px; border-radius: 8px;"
                  >
                    取消订单
                  </button>
                  <button 
                    @click="payOrder(order.orderNo)" 
                    class="btn-primary" 
                    style="height: 32px; font-size: 0.8rem; padding: 0 14px; border-radius: 8px;"
                  >
                    <ion-icon name="wallet-outline"></ion-icon>
                    模拟支付回调
                  </button>
                </template>

                <!-- 已出票订单 ➔ 退票申请 -->
                <template v-if="order.status === 2">
                  <button 
                    @click="refundOrder(order.orderNo)" 
                    class="btn-secondary" 
                    style="height: 32px; font-size: 0.8rem; padding: 0 14px; border-radius: 8px; border-color: rgba(255,0,0,0.15); color: #ff3b30;"
                  >
                    <ion-icon name="arrow-undo-outline"></ion-icon>
                    退票申请 (归还库存)
                  </button>
                </template>
              </div>
            </div>

          </div>
        </div>
      </div>

    </div>
  `,
  setup(props, { emit }) {
    const activeSku = ref(null);
    const ticketCount = ref(1);
    const orders = ref([]);

    // 抢票动态状态
    const grabStatus = ref('idle'); // idle, fetching_token, grabbing, success, failed
    const grabProgress = ref(0);
    const pathToken = ref('');
    const createdOrderNo = ref('');
    const grabError = ref('');

    // 加载订单列表
    const fetchOrders = async () => {
      try {
        const data = await request('/api/engine/order/page');
        orders.value = data.records;
      } catch (err) {
        console.error('拉取订单列表失败', err);
      }
    };

    // 一键触发高并发抢票全流程 (Token 防刷 ➔ MQ 削峰排队 ➔ 落库)
    const triggerGrab = async () => {
      if (!activeSku.value) return;
      grabStatus.value = 'fetching_token';
      
      try {
        // 第一步：拉取防刷 Token
        pathToken.value = await request('/api/engine/order/token?skuId=' + activeSku.value.id);
        
        // 延时 800ms，让用户看清 Token 防护动作
        setTimeout(async () => {
          grabStatus.value = 'grabbing';
          grabProgress.value = 10;
          
          // 模拟消息队列排队进度条 (高保真毕设演示)
          const interval = setInterval(() => {
            if (grabProgress.value < 90) {
              grabProgress.value += Math.floor(10 + Math.random()*20);
            }
          }, 300);

          try {
            // 第二步：带加密 Token 路径发起下单
            const orderNo = await request('/api/engine/order/create/' + pathToken.value, {
              method: 'POST',
              body: JSON.stringify({
                skuId: activeSku.value.id,
                count: ticketCount.value,
                visitorIds: Array.from({ length: ticketCount.value }, (_, i) => 1000 + i) // 模拟观演人
              })
            });

            clearInterval(interval);
            grabProgress.value = 100;
            
            setTimeout(() => {
              createdOrderNo.value = orderNo;
              grabStatus.value = 'success';
              fetchOrders();
              // 自动刷新规格的库存，确保 L1 拦截状态能反映在前端上
              if (props.selectedEvent) {
                const sku = props.selectedEvent.skus.find(s=>s.id === activeSku.value.id);
                if (sku) sku.stock = Math.max(0, sku.stock - ticketCount.value);
              }
            }, 400);

          } catch (err) {
            clearInterval(interval);
            grabError.value = err.message;
            grabStatus.value = 'failed';
          }
        }, 800);

      } catch (err) {
        grabError.value = err.message;
        grabStatus.value = 'failed';
      }
    };

    const resetGrab = () => {
      grabStatus.value = 'idle';
      grabProgress.value = 0;
      pathToken.value = '';
      createdOrderNo.value = '';
      grabError.value = '';
    };

    const viewOrders = () => {
      resetGrab();
      // 平滑滚动到订单视图
      window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    };

    // 订单控制：模拟支付回调
    const payOrder = async (orderNo) => {
      try {
        await request('/api/engine/order/pay-callback', {
          method: 'POST',
          body: JSON.stringify({ orderNo, tradeNo: 'TRADE-' + Date.now() })
        });
        fetchOrders();
      } catch (err) {
        alert('支付失败：' + err.message);
      }
    };

    // 订单控制：取消订单并释放库存
    const cancelOrder = async (orderNo) => {
      try {
        await request('/api/engine/order/cancel', {
          method: 'POST',
          body: JSON.stringify({ orderNo })
        });
        fetchOrders();
        // 刷新门票库存显示
        alert("订单已成功取消，分布式事务已安全将 Redis/DB 缓存库存释放归还！");
      } catch (err) {
        alert('取消失败：' + err.message);
      }
    };

    // 订单控制：退票申请
    const refundOrder = async (orderNo) => {
      if (!confirm("确定要申请退票吗？资金与库存都将瞬间回流。")) return;
      try {
        await request('/api/engine/order/refund', {
          method: 'POST',
          body: JSON.stringify({ orderNo })
        });
        fetchOrders();
        alert("退票成功！演出库存已在本地 JVM Map 与 Redis 实例中双向清除售罄标记并完整归还。");
      } catch (err) {
        alert('退票失败：' + err.message);
      }
    };

    onMounted(() => {
      fetchOrders();
      if (props.selectedEvent && props.selectedEvent.skus.length > 0) {
        activeSku.value = props.selectedEvent.skus[0];
      }
    });

    return {
      activeSku,
      ticketCount,
      orders,
      grabStatus,
      grabProgress,
      pathToken,
      createdOrderNo,
      grabError,
      triggerGrab,
      resetGrab,
      viewOrders,
      payOrder,
      cancelOrder,
      refundOrder
    };
  }
};
