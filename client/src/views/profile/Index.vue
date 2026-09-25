<template>
  <section class="profile-page">
    <div class="profile-header">
      <div>
        <p class="profile-kicker">Account Profile</p>
        <h1>个人资料</h1>
      </div>
      <a-tag color="processing">{{ apiState.currentUser?.phone || '未绑定手机' }}</a-tag>
    </div>

    <div class="profile-grid">
      <section class="profile-panel profile-panel--identity">
        <div class="avatar-block">
          <a-avatar :size="104" :src="form.avatar || undefined" class="profile-avatar">
            {{ displayInitial }}
          </a-avatar>
          <div class="avatar-actions">
            <input
              ref="fileInputRef"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              class="hidden-file"
              @change="handleFileChange"
            />
            <a-space>
              <a-button :icon="h(UploadOutlined)" :loading="uploading" @click="chooseFile">
                上传头像
              </a-button>
              <a-button v-if="form.avatar" :icon="h(DeleteOutlined)" @click="clearAvatar">
                移除
              </a-button>
            </a-space>
            <p class="avatar-tip">支持 PNG、JPG、WebP，当前后端限制 10MB。</p>
          </div>
        </div>

        <div class="identity-copy">
          <strong>{{ form.realName || form.username || 'Livestart 用户' }}</strong>
          <span>{{ form.signature || '还没有填写个性签名' }}</span>
        </div>
      </section>

      <section class="profile-panel">
        <a-form layout="vertical" class="profile-form" @submit.prevent>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="昵称">
                <a-input
                  v-model:value="form.username"
                  :maxlength="24"
                  placeholder="给自己起一个好记的昵称"
                  show-count
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="真实姓名">
                <a-input
                  v-model:value="form.realName"
                  :maxlength="24"
                  placeholder="用于票务实名信息展示"
                  show-count
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="邮箱">
                <a-input v-model:value="form.mail" placeholder="name@example.com" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="性别">
                <a-segmented v-model:value="form.gender" :options="genderOptions" block />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="生日">
                <a-input v-model:value="form.birthday" type="date" />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="个性签名">
            <a-textarea
              v-model:value="form.signature"
              :maxlength="80"
              :rows="4"
              placeholder="写一句会显示在个人资料里的签名"
              show-count
            />
          </a-form-item>

          <div class="profile-actions">
            <a-button @click="resetForm">重置</a-button>
            <a-button type="primary" :loading="saving" @click="saveProfile">保存资料</a-button>
          </div>
        </a-form>
      </section>
    </div>

    <section v-if="isFan" class="profile-panel earning-panel">
      <div class="earning-panel__header">
        <div>
          <h2>绑定艺人推广关系</h2>
          <p>绑定后，后续支付订单会归因到该艺人的推广渠道。</p>
        </div>
      </div>
      <a-space direction="vertical" style="width: 100%">
        <a-input v-model:value="inviteCode" placeholder="输入艺人推广码" />
        <a-button type="primary" :loading="binding" @click="bindArtist">确认绑定</a-button>
      </a-space>
    </section>

  </section>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { apiState, request } from '@/composables/infra/useRequest'
import { persistSession } from '@/composables/sessionState'

const fileInputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const saving = ref(false)
const binding = ref(false)
const inviteCode = ref('')

const isFan = computed(() => Number(apiState.currentUser?.userType) === 1)

const genderOptions = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

const form = reactive({
  username: '',
  realName: '',
  avatar: '',
  mail: '',
  signature: '',
  gender: 0,
  birthday: '',
})

const displayInitial = computed(() => (form.realName || form.username || 'U').substring(0, 1))

function hydrateForm() {
  const user = apiState.currentUser
  form.username = user?.username || ''
  form.realName = user?.realName || ''
  form.avatar = user?.avatar || ''
  form.mail = user?.mail || ''
  form.signature = user?.signature || ''
  form.gender = user?.gender ?? 0
  form.birthday = normalizeBirthday(user?.birthday)
}

function normalizeBirthday(value?: string | number | Date) {
  if (!value) {
    return ''
  }
  if (typeof value === 'string') {
    return value.substring(0, 10)
  }
  return new Date(value).toISOString().substring(0, 10)
}

function chooseFile() {
  fileInputRef.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    message.warning('图片不能超过 10MB')
    return
  }

  const data = new FormData()
  data.append('file', file)
  uploading.value = true
  try {
    form.avatar = await request<string>('/api/live-start/admin/v1/user/avatar', {
      method: 'POST',
      body: data,
    })
    apiState.currentUser = {
      ...apiState.currentUser,
      avatar: form.avatar,
    }
    persistSession()
    message.success('头像上传成功')
  } catch (err: any) {
    message.error(err.message || '头像上传失败')
  } finally {
    uploading.value = false
  }
}

function clearAvatar() {
  form.avatar = ''
}

function resetForm() {
  hydrateForm()
}

async function saveProfile() {
  const phone = apiState.phone || apiState.currentUser?.phone
  if (!phone) {
    message.warning('请先登录后再修改资料')
    return
  }
  const username = form.username.trim()
  const realName = form.realName.trim()
  if (!username && !realName) {
    message.warning('昵称和真实姓名至少填写一项')
    return
  }

  saving.value = true
  try {
    await request('/api/live-start/admin/v1/user', {
      method: 'PUT',
      body: JSON.stringify({
        phone,
        username,
        realName,
        avatar: form.avatar,
        mail: form.mail.trim(),
        signature: form.signature.trim(),
        gender: form.gender,
        birthday: form.birthday || null,
      }),
    })

    const latest = await request<any>('/api/live-start/admin/v1/user/me')
    apiState.currentUser = {
      ...apiState.currentUser,
      ...latest,
      phone,
    }
    apiState.phone = phone
    if (latest?.id != null) {
      apiState.userId = String(latest.id)
    }
    persistSession()
    hydrateForm()
    message.success('个人资料已保存')
  } catch (err: any) {
    message.error(err.message || '保存资料失败')
  } finally {
    saving.value = false
  }
}

async function bindArtist() {
  if (!inviteCode.value.trim()) {
    message.warning('请输入艺人推广码')
    return
  }
  binding.value = true
  try {
    await request('/api/live-start/distribution/v1/artist/bind', {
      method: 'POST',
      body: JSON.stringify({ inviteCode: inviteCode.value.trim() }),
    })
    message.success('艺人绑定成功')
  } catch (err: any) {
    message.error(err.message || '绑定失败')
  } finally {
    binding.value = false
  }
}

hydrateForm()
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.earning-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.earning-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.earning-panel h2 {
  margin: 0;
  color: var(--ls-text-primary);
  font-size: 18px;
}

.earning-panel p {
  margin: 6px 0 0;
  color: var(--ls-text-secondary);
}

.profile-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.profile-kicker {
  margin: 0 0 6px;
  color: var(--ls-color-primary);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.profile-header h1 {
  margin: 0;
  color: var(--ls-text-primary);
  font-size: 28px;
  line-height: 1.2;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr);
  gap: 18px;
}

.profile-panel {
  border: 1px solid var(--ls-glass-border);
  border-radius: 8px;
  background: var(--ls-card-bg);
  box-shadow: var(--ls-card-shadow);
  padding: 22px;
}

.profile-panel--identity {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.avatar-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.profile-avatar {
  flex-shrink: 0;
  background: var(--ls-logo-gradient, #1677ff);
  font-size: 40px;
  font-weight: 700;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.avatar-tip {
  margin: 0;
  color: var(--ls-text-secondary);
  font-size: 12px;
}

.hidden-file {
  display: none;
}

.identity-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: center;
}

.identity-copy strong {
  color: var(--ls-text-primary);
  font-size: 18px;
}

.identity-copy span {
  color: var(--ls-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.profile-form :deep(.ant-form-item-label > label) {
  color: var(--ls-text-primary);
  font-weight: 600;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 820px) {
  .profile-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
