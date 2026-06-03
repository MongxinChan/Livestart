<template>
  <div>
    <a-page-header title="数据看板" sub-title="Livestart 运营概览" :ghost="false" style="margin-bottom: 24px" />

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="12" :lg="6" v-for="stat in stats" :key="stat.title">
        <a-card class="stat-card" :bordered="false">
          <a-statistic
            :title="stat.title"
            :value="stat.value"
            :prefix="h(stat.icon)"
            :suffix="stat.suffix"
            :value-style="{ color: stat.color, fontWeight: 700 }"
          />
          <div style="margin-top: 8px; font-size: 12px; color: #8c8c8c">
            {{ stat.desc }}
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 快捷入口 -->
    <a-card title="快捷操作" :bordered="false" style="margin-top: 24px">
      <a-row :gutter="16">
        <a-col :span="6" v-for="shortcut in shortcuts" :key="shortcut.label">
          <a-button
            type="dashed"
            block
            size="large"
            style="height: 80px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px"
            @click="$router.push(shortcut.path)"
          >
            <component :is="shortcut.icon" style="font-size: 24px" />
            <span>{{ shortcut.label }}</span>
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 系统信息 -->
    <a-row :gutter="16" style="margin-top: 24px">
      <a-col :span="12">
        <a-card title="系统架构" :bordered="false">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="网关">Spring Cloud Gateway (8888)</a-descriptions-item>
            <a-descriptions-item label="购票引擎">engine-service (8004)</a-descriptions-item>
            <a-descriptions-item label="商户后台">merchant-admin (8003)</a-descriptions-item>
            <a-descriptions-item label="用户中心">admin-service (8002)</a-descriptions-item>
            <a-descriptions-item label="搜索服务">search-service (8006)</a-descriptions-item>
            <a-descriptions-item label="结算服务">settlement-service (8007)</a-descriptions-item>
            <a-descriptions-item label="消息队列">RocketMQ (延时关单 + 异步出票)</a-descriptions-item>
            <a-descriptions-item label="分库分表">ShardingSphere 16 分表</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="技术亮点" :bordered="false">
          <a-timeline>
            <a-timeline-item color="blue">动态 Path Token 防刷 + JVM L1 售罄拦截</a-timeline-item>
            <a-timeline-item color="green">Redis ZSet 热搜排行 + Redisson 分布式锁</a-timeline-item>
            <a-timeline-item color="orange">RocketMQ 延时消息 15 分钟自动关单</a-timeline-item>
            <a-timeline-item color="purple">@NoMQDuplicateConsume 幂等消费</a-timeline-item>
            <a-timeline-item color="red">OpenFeign 微服务间远程调用</a-timeline-item>
            <a-timeline-item color="cyan">16 分表跨表聚合结算</a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { h } from 'vue'
import {
  VideoCameraOutlined,
  ShoppingCartOutlined,
  DollarOutlined,
  UserOutlined,
  PlusOutlined,
  UnorderedListOutlined,
  BarChartOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'

const stats = [
  { title: '演出总数', value: 12, icon: VideoCameraOutlined, color: '#1677ff', suffix: '场', desc: '含在售 + 预售' },
  { title: '累计订单', value: 3842, icon: ShoppingCartOutlined, color: '#52c41a', suffix: '笔', desc: '含全部状态' },
  { title: '总销售额', value: 2856000, icon: DollarOutlined, color: '#fa8c16', suffix: '元', desc: '已支付订单' },
  { title: '注册用户', value: 1568, icon: UserOutlined, color: '#722ed1', suffix: '人', desc: '平台总用户数' },
]

const shortcuts = [
  { label: '创建演出', path: '/event', icon: PlusOutlined },
  { label: '订单管理', path: '/order', icon: UnorderedListOutlined },
  { label: '结算报表', path: '/settlement', icon: BarChartOutlined },
  { label: '用户管理', path: '/user', icon: TeamOutlined },
]
</script>
