<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Avatar, 
  Document, 
  Odometer, 
  SwitchButton, 
  Expand, 
  Fold,
  Location,
  Stamp,
  ChatLineRound 
} from '@element-plus/icons-vue'

// --- 类型定义 ---
interface LoginUser {
  userId: number
  userName: string
  role: number // 0: 管理员, 1: 员工
  avatarUrl?: string
}

interface MenuItem {
  index: string
  title: string
  icon: any
}

// --- 状态管理 ---
const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const currentUser = ref<LoginUser | null>(null)
const DEFAULT_AVATAR = 'http://yumoni.top/upload/Transparent_Akkarin_Transparentized.png'

// --- 菜单配置 ---
const menuItems: MenuItem[] = [
  { index: '/dashboard', title: '仪表盘', icon: Odometer },
  { index: '/audit', title: '审核管理', icon: Stamp },
  { index: '/user', title: '用户管理', icon: Avatar },
  { index: '/port', title: '文章管理', icon: Document },
  { index: '/comments', title: '评论管理', icon: ChatLineRound },
]

// --- 逻辑方法 ---
const loadUser = () => {
  const raw = localStorage.getItem('login_user')
  if (!raw) {
    currentUser.value = null
    return
  }
  try {
    const parsed = JSON.parse(raw) as LoginUser
    parsed.role = typeof (parsed as any).role === 'string' ? Number((parsed as any).role) : parsed.role
    currentUser.value = parsed
  } catch (e) {
    console.error('parse login_user error:', e)
    currentUser.value = null
  }
}

const isAdmin = computed(() => currentUser.value?.role === 0)

const logout = () => {
  localStorage.removeItem('auth_token')
  localStorage.removeItem('login_user')
  ElMessage.success('已退出登录')
  router.replace('/login')
}

const navigate = (path: string) => {
  router.push(path)
}

// --- 生命周期 ---
onMounted(loadUser)
watch(() => route.path, () => loadUser())
</script>

<template>
  <div class="flex h-screen w-full bg-[#f3f4f6] font-sans overflow-hidden">
    
    <!-- 左侧侧边栏：-->
    <aside 
      class="relative flex flex-col bg-[#1e293b] text-slate-300 transition-all duration-300 ease-in-out shadow-xl z-20"
      :class="isCollapse ? 'w-20' : 'w-64'"
    >
      <div class="absolute top-0 left-0 w-full h-32 bg-gradient-to-b from-blue-500/10 to-transparent pointer-events-none"></div>

      <!-- Logo 区域 -->
      <div class="h-16 flex items-center justify-center border-b border-slate-700/50 relative z-10">
        <div class="flex items-center gap-2 overflow-hidden whitespace-nowrap">
          <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold shadow-lg shadow-blue-900/50">
            西
          </div>
          <span v-if="!isCollapse" class="text-lg font-bold text-white tracking-wide transition-opacity duration-200">
            西瓜头条
          </span>
        </div>
      </div>


      <!-- 菜单列表 -->
      <div class="flex-1 py-6 px-3 space-y-1 overflow-y-auto relative z-10">
        <div 
          v-for="item in menuItems" 
          :key="item.index"
          @click="navigate(item.index)"
          class="group flex items-center px-3 py-3 rounded-lg cursor-pointer transition-all duration-200 mb-1"
          :class="route.path.startsWith(item.index) 
            ? 'bg-blue-600 text-white shadow-md shadow-blue-900/30' 
            : 'hover:bg-slate-800 hover:text-white'"
        >
          <el-icon :size="20" :class="route.path.startsWith(item.index) ? '' : 'text-slate-400 group-hover:text-white'">
            <component :is="item.icon" />
          </el-icon>
          
          <span v-if="!isCollapse" class="ml-3 text-sm font-medium">
            {{ item.title }}
          </span>
        </div>
      </div>

      <!-- 底部折叠按钮 -->
      <div class="p-4 border-t border-slate-700/50 relative z-10">
        <button 
          @click="isCollapse = !isCollapse"
          class="w-full flex items-center justify-center p-2 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
        >
          <el-icon :size="18">
            <component :is="isCollapse ? Expand : Fold" />
          </el-icon>
        </button>
      </div>
    </aside>

    <!-- 右侧主内容区 -->
    <div class="flex-1 flex flex-col min-w-0 bg-[#f3f4f6]">
      
      <!-- 顶部导航栏： -->
      <header class="h-16 px-6 flex items-center justify-between bg-gradient-to-r from-[#1e293b] via-blue-600 to-purple-600 shadow-md z-10">
        <!-- 左侧：当前位置指示 -->
        <div class="flex items-center text-white/90 text-sm">
          <el-icon class="mr-2"><Location /></el-icon>
          <span>管理控制台</span>
          <span class="mx-2">/</span>
          <span class="font-bold text-white tracking-wide">
            {{ menuItems.find(i => route.path.startsWith(i.index))?.title || '首页' }}
          </span>
        </div>

        <!-- 右侧用户信息 -->
        <div class="flex items-center gap-4">
          <div v-if="currentUser" class="flex items-center gap-3 pl-4">
            <div class="text-right hidden sm:block">
              <div class="text-sm font-medium text-white">{{ currentUser.userName }}</div>
              <div class="text-xs text-blue-100">{{ isAdmin ? '管理员' : '普通员工' }}</div>
            </div>
            
            <img 
              :src="currentUser.avatarUrl || DEFAULT_AVATAR" 
              class="w-9 h-9 rounded-full object-cover border-2 border-white/20 shadow-sm" 
              alt="avatar" 
            />

            <button 
              @click="logout"
              class="ml-2 p-2 rounded-full hover:bg-white/20 text-white transition-colors"
              title="退出登录"
            >
              <el-icon :size="18"><SwitchButton /></el-icon>
            </button>
          </div>
        </div>
      </header>

      <!-- 内容区域：保持原有背景色，确保表格等组件显示正常 -->
      <main class="flex-1 overflow-auto p-6 relative">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="route.fullPath" :is-admin="isAdmin" v-if="Component" />
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 简单的淡入淡出动画，不夸张 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>