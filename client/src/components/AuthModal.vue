<template>
  <a-modal v-model:open="showAuthModal" wrap-class-name="auth-modal" :footer="null" :width="420" centered destroy-on-close>
    <template #title>
      <span style="font-weight: 800; font-size: 1.1rem">
        <UserOutlined style="margin-right: 8px; color: var(--ls-color-primary)" />
        手机验证码登录
      </span>
    </template>

    <div class="auth-intro">
      <span class="auth-intro-kicker">WELCOME TO LIVESTART</span>
      <p>使用手机号完成验证。新用户会自动创建账号。</p>
    </div>

    <a-form layout="vertical" @submit.prevent="handleAuthSubmit">
      <a-form-item label="手机号">
        <a-input
          :value="authForm.phone"
          placeholder="请输入 11 位登录手机号"
          size="large"
          maxlength="11"
          inputmode="numeric"
          @update:value="handlePhoneInput"
        >
          <template #prefix><MobileOutlined style="color: rgba(255,255,255,0.25)" /></template>
        </a-input>
      </a-form-item>

      <a-form-item label="短信验证码">
        <a-space style="width: 100%">
          <a-input
            :value="authForm.code"
            placeholder="请输入 6 位验证码"
            size="large"
            maxlength="6"
            inputmode="numeric"
            style="flex: 1"
            @update:value="handleCodeInput"
          >
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

function keepDigits(value: string) {
  return value.replace(/\D/g, '')
}

function handlePhoneInput(value: string) {
  authForm.phone = keepDigits(value).slice(0, 11)
}

function handleCodeInput(value: string) {
  authForm.code = keepDigits(value).slice(0, 6)
}
</script>
