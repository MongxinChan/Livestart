<template>
  <a-modal v-model:open="showAuthModal" :footer="null" :width="380" centered destroy-on-close>
    <template #title>
      <span style="font-weight: 800; font-size: 1.1rem">
        <UserOutlined style="margin-right: 8px; color: var(--ant-color-primary)" />
        快捷验证登录与自动注册
      </span>
    </template>

    <a-typography-paragraph type="secondary" style="font-size: 12px; margin-bottom: 18px">
      已支持手机验证码快捷登录。未注册手机号将自动在后台进行隐式自动注册及社交资料建档。
    </a-typography-paragraph>

    <a-form layout="vertical" @submit.prevent="handleAuthSubmit">
      <a-form-item label="手机号">
        <a-input v-model:value="authForm.phone" placeholder="请输入11位登录手机号" size="large" maxlength="11">
          <template #prefix><MobileOutlined style="color: rgba(255,255,255,0.25)" /></template>
        </a-input>
      </a-form-item>

      <a-form-item label="短信验证码">
        <a-space style="width: 100%">
          <a-input v-model:value="authForm.code" placeholder="请输入6位验证码" size="large" maxlength="6" style="flex: 1">
            <template #prefix><SafetyOutlined style="color: rgba(255,255,255,0.25)" /></template>
          </a-input>
          <a-button size="large" :disabled="countdown > 0" @click="sendVerificationCode" style="min-width: 120px">
            {{ countdown > 0 ? `${countdown}s 重试` : '获取验证码' }}
          </a-button>
        </a-space>
      </a-form-item>

      <a-button type="primary" block size="large" html-type="submit" :loading="authLoading" style="margin-top: 10px">
        立即登录 / 自动注册
      </a-button>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { UserOutlined, MobileOutlined, SafetyOutlined } from '@ant-design/icons-vue'
import { useAuth } from '../composables/useAuth'

const { showAuthModal, authForm, authLoading, countdown, sendVerificationCode, handleAuthSubmit } = useAuth()
</script>
