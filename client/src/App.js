import { ref, computed, watch } from 'vue';
import EventSquare from './components/EventSquare.js';
import TicketOrderCabin from './components/TicketOrderCabin.js';
import MerchantSettlement from './components/MerchantSettlement.js';
import { state as apiState } from './utils/request.js';

export default {
  name: 'App',
  components: {
    EventSquare,
    TicketOrderCabin,
    MerchantSettlement
  },
  template: `
    <div :class="['app-root-container', 'theme-' + activeTheme]" style="min-height: 100vh; background-color: var(--bg-primary); color: var(--text-primary);">
      
      <!-- 1. 顶部高保真导航栏 -->
      <header class="main-header">
        <!-- Logo 区域 -->
        <div class="logo-section" @click="navigateTo('square')">
          <ion-icon name="pulse-outline" class="logo-icon"></ion-icon>
          <span class="logo-title text-gradient">LIVESTART</span>
        </div>

        <!-- 中间主视图导航链接 -->
        <nav class="nav-links">
          <div 
            @click="navigateTo('square')" 
            :class="['nav-item', { active: activeView === 'square' || activeView === 'cabin' }]"
          >
            <ion-icon name="ticket-outline"></ion-icon>
            演出发现
          </div>
          <div 
            @click="navigateTo('orders')" 
            :class="['nav-item', { active: activeView === 'orders' }]"
          >
            <ion-icon name="receipt-outline"></ion-icon>
            我的订单中心
          </div>
          <div 
            @click="navigateTo('settlement')" 
            :class="['nav-item', { active: activeView === 'settlement' }]"
          >
            <ion-icon name="calculator-outline"></ion-icon>
            商户结算看板
          </div>
        </nav>

        <!-- 右侧动作控制组 -->
        <div class="header-actions">
          
          <!-- Mock / 联调网关模式切换 -->
          <div class="mode-toggle">
            <span :class="['mode-indicator', { online: !isMockMode }]"></span>
            <span style="color: var(--text-secondary); font-size: 0.75rem;">
              {{ isMockMode ? '离线 Mock 模式' : '网关联调模式' }}
            </span>
            <input 
              type="checkbox" 
              v-model="isMockMode" 
              class="mode-checkbox" 
              title="切换离线Mock演示与真实Gateway接口通讯"
            />
          </div>

          <!-- 主题切换下拉框 -->
          <div class="theme-selector">
            <button @click="toggleThemeMenu" class="theme-btn">
              <ion-icon name="color-palette-outline"></ion-icon>
              {{ currentThemeName }}
              <ion-icon name="chevron-down-outline" style="font-size: 0.7rem;"></ion-icon>
            </button>
            
            <div v-if="showThemeMenu" class="theme-dropdown glass-panel">
              <div 
                v-for="theme in themes" 
                :key="theme.id"
                @click="changeTheme(theme.id)"
                :class="['dropdown-item', { active: activeTheme === theme.id }]"
              >
                {{ theme.name }}
                <ion-icon v-if="activeTheme === theme.id" name="checkmark-outline" style="font-size: 0.8rem;"></ion-icon>
              </div>
            </div>
          </div>

          <!-- 个人登录卡片 -->
          <div class="user-profile">
            <div class="avatar">陈</div>
            <div class="username">陈孟欣 (开发者)</div>
          </div>

        </div>
      </header>

      <!-- 2. 主页面内容区域 -->
      <main class="main-viewport">
        <!-- 演出发现广场 -->
        <div v-if="activeView === 'square'">
          <event-square @select-event="selectEventForCabin"></event-square>
        </div>

        <!-- 抢票下单演示舱 -->
        <div v-else-if="activeView === 'cabin'">
          <ticket-order-cabin 
            :selected-event="selectedEvent"
            @back-to-square="navigateTo('square')"
          ></ticket-order-cabin>
        </div>

        <!-- 纯订单中心面板显示 -->
        <div v-else-if="activeView === 'orders'">
          <ticket-order-cabin 
            :selected-event="null"
            @back-to-square="navigateTo('square')"
          ></ticket-order-cabin>
        </div>

        <!-- 商家核算结算看板 -->
        <div v-else-if="activeView === 'settlement'">
          <merchant-settlement></merchant-settlement>
        </div>
      </main>

    </div>
  `,
  setup() {
    const activeView = ref('square'); // square, cabin, orders, settlement
    const selectedEvent = ref(null);
    const showThemeMenu = ref(false);
    
    // 多主题配置表
    const activeTheme = ref('cyberpunk-dark'); // 默认暗黑赛博风
    const themes = [
      { id: 'cyberpunk-dark', name: '🌌 赛博暗黑' },
      { id: 'minimalist-light', name: '☀️ 高雅极简' },
      { id: 'damai-crimson', name: '🔴 大麦炽红' },
      { id: 'showstart-neon', name: '🟢 秀动荧光' }
    ];

    const currentThemeName = computed(() => {
      const t = themes.find(x => x.id === activeTheme.value);
      return t ? t.name : '主题切换';
    });

    const toggleThemeMenu = () => {
      showThemeMenu.value = !showThemeMenu.value;
    };

    const changeTheme = (themeId) => {
      activeTheme.value = themeId;
      showThemeMenu.value = false;
    };

    const navigateTo = (viewName) => {
      activeView.value = viewName;
      if (viewName === 'square') {
        selectedEvent.value = null;
      }
    };

    const selectEventForCabin = (event) => {
      selectedEvent.value = event;
      activeView.value = 'cabin';
    };

    // 双核驱动 Mock 开关响应式绑定
    const isMockMode = ref(apiState.isMock);
    watch(isMockMode, (newVal) => {
      apiState.isMock = newVal;
      console.log('API 通讯核心已流转为：', newVal ? '离线 Mock 模式' : '真实网关联调模式');
    });

    onMounted(() => {
      // 首屏加载完成后，平滑撤销骨架屏
      const skeleton = document.querySelector('.app-skeleton');
      if (skeleton) {
        setTimeout(() => {
          skeleton.style.opacity = '0';
          skeleton.style.transition = 'opacity 0.4s ease';
          setTimeout(() => {
            skeleton.remove();
          }, 400);
        }, 500);
      }
    });

    return {
      activeView,
      selectedEvent,
      showThemeMenu,
      activeTheme,
      themes,
      currentThemeName,
      isMockMode,
      toggleThemeMenu,
      changeTheme,
      navigateTo,
      selectEventForCabin
    };
  }
};
