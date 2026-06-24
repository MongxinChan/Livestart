<template>
  <div>
    <a-page-header title="票档管理" sub-title="管理各演出的票种与库存" :ghost="false" style="margin-bottom: 24px">
      <template #extra>
        <a-button type="primary" @click="openForm()">
          <template #icon><PlusOutlined /></template>
          新增票档
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false">
      <div style="margin-bottom: 16px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap">
        <span style="white-space: nowrap">按演出筛选</span>
        <a-radio-group v-model:value="filterMode" button-style="solid">
          <a-radio-button value="id">按 ID</a-radio-button>
          <a-radio-button value="name">按名称</a-radio-button>
        </a-radio-group>
        <a-input-number
          v-if="filterMode === 'id'"
          v-model:value="filterEventIdInput"
          :min="1"
          placeholder="输入演出 ID"
          style="width: 200px"
        />
        <a-select
          v-else
          v-model:value="filterEventIdInput"
          show-search
          allow-clear
          placeholder="选择演出"
          style="width: 320px"
          :options="eventOptions"
          :filter-option="filterEventOption"
          :not-found-content="eventOptions.length === 0 ? '暂无演出数据' : '无匹配项'"
        />
        <a-button type="primary" @click="onFilter">查询</a-button>
        <a-button @click="onClearFilter">重置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'event'">
            <div style="display: flex; flex-direction: column; line-height: 1.4">
              <span style="font-weight: 600">{{ eventTitleMap[record.eventId] || '未知演出' }}</span>
              <span style="font-size: 11px; color: #8c8c8c">ID: {{ record.eventId }}</span>
            </div>
          </template>

          <template v-if="column.key === 'stock'">
            <a-progress
              :percent="record.totalStock ? Math.round((record.remainingStock / record.totalStock) * 100) : 0"
              :status="record.remainingStock === 0 ? 'exception' : 'active'"
              :format="() => `${record.remainingStock} / ${record.totalStock}`"
              size="small"
            />
            <div style="font-size: 11px; color: #8c8c8c; margin-top: 4px">
              一开: {{ record.stage1Stock ?? 0 }} / 二开: {{ record.stage2Stock ?? 0 }}
            </div>
            <div style="margin-top: 6px">
              <a-tag :color="record.stage2Released ? 'orange' : 'default'">
                {{ record.stage2Released ? 'Released' : 'Pending' }}
              </a-tag>
            </div>
          </template>

          <template v-if="column.key === 'price'">
            <span style="color: #ff4d4f; font-weight: 600">¥{{ record.sellingPrice }}</span>
            <span
              v-if="record.originalPrice !== record.sellingPrice"
              style="text-decoration: line-through; color: #999; margin-left: 8px; font-size: 12px"
            >
              ¥{{ record.originalPrice }}
            </span>
          </template>

          <template v-if="column.key === 'action'">
            <a class="table-action-link" @click="openForm(record)">编辑</a>
            <a-divider type="vertical" />
            <a class="table-action-link" @click="openIncreaseStock(record)">增发库存</a>
            <a-divider type="vertical" />
            <a-popconfirm title="确认删除该票档？" @confirm="onDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formVisible"
      :title="editingId ? '编辑票档' : '新增票档'"
      :confirm-loading="submitting"
      @ok="onSubmit"
      width="500px"
    >
      <a-form :model="formData" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="演出" required>
          <a-select
            v-model:value="formData.eventId"
            show-search
            placeholder="选择演出"
            :options="eventOptions"
            :filter-option="filterEventOption"
          />
        </a-form-item>
        <a-form-item label="票种名称" required>
          <a-input v-model:value="formData.title" placeholder="如：VIP票 / 学生票 / 预售票" />
        </a-form-item>
        <a-form-item label="原价">
          <a-input-number v-model:value="formData.originalPrice" :min="0" :precision="2" prefix="¥" style="width: 100%" />
        </a-form-item>
        <a-form-item label="售价" required>
          <a-input-number v-model:value="formData.sellingPrice" :min="0" :precision="2" prefix="¥" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="!editingId" label="总库存" required>
          <a-input-number v-model:value="formData.totalStock" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item v-else label="总库存">
          <a-input-number :value="formData.totalStock" disabled style="width: 100%" />
        </a-form-item>
        <a-form-item label="一开数量">
          <a-input-number v-model:value="formData.stage1Stock" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="二开数量">
          <a-input-number v-model:value="formData.stage2Stock" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="限购数量">
          <a-input-number v-model:value="formData.limitNum" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="increaseStockVisible"
      title="增发库存"
      :confirm-loading="increaseStockSubmitting"
      @ok="onSubmitIncreaseStock"
      width="420px"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="票档">
          <a-input :value="increaseStockTarget ? `${increaseStockTarget.title} (ID: ${increaseStockTarget.id})` : ''" disabled />
        </a-form-item>
        <a-form-item label="当前库存">
          <a-input :value="increaseStockTarget ? `${increaseStockTarget.remainingStock} / ${increaseStockTarget.totalStock}` : ''" disabled />
        </a-form-item>
        <a-form-item label="增发数量" required>
          <a-input-number v-model:value="increaseStockCount" :min="1" :precision="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'
import { useTicketSkuList } from './useTicketSkuList'

const {
  columns,
  loading,
  list,
  pagination,
  eventOptions,
  eventTitleMap,
  filterEventOption,
  filterMode,
  filterEventIdInput,
  onFilter,
  onClearFilter,
  onTableChange,
  formVisible,
  submitting,
  editingId,
  formData,
  increaseStockVisible,
  increaseStockSubmitting,
  increaseStockTarget,
  increaseStockCount,
  openForm,
  openIncreaseStock,
  onSubmit,
  onSubmitIncreaseStock,
  onDelete,
} = useTicketSkuList()
</script>
