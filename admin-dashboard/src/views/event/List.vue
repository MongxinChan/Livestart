<template>
  <div>
    <a-page-header title="演出管理" sub-title="创建、编辑、上下架演出项目" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-upload :show-upload-list="false" accept=".xlsx,.xls" :before-upload="beforeUpload">
          <a-button>
            <template #icon><UploadOutlined /></template>
            Excel 导入
          </a-button>
        </a-upload>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          创建演出
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <a-table
        :columns="eventTableColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'posterUrl'">
            <a-image
              :src="record.posterUrl"
              :width="60"
              :height="40"
              style="object-fit: cover; border-radius: 4px"
              fallback="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='60' height='40'%3E%3Crect fill='%23f0f0f0' width='60' height='40'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%23999' font-size='10'%3E无图%3C/text%3E%3C/svg%3E"
            />
          </template>
          <template v-if="column.key === 'eventType'">
            <a-tag :color="record.eventType === 0 ? 'blue' : 'purple'">
              {{ record.eventType === 0 ? 'Livehouse' : '演唱会' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'performerName'">
            <span>{{ record.performerName || '未指定艺人' }}</span>
          </template>
          <template v-if="column.key === 'genre'">
            <template v-if="record.genre">
              <a-tag v-for="genre in record.genre.split(',')" :key="genre" color="purple">{{ genre }}</a-tag>
            </template>
            <a-tag v-else color="default">未分类</a-tag>
          </template>
          <template v-if="column.key === 'ticketStage'">
            <a-tag :color="record.ticketStage === 2 ? 'orange' : 'blue'">
              {{ record.ticketStage === 2 ? '二开' : '一开' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="eventStatusColors[record.status]">{{ eventStatusLabels[record.status] }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a class="table-action-link" @click="openForm(record)">编辑</a>
              <a-divider type="vertical" />
              <a-dropdown>
                <a>更多 <DownOutlined /></a>
                <template #overlay>
                  <a-menu @click="({ key }: any) => onAction(key, record)">
                    <a-menu-item key="config">演出配置</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="publish" :disabled="record.status !== 1">上架开售</a-menu-item>
                    <a-menu-item key="shelve" :disabled="record.status !== 2">下架</a-menu-item>
                    <a-menu-item key="terminate" :disabled="record.status >= 3">
                      <span style="color: #ff4d4f">终止售票</span>
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete">
                      <span style="color: #ff4d4f">删除</span>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑演出' : '创建演出'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      width="760px"
      class="form-modal"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="演出标题" required>
          <a-input v-model:value="formData.title" placeholder="如：周杰伦嘉年华世界巡回演唱会" />
        </a-form-item>
        <a-form-item label="演出类型" required>
          <a-radio-group v-model:value="formData.eventType">
            <a-radio :value="0">Livehouse（站票）</a-radio>
            <a-radio :value="1">演唱会（选座）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="关联场馆" required>
          <a-select v-model:value="formData.venueId" placeholder="选择场馆" show-search option-filter-prop="label" :options="venueOptions" />
        </a-form-item>
        <a-form-item label="出演艺人">
          <a-select v-model:value="formData.performerId" placeholder="选择歌手/艺人（可选）" allow-clear>
            <a-select-option v-for="performer in performerOptions" :key="performer.id" :value="performer.id">
              {{ performer.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="音乐风格">
          <a-select
            v-model:value="formData.styleIds"
            mode="multiple"
            show-search
            option-filter-prop="name"
            placeholder="请选择音乐风格"
            :options="styleOptions"
            :field-names="{ label: 'name', value: 'id' }"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="演出时间" required>
          <a-date-picker
            v-model:value="formData.startTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledPastDate"
            :disabled-time="disabledPastTime"
          />
        </a-form-item>
        <a-form-item label="海报图片">
          <a-input v-model:value="formData.posterUrl" placeholder="海报图片 URL 地址" />
        </a-form-item>

        <a-form-item label="开售阶段" required>
          <div style="display: flex; flex-direction: column; gap: 12px; width: 100%">
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-size: 13px; color: #666">当前共 {{ stageCount }} 个开售阶段</span>
              <a-button type="dashed" size="small" @click="addSaleStage">新增阶段</a-button>
            </div>

            <a-card
              v-for="(stage, index) in formData.saleStages"
              :key="`stage-${index}`"
              size="small"
              :bordered="true"
              style="border-radius: 10px; background: #fafafa"
            >
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
                <span style="font-weight: 600">阶段 {{ stage.stageNo }}</span>
                <a-button danger type="link" size="small" @click="removeSaleStage(index)">删除</a-button>
              </div>

              <a-row :gutter="12">
                <a-col :span="10">
                  <a-form-item label="阶段名称" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" style="margin-bottom: 12px">
                    <a-input v-model:value="stage.stageName" placeholder="如：预售第一阶段" />
                  </a-form-item>
                </a-col>
                <a-col :span="14">
                  <a-form-item label="开售时间" :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }" style="margin-bottom: 12px">
                    <a-date-picker
                      v-model:value="stage.saleStartTime"
                      show-time
                      format="YYYY-MM-DD HH:mm:ss"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      style="width: 100%"
                      :disabled-date="disabledPastDate"
                      :disabled-time="disabledPastTime"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item label="备注" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }" style="margin-bottom: 0">
                <a-input v-model:value="stage.remark" placeholder="如：会员优先 / 普通预售 / 加场补票" />
              </a-form-item>
            </a-card>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="configVisible"
      title="演出配置"
      :confirm-loading="configSubmitting"
      @ok="onConfigSubmit"
      width="620px"
    >
      <a-form :model="configData" :label-col="{ span: 7 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="选座模式">
          <a-radio-group v-model:value="configData.selectionMode">
            <a-radio :value="0">系统自动配座（高并发）</a-radio>
            <a-radio :value="1">手动选座（剧场）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="强制实名入场">
          <a-switch v-model:checked="configVerifyRequired" />
        </a-form-item>
        <a-form-item label="单人限购上限">
          <a-input-number v-model:value="configData.maxTicketsPerUser" :min="1" :max="20" style="width: 100%" />
        </a-form-item>
        <a-form-item label="退票政策">
          <a-select v-model:value="configData.refundPolicyType">
            <a-select-option :value="0">不可退票</a-select-option>
            <a-select-option :value="1">全额退票</a-select-option>
            <a-select-option :value="2">阶梯退票</a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="configData.refundPolicyType === 1 || configData.refundPolicyType === 2">
          <a-form-item label="全额退票截止">
            <a-input-number
              v-model:value="configData.tier1FreeRefundHours"
              :min="1"
              addon-after="小时"
              placeholder="开演前 X 小时"
              style="width: 100%"
            />
          </a-form-item>
        </template>
        <template v-if="configData.refundPolicyType === 2">
          <a-form-item label="部分退票截止">
            <a-input-number
              v-model:value="configData.tier2PartialRefundHours"
              :min="1"
              addon-after="小时"
              placeholder="开演前 Y 小时"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="手续费比例">
            <a-input-number
              v-model:value="configData.tier2RefundFeeRate"
              :min="0"
              :max="1"
              :step="0.05"
              :precision="2"
              placeholder="如：0.20 表示 20%"
              style="width: 100%"
            />
          </a-form-item>
        </template>
        <a-form-item label="允许转赠门票">
          <a-switch v-model:checked="configTransferable" />
        </a-form-item>
        <a-form-item label="候补购票功能">
          <a-switch v-model:checked="configWaitingAllowed" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { DownOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { eventTableColumns } from './columns'
import { eventStatusColors, eventStatusLabels, useEventList } from './useEventList'

const {
  loading,
  list,
  pagination,
  performerOptions,
  styleOptions,
  venueOptions,
  formVisible,
  submitting,
  editingId,
  formData,
  stageCount,
  disabledPastDate,
  disabledPastTime,
  addSaleStage,
  removeSaleStage,
  configVisible,
  configSubmitting,
  configData,
  configVerifyRequired,
  configTransferable,
  configWaitingAllowed,
  onTableChange,
  openForm,
  beforeUpload,
  onSubmit,
  onConfigSubmit,
  onAction,
} = useEventList()
</script>
