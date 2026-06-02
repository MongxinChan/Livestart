import { ref, onMounted, computed } from 'vue';
import request from '../utils/request.js';

export default {
  name: 'EventSquare',
  emits: ['select-event'],
  template: `
    <div class="event-square-view">
      <!-- 搜索及热搜词云区 -->
      <div class="search-hero glass-panel center-all" style="flex-direction: column; padding: 40px; margin-bottom: 30px; text-align: center;">
        <h2 style="font-size: 2.2rem; font-weight: 800; margin-bottom: 12px; letter-spacing: 0.1rem;">
          探索精彩 <span class="text-gradient">LIVE 现场演出</span>
        </h2>
        <p style="color: var(--text-secondary); margin-bottom: 25px; max-width: 600px; font-size: 0.95rem;">
          汇集大麦大型演唱会、秀动小众 Livehouse。搭载高并发 L1 本地安全保护与抢票防刷系统，让抢票丝滑顺畅。
        </p>
        
        <!-- 搜索条 -->
        <div class="search-bar-wrap center-all" style="width: 100%; max-width: 580px; position: relative;">
          <ion-icon name="search-outline" style="position: absolute; left: 16px; font-size: 1.3rem; color: var(--text-secondary);"></ion-icon>
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索明星、乐队、演出、场馆..." 
            @keyup.enter="handleSearch"
            style="width: 100%; height: 50px; border-radius: 12px; padding: 0 50px 0 45px; background: var(--input-bg); border: 1px solid var(--border-color); color: var(--text-primary); font-size: 0.95rem; font-weight: 500;"
          />
          <button @click="handleSearch" class="btn-primary" style="position: absolute; right: 6px; height: 38px; padding: 0 18px; border-radius: 8px; font-size: 0.85rem;">
            搜索
          </button>
        </div>

        <!-- 热度搜索词云 -->
        <div class="hot-search-cloud center-all" style="flex-wrap: wrap; gap: 8px; margin-top: 18px; max-width: 700px;">
          <span style="font-size: 0.8rem; font-weight: 600; color: var(--text-secondary); margin-right: 6px;">实时热搜:</span>
          <div 
            v-for="(hot, index) in hotSearches" 
            :key="index"
            @click="clickHotWord(hot.keyword)"
            class="hot-chip glow-card center-all"
            :style="{
              cursor: 'pointer',
              padding: '4px 12px',
              borderRadius: '20px',
              fontSize: '0.8rem',
              fontWeight: 500,
              background: 'rgba(255, 255, 255, 0.02)',
              border: '1px solid var(--glass-border)'
            }"
          >
            <ion-icon name="flame-outline" style="color: var(--color-accent); font-size: 0.9rem; margin-right: 4px;"></ion-icon>
            {{ hot.keyword }}
            <span style="color: var(--text-secondary); font-size: 0.7rem; margin-left: 4px; opacity: 0.7;">{{ hot.score }}</span>
          </div>
        </div>
      </div>

      <!-- 类别过滤器 -->
      <div class="filter-bar flex-between" style="margin-bottom: 25px;">
        <div class="category-tabs center-all" style="gap: 10px;">
          <button 
            v-for="cat in ['全部', '演唱会', 'Livehouse']"
            :key="cat"
            @click="activeCategory = cat"
            :class="['nav-item', { active: activeCategory === cat }]"
            style="border: 1px solid var(--glass-border); background: var(--bg-card); cursor: pointer;"
          >
            {{ cat }}
          </button>
        </div>
        <div style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 500;">
          已为您挑选出 <span style="color: var(--color-accent); font-weight: bold;">{{ filteredEvents.length }}</span> 场精彩现场
        </div>
      </div>

      <!-- 演出列表网格 -->
      <div v-if="loading" class="events-skeleton-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px;">
        <div v-for="i in 4" :key="i" class="glass-panel" style="height: 380px; padding: 15px; display: flex; flex-direction: column; gap: 15px; border-radius: 16px;">
          <div style="width: 100%; height: 200px; background: rgba(255,255,255,0.03); border-radius: 12px; animation: pulse 1.5s infinite;"></div>
          <div style="width: 80%; height: 20px; background: rgba(255,255,255,0.03); border-radius: 4px; animation: pulse 1.5s infinite;"></div>
          <div style="width: 40%; height: 16px; background: rgba(255,255,255,0.03); border-radius: 4px; animation: pulse 1.5s infinite;"></div>
          <div style="width: 100%; height: 40px; background: rgba(255,255,255,0.03); border-radius: 8px; margin-top: auto; animation: pulse 1.5s infinite;"></div>
        </div>
      </div>

      <div v-else class="events-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px;">
        <div 
          v-for="event in filteredEvents" 
          :key="event.id"
          class="event-card glow-card glass-panel"
          style="padding: 15px; border-radius: 16px; display: flex; flex-direction: column; height: 100%; overflow: hidden;"
        >
          <!-- 封面大图 -->
          <div class="event-cover-wrap" style="width: 100%; height: 180px; border-radius: 10px; overflow: hidden; position: relative;">
            <img 
              :src="event.cover" 
              :alt="event.title" 
              style="width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s ease;"
              @mouseover="e => e.target.style.transform = 'scale(1.08)'"
              @mouseout="e => e.target.style.transform = 'scale(1)'"
            />
            <span class="chip" :style="{
              position: 'absolute',
              top: '10px',
              left: '10px',
              background: event.type === '演唱会' ? 'rgba(255, 45, 85, 0.9)' : 'rgba(0, 255, 204, 0.9)',
              color: event.type === '演唱会' ? '#fff' : '#000',
              backdropFilter: 'blur(4px)'
            }">
              {{ event.type }}
            </span>
          </div>

          <!-- 演出内容 -->
          <div class="event-info" style="display: flex; flex-direction: column; flex: 1; padding: 12px 0 0 0;">
            <h3 style="font-size: 0.95rem; font-weight: 700; line-height: 1.4; height: 42px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 8px;">
              {{ event.title }}
            </h3>
            
            <div style="font-size: 0.8rem; color: var(--text-secondary); display: flex; flex-direction: column; gap: 4px; margin-bottom: 12px;">
              <span class="center-all" style="justify-content: flex-start; gap: 6px;">
                <ion-icon name="calendar-outline" style="font-size: 0.95rem; color: var(--color-accent);"></ion-icon>
                {{ event.date }}
              </span>
              <span class="center-all" style="justify-content: flex-start; gap: 6px;">
                <ion-icon name="location-outline" style="font-size: 0.95rem; color: var(--color-accent);"></ion-icon>
                {{ event.venue }}
              </span>
            </div>

            <!-- 起步价与动作 -->
            <div class="event-footer flex-between" style="margin-top: auto; border-top: 1px solid var(--glass-border); padding-top: 12px;">
              <div>
                <span style="font-size: 0.75rem; color: var(--text-secondary);">票价 ¥ </span>
                <span style="font-size: 1.3rem; font-weight: 800; color: var(--color-accent);">{{ event.minPrice }}</span>
                <span style="font-size: 0.75rem; color: var(--text-secondary);"> 起</span>
              </div>
              <button 
                @click="$emit('select-event', event)" 
                class="btn-primary" 
                style="height: 34px; padding: 0 14px; border-radius: 8px; font-size: 0.8rem;"
              >
                立即抢票
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  setup(props, { emit }) {
    const searchQuery = ref('');
    const activeCategory = ref('全部');
    const loading = ref(false);
    const events = ref([]);
    const hotSearches = ref([]);

    const fetchEvents = async () => {
      loading.value = true;
      try {
        const data = await request('/api/engine/event/list');
        events.value = data;
      } catch (err) {
        console.error('拉取演出失败', err);
      } finally {
        loading.value = false;
      }
    };

    const fetchHotSearches = async () => {
      try {
        const data = await request('/api/search/hot');
        hotSearches.value = data;
      } catch (err) {
        console.error('拉取热搜词云失败', err);
      }
    };

    const handleSearch = async () => {
      if (!searchQuery.value.trim()) {
        fetchEvents();
        return;
      }
      loading.value = true;
      try {
        // 模糊搜索
        const data = await request('/api/search/event?keyword=' + encodeURIComponent(searchQuery.value));
        events.value = data;
        // 写入 Redis ZSet 增加权重
        await request('/api/search/click?keyword=' + encodeURIComponent(searchQuery.value));
        fetchHotSearches();
      } catch (err) {
        console.error('搜索演出失败', err);
      } finally {
        loading.value = false;
      }
    };

    const clickHotWord = (word) => {
      searchQuery.value = word;
      handleSearch();
    };

    const filteredEvents = computed(() => {
      if (activeCategory.value === '全部') return events.value;
      return events.value.filter(e => e.type === activeCategory.value);
    });

    onMounted(() => {
      fetchEvents();
      fetchHotSearches();
    });

    return {
      searchQuery,
      activeCategory,
      loading,
      hotSearches,
      filteredEvents,
      handleSearch,
      clickHotWord
    };
  }
};
