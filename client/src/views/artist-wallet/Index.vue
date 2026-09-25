<template>
  <section class="wallet-page">
    <div class="wallet-header">
      <div>
        <p class="wallet-kicker">Artist Wallet</p>
        <h1>艺人钱包</h1>
        <p class="wallet-intro">查看推广佣金到账情况，提交并跟踪提现申请。</p>
      </div>
      <a-button :loading="loading" :icon="h(ReloadOutlined)" @click="refresh">刷新</a-button>
    </div>

    <a-spin :spinning="loading && !walletLoaded">
      <div class="wallet-summary-grid">
        <section class="wallet-card wallet-card--primary">
          <span class="wallet-card__label">可提现余额</span>
          <strong>¥{{ money(wallet.availableAmount) }}</strong>
          <span class="wallet-card__hint">提现后金额进入冻结中</span>
        </section>
        <section class="wallet-card">
          <span class="wallet-card__label">冻结金额</span>
          <strong>¥{{ money(wallet.frozenAmount) }}</strong>
          <span class="wallet-card__hint">待审核或打款中的提现</span>
        </section>
        <section class="wallet-card">
          <span class="wallet-card__label">累计入账</span>
          <strong>¥{{ money(wallet.totalEarned) }}</strong>
          <span class="wallet-card__hint">退款期结束后的税后佣金</span>
        </section>
        <section class="wallet-card">
          <span class="wallet-card__label">累计提现</span>
          <strong>¥{{ money(wallet.totalWithdrawn) }}</strong>
          <span class="wallet-card__hint">已完成的提现金额</span>
        </section>
      </div>
    </a-spin>

    <div class="wallet-content-grid">
      <section class="wallet-panel">
        <div class="wallet-panel__header">
          <div>
            <h2>申请提现</h2>
            <p>每次提现会先冻结余额，审核完成后打款。</p>
          </div>
        </div>
        <a-form layout="vertical" @submit.prevent>
          <a-form-item label="提现金额">
            <a-input-number
              v-model:value="withdrawForm.amount"
              :min="0.01"
              :max="wallet.availableAmount"
              :precision="2"
              style="width: 100%"
              placeholder="请输入提现金额"
            />
          </a-form-item>
          <a-row :gutter="14">
            <a-col :xs="24" :sm="8">
              <a-form-item label="账户类型">
                <a-select v-model:value="withdrawForm.accountType">
                  <a-select-option value="ALIPAY">支付宝</a-select-option>
                  <a-select-option value="BANK">银行卡</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="16">
              <a-form-item label="收款账号">
                <a-input v-model:value="withdrawForm.accountNo" placeholder="请输入收款账号" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="收款人姓名">
            <a-input v-model:value="withdrawForm.accountName" placeholder="请输入收款人姓名" />
          </a-form-item>
          <a-button type="primary" block :loading="withdrawing" @click="submitWithdrawal">提交提现申请</a-button>
        </a-form>
      </section>

      <section class="wallet-panel">
        <div class="wallet-panel__header">
          <div>
            <h2>佣金明细</h2>
            <p>每笔推广订单的税前、税费和实得金额。</p>
          </div>
          <a-tag color="blue">{{ commissionPage.total }} 笔</a-tag>
        </div>
        <a-table
          :columns="commissionColumns"
          :data-source="commissionPage.records"
          :loading="commissionLoading"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ x: 680 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actualAmount'">
              <strong>¥{{ money(record.actualAmount) }}</strong>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="commissionStatusColor(record.status)">{{ commissionStatusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
          </template>
        </a-table>
        <a-pagination
          v-model:current="commissionQuery.pageNo"
          v-model:page-size="commissionQuery.pageSize"
          size="small"
          :total="commissionPage.total"
          :show-size-changer="false"
          @change="loadCommissions"
        />
      </section>
    </div>

    <section class="wallet-panel">
      <div class="wallet-panel__header">
        <div>
          <h2>提现记录</h2>
          <p>查看申请状态和打款流水。</p>
        </div>
        <a-tag>{{ withdrawalPage.total }} 笔</a-tag>
      </div>
      <a-table
        :columns="withdrawalColumns"
        :data-source="withdrawalPage.records"
        :loading="withdrawalLoading"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 760 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">¥{{ money(record.amount) }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="withdrawalStatusColor(record.status)">{{ withdrawalStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">{{ formatTime(record.createTime) }}</template>
          <template v-else-if="column.key === 'action' && record.status === 0">
            <a-button type="link" size="small" :loading="cancellingId === record.id" @click="cancelWithdrawal(record)">取消</a-button>
          </template>
        </template>
      </a-table>
      <a-pagination
        v-model:current="withdrawalQuery.pageNo"
        v-model:page-size="withdrawalQuery.pageSize"
        size="small"
        :total="withdrawalPage.total"
        :show-size-changer="false"
        @change="loadWithdrawals"
      />
    </section>
  </section>
</template>

<script setup lang="ts">
import { useArtistWalletPage } from './Index'

const {
  h,
  ReloadOutlined,
  loading,
  walletLoaded,
  withdrawing,
  commissionLoading,
  withdrawalLoading,
  cancellingId,
  wallet,
  withdrawForm,
  commissionQuery,
  withdrawalQuery,
  commissionPage,
  withdrawalPage,
  commissionColumns,
  withdrawalColumns,
  money,
  formatTime,
  commissionStatusText,
  commissionStatusColor,
  withdrawalStatusText,
  withdrawalStatusColor,
  refresh,
  loadCommissions,
  loadWithdrawals,
  submitWithdrawal,
  cancelWithdrawal,
} = useArtistWalletPage()
</script>

<style scoped src="./Index.css"></style>
