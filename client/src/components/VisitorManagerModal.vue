<template>
  <a-modal
    :open="open"
    :footer="null"
    :width="560"
    centered
    destroy-on-close
    @cancel="handleCancel"
  >
    <template #title>
      <span>
        <TeamOutlined style="color: var(--ls-color-primary); margin-right: 8px" />
        常用观演人管理
      </span>
    </template>

    <a-list
      class="visitor-manager-list"
      :data-source="visitors"
      :loading="loading"
      size="small"
      style="max-height: 240px; overflow-y: auto; margin-bottom: 20px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.08)"
    >
      <template #renderItem="{ item }">
        <a-list-item>
          <a-space direction="vertical" :size="2">
            <span style="font-weight: 600; font-size: 14px">{{ item.realName }}</span>
            <span style="font-size: 12px; color: var(--ls-text-secondary)">
              {{ item.cardTypeDesc || '身份证' }}
              <span style="margin-left: 8px; font-family: monospace; letter-spacing: 0.5px">{{ item.cardNo }}</span>
              <span v-if="item.mobile" style="margin-left: 12px">手机号：{{ item.mobile }}</span>
            </span>
          </a-space>
          <template #actions>
            <a-button type="link" size="small" @click="startEdit(item)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-popconfirm
              title="确定要删除这个常用观演人吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="deleteVisitor(item.id)"
            >
              <a-button type="link" danger size="small">
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
            </a-popconfirm>
          </template>
        </a-list-item>
      </template>
    </a-list>

    <a-divider dashed style="margin: 16px 0" />

    <div style="background: rgba(255,255,255,0.02); padding: 16px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.04)">
      <h5 style="font-weight: 700; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
        <span>{{ isEditMode ? '编辑观演人' : '新增观演人' }}</span>
        <a-button
          v-if="isEditMode"
          type="link"
          size="small"
          style="padding: 0; font-size: 12px"
          @click="cancelEdit"
        >
          取消编辑
        </a-button>
      </h5>

      <a-alert
        v-if="!isEditMode"
        type="info"
        show-icon
        style="margin-bottom: 16px"
        message="新增时请填写合法证件号"
        description="当前后端会校验证件格式。身份证必须是有效的 18 位号码，不能只凑长度。"
      />

      <a-form layout="vertical" :model="form" @finish="handleSubmit">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="真实姓名" required>
              <a-input
                :value="form.realName"
                placeholder="请输入真实姓名"
                @update:value="handleRealNameInput"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号">
              <a-input
                :value="form.mobile"
                placeholder="选填，11 位手机号"
                maxlength="11"
                inputmode="numeric"
                @update:value="handleMobileInput"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="证件号" required>
          <a-input
            :value="form.cardNo"
            placeholder="请输入合法的 18 位身份证号"
            :maxlength="18"
            :disabled="isEditMode"
            style="font-family: monospace"
            @update:value="handleCardNoInput"
          />
        </a-form-item>

        <a-typography-paragraph v-if="!isEditMode" type="secondary" style="font-size: 12px; margin-bottom: 12px">
          证件号保存后不可修改，如填错请删除后重新新增。
        </a-typography-paragraph>

        <a-button
          type="primary"
          block
          html-type="submit"
          :loading="submitting"
          style="margin-top: 8px; font-weight: 600"
        >
          {{ isEditMode ? '保存修改' : '确认新增' }}
        </a-button>
      </a-form>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { DeleteOutlined, EditOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { useVisitorManager } from '@/composables/visitor/useVisitorManager'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [val: boolean]
  'change': []
}>()

const {
  loading,
  submitting,
  visitors,
  isEditMode,
  form,
  deleteVisitor,
  startEdit,
  cancelEdit,
  handleSubmit,
  handleCancel,
  handleRealNameInput,
  handleMobileInput,
  handleCardNoInput,
} = useVisitorManager(props, emit)
</script>
