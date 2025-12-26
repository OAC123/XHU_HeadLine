<template>
  <!-- 动态绑定背景样式 -->
  <div 
    class="setting-page"
    :style="bgImage ? { backgroundImage: `url(${bgImage})` } : {}"
  >
    <!-- 顶部：用户信息 + 头像上传 -->
    <el-card class="user-card" :loading="loadingProfile" shadow="never">
      <div class="user-header">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :http-request="uploadAvatar"
        >
          <img
            v-if="userInfo.avatar"
            :src="userInfo.avatar"
            alt="avatar"
            class="avatar"
          />
          <div
            v-else
            class="avatar-placeholder"
          >
            上传头像
          </div>
        </el-upload>

        <div class="user-meta">
          <div class="user-name-row">
            <span class="user-name">
              {{ userInfo.nickname || userInfo.username || '未命名用户' }}
            </span>
          </div>
          <div class="user-id">
            ID：{{ userInfo.id || '-' }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- 折叠：账号与安全 / 修改密码 -->
    <el-card class="section-card" shadow="never">
      <el-collapse v-model="activeSections">
        <!-- 账号与安全 -->
        <el-collapse-item name="account">
          <template #title>
            <span class="collapse-title">账号与安全</span>
          </template>

          <el-form
            :model="userInfo"
            label-width="90px"
          >
            <el-form-item label="用户 ID">
              <!-- ID 只读，不可修改 -->
              <el-input
                v-model="userInfo.id"
                disabled
              />
            </el-form-item>

            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" />
            </el-form-item>

            <el-form-item label="昵称">
              <el-input v-model="userInfo.nickname" />
            </el-form-item>

            <el-form-item label="手机号">
              <el-input v-model="userInfo.phone" />
            </el-form-item>
          </el-form>

          <div class="section-actions">
            <el-button
              type="primary"
              :loading="loadingProfile"
              @click="saveProfile"
            >
              保存基本信息
            </el-button>
          </div>
        </el-collapse-item>

        <!-- 修改密码 -->
        <el-collapse-item name="password">
          <template #title>
            <span class="collapse-title">密码与登录安全</span>
          </template>

          <el-form
            :model="passwordForm"
            label-width="90px"
          >
            <el-form-item label="当前密码">
              <el-input
                v-model="passwordForm.oldPassword"
                show-password
              />
            </el-form-item>

            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.newPassword"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认新密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                show-password
              />
            </el-form-item>
          </el-form>

          <div class="section-actions">
            <el-button
              type="primary"
              :loading="loadingPassword"
              @click="changePassword"
            >
              修改密码
            </el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 功能开关 -->
    <el-card
      class="section-card"
      header="个性化设置"
      shadow="never"
    >
      <div class="feature-row">
        <div>
          <div class="feature-title">页面切换动画</div>
          <div class="feature-sub">全局开启或关闭页面跳转时的过渡效果</div>
        </div>
        <el-switch 
          v-model="enableAnimation" 
          @change="setEnableAnimation"
        />
      </div>

      <!-- 2. 动画类型  -->
      <div class="feature-row" v-if="enableAnimation">
        <div>
          <div class="feature-title">动画风格</div>
          <div class="feature-sub">选择喜欢的切换方式</div>
        </div>
        <el-radio-group v-model="animationType" @change="setAnimationType" size="small">
          <el-radio-button label="slide">滑动</el-radio-button>
          <el-radio-button label="fade">淡入</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 自定义背景设置 -->
      <div class="feature-row bg-setting-row">
        <div>
          <div class="feature-title">自定义背景</div>
          <div class="feature-sub">上传图片作为全站背景</div>
        </div>
        <div class="bg-actions">
           <el-upload
            class="bg-uploader"
            :show-file-list="false"
            :http-request="uploadBackground"
            accept="image/*"
          >
            <el-button size="small" :loading="uploadingBg">上传图片</el-button>
          </el-upload>
          <el-button v-if="bgImage" size="small" type="danger" link @click="clearBackground">清除</el-button>
        </div>
      </div>
      <!-- 已移除预览区域 -->

      <!-- 原有的 Scroll Island 开关 -->
      <div class="feature-row">
        <div>
          <div class="feature-title">阅读进度条</div>
          <div class="feature-sub">控制文章详情页顶部的滚动进度提示组件</div>
        </div>
        <el-switch v-model="featureToggles.scrollIsland" />
      </div>

      <!-- 原来的占位开关保持不变 -->
      <div class="feature-row">
        <div>
          <div class="feature-title">新消息通知</div>
          <div class="feature-sub">开启后，将在有新评论、点赞时提醒</div>
        </div>
        <el-switch v-model="featureToggles.newMessageNotify" />
      </div>

      <div class="feature-row">
        <div>
          <div class="feature-title">夜间模式</div>
          <div class="feature-sub">根据此开关统一控制深色主题（占位）</div>
        </div>
        <el-switch v-model="featureToggles.nightMode" />
      </div>

      <div class="feature-row">
        <div>
          <div class="feature-title">内容过滤</div>
          <div class="feature-sub">过滤敏感或低质量内容（占位，尚未接入）</div>
        </div>
        <el-switch v-model="featureToggles.contentFilter" />
      </div>
    </el-card>

    <!-- 退出登录 -->
    <el-card class="section-card danger-card" shadow="never">
      <div class="logout-row">
        <div>
          <div class="logout-title">退出登录</div>
          <div class="logout-sub">
            退出后需要重新登录才能使用账号相关功能
          </div>
        </div>
        <el-button
          type="danger"
          @click="logout"
        >
          退出登录
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="js">
// ...existing code...
// (script 部分保持不变，逻辑已经包含了 bgImage 的读取和设置)
// ...existing code...
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import instance from '@/utils/axios/main.js'
import { useAnimationTransitionStore } from '@/stores'
import { useLoginPromptStore } from '@/stores/loginPrompt' 

defineOptions({
  name: 'SettingPage'
})

const router = useRouter()
const loginUserId = ref(null)
const animationStore = useAnimationTransitionStore()
const { enableAnimation, animationType } = storeToRefs(animationStore)
const { setEnableAnimation, setAnimationType } = animationStore
const loginPromptStore = useLoginPromptStore()
const activeSections = ref([])
const loadingProfile = ref(false)
const loadingPassword = ref(false)
const bgImage = ref('')
const uploadingBg = ref(false)
const userInfo = ref({
  id: '',
  username: '',
  nickname: '',
  phone: '',
  avatar: '',
})
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const featureToggles = ref({
  scrollIsland: true,
  newMessageNotify: true,
  nightMode: false,
  contentFilter: true,
})

watch(
  () => featureToggles.value.scrollIsland,
  (newValue) => {
    localStorage.setItem('enable_scroll_island', newValue ? '1' : '0')
  }
)

const loadUserInfo = async () => {
  try {
    if (!loginUserId.value) return
    loadingProfile.value = true
    const res = await instance.get('/user/info', {
      params: { id: loginUserId.value },
    })
    if (res.code === 0) {
      ElMessage.error(res.message || '获取用户信息失败')
      return
    }
    const data = res.data ?? res
    userInfo.value = {
      id: data.id ?? '',
      username: data.username ?? '',
      nickname: data.nickname ?? data.nickName ?? '',
      phone: data.phone ?? '',
      avatar: data.avatar ?? '',
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取用户信息失败')
  } finally {
    loadingProfile.value = false
  }
}

onMounted(() => {
  try {
    const raw = localStorage.getItem('login_user')
    if (raw) {
      const info = JSON.parse(raw)
      loginUserId.value = info.userId || null
    }
  } catch (e) {
    console.error('解析本地登录用户信息失败', e)
  }
  if (loginUserId.value) {
    loadUserInfo()
  }
  const saved = localStorage.getItem('enable_scroll_island')
  if (saved !== null) {
    featureToggles.value.scrollIsland = saved === '1'
  }
  const storedBg = localStorage.getItem('backgroundImage')
  if (storedBg) {
    bgImage.value = storedBg
  }
})

const uploadAvatar = async (option) => {
  try {
    const formData = new FormData()
    formData.append('image', option.file)
    const res = await instance.post('/user/upload', formData)
    const data = res.data || res
    if (data.code === 1 || data.code === 200) {
      userInfo.value.avatar = data.imageUrl || data.data
      ElMessage.success('头像上传成功')
      option.onSuccess(data, option.file)
    } else {
      ElMessage.error(data.message || '头像上传失败')
      option.onError(new Error(data.message))
    }
  } catch (e) {
    ElMessage.error('头像上传失败')
    option.onError(e)
  }
}

const uploadBackground = async (options) => {
  const { file } = options
  uploadingBg.value = true
  try {
    const formData = new FormData()
    formData.append('image', file)
    const res = await instance.post('/user/upload', formData)
    const data = res.data || res
    if (data.code === 1 || data.code === 200) {
        const url = data.imageUrl || data.data
        if (url) {
            bgImage.value = url
            localStorage.setItem('backgroundImage', url)
            ElMessage.success('背景设置成功')
        }
    } else {
        ElMessage.error(data.message || '上传失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('上传出错')
  } finally {
    uploadingBg.value = false
  }
}

const clearBackground = () => {
    bgImage.value = ''
    localStorage.removeItem('backgroundImage')
    ElMessage.success('已恢复默认背景')
}

const saveProfile = async () => {
  try {
    if (!loginUserId.value) return
    loadingProfile.value = true
    const payload = {
      id: userInfo.value.id || loginUserId.value,
      username: userInfo.value.username,
      nickname: userInfo.value.nickname,
      phone: userInfo.value.phone,
      avatar: userInfo.value.avatar,
    }
    const res = await instance.put('/user/info', payload)
    if (res.code === 1) {
      ElMessage.success('保存成功')
      loadUserInfo()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败：' + e)
  } finally {
    loadingProfile.value = false
  }
}

const changePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次新密码不一致')
    return
  }
  try {
    loadingPassword.value = true
    const res = await instance.put('/user/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })
    if (res.code === 1 || res.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('修改失败：' + e)
  } finally {
    loadingPassword.value = false
  }
}

const logout = async () => {
  loginPromptStore.showLogout()
}
</script>

<style scoped>
.setting-page {
  padding: 16px;
  padding-bottom: 90px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100vh; /* 确保铺满屏幕 */
  
  /* 背景图通用属性 */
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  background-repeat: no-repeat;
  background-color: #f5f7fa; /* 默认背景色 */
}

/* 卡片半透明化，适配背景图 */
.user-card, .section-card {
  border-radius: 16px;
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-uploader {
  cursor: pointer;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  font-size: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
}

.user-id {
  font-size: 12px;
  color: #9ca3af;
}

.section-actions {
  margin-top: 8px;
  text-align: right;
}

.collapse-title {
  font-size: 14px;
  font-weight: 600;
}

/* 功能开关区域样式 */
.feature-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.feature-title {
  font-size: 14px;
  font-weight: 500;
}

.feature-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

/* 背景设置相关样式 */
.bg-setting-row {
  align-items: flex-start;
}

.bg-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.danger-card .logout-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logout-title {
  font-size: 16px;
  font-weight: 600;
}
.logout-sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>