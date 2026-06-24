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
        <TeamOutlined style="color: var(--ant-color-primary); margin-right: 8px" />
        常用观演人配置中心
      </span>
    </template>

    <!-- 已有观演人列表 -->
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
              身份证: <span style="font-family: monospace; letter-spacing: 0.5px">{{ item.cardNo }}</span>
              <span v-if="item.mobile" style="margin-left: 12px">手机: {{ item.mobile }}</span>
            </span>
          </a-space>
          <template #actions>
            <a-button type="link" size="small" @click="startEdit(item)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-popconfirm
              title="确定要物理注销该常用观演人吗？"
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

    <!-- 增/改 动态表单 -->
    <div style="background: rgba(255,255,255,0.02); padding: 16px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.04)">
      <h5 style="font-weight: 700; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
        <span>{{ isEditMode ? '📝 修改常用观演人' : '➕ 新增常用观演人' }}</span>
        <a-button v-if="isEditMode" type="link" size="small" style="padding: 0; font-size: 12px" @click="cancelEdit">
          取消修改
        </a-button>
      </h5>

      <a-form layout="vertical" :model="form" @finish="handleSubmit">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="真实姓名" required>
              <a-input v-model:value="form.realName" placeholder="请输入真实姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机号码">
              <a-input v-model:value="form.mobile" placeholder="选填 手机号" :maxlength="11" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="身份证号码" required>
          <a-input
            v-model:value="form.cardNo"
            placeholder="请输入18位二代身份证"
            :maxlength="18"
            :disabled="isEditMode"
            style="font-family: monospace"
          />
        </a-form-item>

        <a-button
          type="primary"
          block
          html-type="submit"
          :loading="submitting"
          style="margin-top: 8px; font-weight: 600"
        >
          {{ isEditMode ? '确认保存修改' : '确认添加并入库' }}
        </a-button>
      </a-form>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { TeamOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
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
  handleCancel
} = useVisitorManager(props, emit)
</script>
