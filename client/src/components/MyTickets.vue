<template>
  <div class="my-tickets-view" style="display: flex; flex-direction: column; gap: 24px">
    <a-card class="glass-panel" :bordered="false">
      <template #title>
        <span style="font-size: 1.15rem; font-weight: 800">
          <FileTextOutlined style="margin-right: 8px; color: var(--ant-color-primary)" />
          我的购票电子票夹
        </span>
      </template>
      <template #extra>
        <a-button type="link" @click="$emit('backToSquare')" style="padding: 0; font-weight: 600">
          返回演出广场
        </a-button>
      </template>

      <a-empty
        v-if="orders.length === 0"
        description="您目前还没有购票订单，去演出广场挑选一场喜欢的演出吧。"
      />

      <div v-else style="display: flex; flex-direction: column; gap: 20px">
        <a-card
          v-for="order in orders"
          :key="order.orderNo"
          size="small"
          :bordered="true"
          style="border-radius: 14px"
        >
          <div
            style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid rgba(var(--ls-accent-rgb), 0.08)"
          >
            <span style="font-size: 12px; color: var(--ls-text-secondary)">
              流水单号: <span style="font-family: monospace; font-weight: 500">{{ order.orderNo }}</span>
            </span>
            <a-tag :color="orderStatusColor(order.status)">{{ order.statusDesc }}</a-tag>
          </div>

          <div class="ticket-card">
            <div class="ticket-left">
              <a-tag color="processing" style="margin-bottom: 8px; font-size: 11px">
                ELECTRONIC TICKET / 电子入场凭证
              </a-tag>
              <h4 style="font-size: 0.9rem; font-weight: 700; margin-bottom: 6px; line-height: 1.4">
                {{ order.title }}
              </h4>
              <div
                style="font-size: 12px; color: var(--ls-text-secondary); display: flex; flex-direction: column; gap: 3px"
              >
                <span>票档：<b>{{ order.skuName }} (¥{{ order.price }} × {{ order.count }}张)</b></span>
                <span>下单：{{ order.createTime }}</span>
              </div>
              <div style="margin-top: 12px; font-size: 11px; color: var(--ls-text-secondary); opacity: 0.85">
                温馨提示：入场时请配合工作人员核销。实名制一票一证，防伪防复制。
              </div>
            </div>

            <div class="ticket-right">
              <div v-if="order.status === 1 && order.isChecked" class="stamp-used">已入场</div>
              <template v-else-if="order.status === 1 && order.checkCode">
                <QrcodeOutlined style="font-size: 36px; color: var(--ant-color-success); margin-bottom: 6px" />
                <span
                  style="font-size: 11px; font-family: monospace; font-weight: 600; color: var(--ant-color-success)"
                >
                  {{ order.checkCode.substring(0, 11) }}
                </span>
              </template>
              <template v-else-if="order.status === 1">
                <SafetyCertificateOutlined
                  style="font-size: 28px; color: var(--ant-color-primary); margin-bottom: 6px"
                />
                <span style="font-size: 11px; color: var(--ant-color-primary); font-weight: 600">
                  已支付，待出票
                </span>
              </template>
              <template v-else-if="order.status === 0">
                <LoadingOutlined style="font-size: 28px; color: var(--ant-color-warning); margin-bottom: 6px" spin />
                <span style="font-size: 11px; color: var(--ant-color-warning); font-weight: 600">等待支付</span>
              </template>
              <template v-else-if="order.status === 2">
                <StopOutlined style="font-size: 28px; color: var(--ls-text-secondary); margin-bottom: 6px" />
                <span style="font-size: 11px; color: var(--ls-text-secondary)">订单已取消</span>
              </template>
              <template v-else-if="order.status === 3">
                <StopOutlined style="font-size: 28px; color: var(--ls-text-secondary); margin-bottom: 6px" />
                <span style="font-size: 11px; color: var(--ls-text-secondary)">已退票</span>
              </template>
              <template v-else>
                <StopOutlined style="font-size: 28px; color: var(--ls-text-secondary); margin-bottom: 6px" />
                <span style="font-size: 11px; color: var(--ls-text-secondary)">状态待确认</span>
              </template>
            </div>
          </div>

          <div
            style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; padding-top: 10px; border-top: 1px solid rgba(var(--ls-accent-rgb), 0.08)"
          >
            <template v-if="order.status === 0">
              <a-button size="small" @click="cancelOrder(order.orderNo)">取消订单</a-button>
              <a-button size="small" type="primary" @click="openCheckoutModal(order)">
                <template #icon><WalletOutlined /></template>
                前往收银台
              </a-button>
            </template>

            <template v-if="order.status === 1 && order.checkCode && !order.isChecked">
              <a-button size="small" danger @click="refundOrder(order.orderNo)">退票申请</a-button>
              <a-button size="small" type="primary" @click="performCheckCode(order)">
                <template #icon><CheckCircleOutlined /></template>
                模拟入场核销
              </a-button>
            </template>
          </div>
        </a-card>
      </div>
    </a-card>

    <a-modal v-model:open="showCheckout" :footer="null" :width="440" centered destroy-on-close>
      <template #title>
        <span>
          <SafetyCertificateOutlined style="color: var(--ant-color-success); margin-right: 8px" />
          Livestart 统一收银结算中心
        </span>
      </template>

      <template v-if="payingOrder">
        <a-card
          size="small"
          :bordered="false"
          style="margin-bottom: 20px; border-radius: 12px; background: rgba(0,0,0,0.06)"
        >
          <h5 style="font-weight: 700; margin-bottom: 4px">{{ payingOrder.title }}</h5>
          <p style="font-size: 12px; color: var(--ls-text-secondary); margin-bottom: 8px">
            {{ payingOrder.skuName }} × {{ payingOrder.count }}张
          </p>
          <div style="font-size: 1.6rem; font-weight: 900; text-align: center; font-family: 'Outfit'">
            ¥ {{ payingOrder.totalAmount }}
          </div>
        </a-card>

        <a-radio-group v-model:value="payMethod" style="width: 100%; margin-bottom: 20px">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-radio-button
                value="wx"
                style="width: 100%; height: 60px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10px"
              >
                <WechatOutlined style="font-size: 22px; color: #09bb07" />
                <span style="font-size: 12px">微信支付</span>
              </a-radio-button>
            </a-col>
            <a-col :span="12">
              <a-radio-button
                value="alipay"
                style="width: 100%; height: 60px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 10px"
              >
                <AlipayCircleOutlined style="font-size: 22px; color: #108ee9" />
                <span style="font-size: 12px">支付宝</span>
              </a-radio-button>
            </a-col>
          </a-row>
        </a-radio-group>

        <div style="text-align: center; margin-bottom: 20px">
          <div
            style="width: 140px; height: 140px; background: #fff; padding: 10px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center"
          >
            <QrcodeOutlined :style="{ fontSize: '100px', color: payMethod === 'wx' ? '#09bb07' : '#108ee9' }" />
          </div>
          <p style="font-size: 12px; color: var(--ls-text-secondary); margin-top: 8px">
            <LoadingOutlined spin /> 等待扫码支付，安全对账连接已建立
          </p>
        </div>

        <a-button type="primary" block size="large" @click="confirmMockPay">
          确认模拟已扫码付款（触发出票回调）
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {
  SafetyCertificateOutlined,
  FileTextOutlined,
  QrcodeOutlined,
  StopOutlined,
  WalletOutlined,
  CheckCircleOutlined,
  WechatOutlined,
  AlipayCircleOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import { useMyTickets } from '@/composables/order/useMyTickets'

defineEmits<{
  backToSquare: []
}>()

const {
  orders,
  showCheckout,
  payingOrder,
  payMethod,
  orderStatusColor,
  openCheckoutModal,
  confirmMockPay,
  cancelOrder,
  refundOrder,
  performCheckCode,
} = useMyTickets()
</script>
