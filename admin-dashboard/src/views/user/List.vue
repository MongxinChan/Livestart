<template>
  <div>
    <a-page-header
      title="用户管理"
      sub-title="平台用户、账号状态与观演人信息管理"
      :ghost="false"
      style="margin-bottom: 24px"
    />

    <a-card :bordered="false">
      <div style="display: flex; justify-content: space-between; margin-bottom: 16px; gap: 12px; flex-wrap: wrap">
        <a-space>
          <a-input
            :value="filters.phone"
            style="width: 220px"
            placeholder="请输入手机号搜索"
            allow-clear
            :maxlength="11"
            @update:value="onPhoneChange"
            @pressEnter="onSearch"
          />
          <a-button type="primary" @click="onSearch">搜索</a-button>
        </a-space>

        <a-space>
          <span style="color: #666">用户类型</span>
          <a-select
            :value="filters.userType"
            :options="userTypeOptions"
            style="width: 180px"
            allow-clear
            placeholder="请选择用户类型"
            @change="onUserTypeChange"
          />
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        :scroll="{ x: 1250 }"
        @change="onTableChange"
        @expand="onExpand"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'phone'">
            <span>{{ maskPhone(record.phone) }}</span>
          </template>
          <template v-else-if="column.key === 'isVerified'">
            <a-tag :color="record.isVerified === 1 ? 'green' : 'default'">
              {{ formatVerifiedStatus(record.isVerified) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userType'">
            <a-tag :color="record.userType === 4 ? 'purple' : record.userType === 3 ? 'blue' : 'default'">
              {{ formatUserType(record.userType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'red' : 'green'">
              {{ record.status === 0 ? '已封禁' : '正常' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span>{{ formatDate(record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-dropdown :disabled="!canOperate(record)">
                <a :class="{ disabled: !canOperate(record) }">设置类型</a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item :disabled="isCurrentUserType(record, 1)" @click="updateUserType(record, 1)">
                      设为普通用户
                    </a-menu-item>
                    <a-menu-item :disabled="isCurrentUserType(record, 2)" @click="updateUserType(record, 2)">
                      设为艺人
                    </a-menu-item>
                    <a-menu-item :disabled="isCurrentUserType(record, 3)" @click="updateUserType(record, 3)">
                      设为场地管理员
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-divider type="vertical" />
              <a-popconfirm
                :title="record.status === 0 ? '确认解封该账号？' : '确认封禁该账号？'"
                @confirm="toggleUserStatus(record)"
              >
                <a :class="{ disabled: !canOperate(record) }" :style="{ color: record.status === 0 ? '#52c41a' : '#ff4d4f' }">
                  {{ record.status === 0 ? '解封' : '封禁' }}
                </a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>

        <template #expandedRowRender="{ record }">
          <div style="padding: 8px 0">
            <a-tag color="blue" style="margin-bottom: 8px">观演人列表</a-tag>
            <a-table
              :columns="visitorColumns"
              :data-source="visitorMap[record.id] || []"
              :loading="loadingVisitors[record.id]"
              row-key="id"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column: visitorColumn, record: visitor }">
                <template v-if="visitorColumn.key === 'cardNo'">
                  {{ maskIdCard(visitor.cardNo) }}
                </template>
                <template v-else-if="visitorColumn.key === 'mobile'">
                  {{ maskPhone(visitor.mobile) }}
                </template>
              </template>
            </a-table>
          </div>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="bindModalOpen"
      title="配置场地管理员"
      :confirm-loading="bindSubmitting"
      ok-text="确认绑定"
      cancel-text="取消"
      @ok="submitBindVenue"
    >
      <a-form layout="vertical">
        <a-form-item label="手机号">
          <a-input :value="maskPhone(bindForm.phone)" disabled />
        </a-form-item>
        <a-form-item label="关联场馆" required>
          <a-select
            v-model:value="bindForm.venueId"
            :options="venueSelectOptions"
            placeholder="请选择要归属的场馆"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { useUserList } from './useUserList'

const {
  columns,
  visitorColumns,
  userTypeOptions,
  loading,
  list,
  pagination,
  filters,
  bindModalOpen,
  bindSubmitting,
  bindForm,
  venueSelectOptions,
  visitorMap,
  loadingVisitors,
  maskPhone,
  maskIdCard,
  formatDate,
  canOperate,
  isCurrentUserType,
  formatUserType,
  formatVerifiedStatus,
  onTableChange,
  onUserTypeChange,
  onPhoneChange,
  onSearch,
  submitBindVenue,
  updateUserType,
  toggleUserStatus,
  onExpand,
} = useUserList()
</script>

<style scoped>
.disabled {
  color: #999 !important;
  cursor: not-allowed;
  pointer-events: none;
}
</style>
