<template>
  <div class="audit-container">
    <!-- 顶部统计与操作栏 -->
    <div class="header-section">
      <div class="title-group">
        <div class="icon-box">
          <el-icon><Stamp /></el-icon>
        </div>
        <div>
          <h2 class="page-title">内容审核工作台</h2>
          <p class="sub-title">当前待处理任务：<span class="highlight">{{ total }}</span> 篇</p>
        </div>
      </div>
      <el-button type="primary" circle :icon="Refresh" @click="fetchData" title="刷新列表" />
    </div>

    <!-- 主要内容区：卡片网格 -->
    <div class="content-grid" v-loading="loading">
      <el-empty v-if="!loading && tableData.length === 0" description="暂无待审核内容，去喝杯咖啡吧~" />
      
      <div v-else class="grid-layout">
        <div 
          v-for="item in tableData" 
          :key="item.id" 
          class="task-card"
          @click="openAuditWorkspace(item)"
        >
          <!-- 卡片封面 -->
          <div class="card-cover">
            <!-- 修改：使用 getCover(item) 获取图片 -->
            <img 
              v-if="getCover(item)"
              v-show="!item.imgLoadError"
              :src="getCover(item)" 
              alt="cover" 
              loading="lazy"
              @error="item.imgLoadError = true"
            />
            <!-- 当没有图片 或 图片加载失败时显示图标 -->
            <div v-if="!getCover(item) || item.imgLoadError" class="no-cover">
              <el-icon :size="32"><Picture /></el-icon>
            </div>
            
            <div class="status-badge">待审核</div>
            <div class="hover-overlay">
              <span>点击开始审核</span>
            </div>
          </div>
          
          <!-- 卡片信息 -->
          <div class="card-body">
            <h3 class="card-title" :title="item.title">{{ item.title }}</h3>
            <div class="card-meta">
              <div class="meta-item">
                <el-icon><User /></el-icon>
                <span>{{ item.authorName || item.authorId || '未知作者' }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Clock /></el-icon>
                <span>{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 全屏审核工作台 Dialog -->
    <el-dialog
      v-model="dialogVisible"
      fullscreen
      destroy-on-close
      :show-close="false"
      class="audit-workspace-dialog"
      @opened="initCherryPreview"
    >
      <template #header>
        <div class="workspace-header">
          <div class="header-left">
            <el-tag type="warning" effect="dark" class="mr-3">审核中</el-tag>
            <span class="article-title">{{ currentItem?.title }}</span>
          </div>
          <el-button circle :icon="Close" @click="dialogVisible = false" />
        </div>
      </template>

      <div class="workspace-body" v-loading="detailLoading">
        <!-- 左侧：元数据面板 -->
        <div class="meta-panel">
          <div class="panel-title">基本信息</div>
          
          <div class="info-group">
            <label>文章 ID</label>
            <div>{{ currentItem?.id }}</div>
          </div>
          
          <div class="info-group">
            <label>作者信息</label>
            <div class="flex items-center gap-2">
              <el-avatar :size="24" :src="currentItem?.authorAvatar" />
              <span>{{ currentItem?.authorName || currentItem?.authorId }}</span>
            </div>
          </div>

          <div class="info-group">
            <label>所属分类</label>
            <el-tag size="small">{{ currentItem?.categoryName || currentItem?.categoryId || '默认' }}</el-tag>
          </div>

          <div class="info-group">
            <label>提交时间</label>
            <div>{{ currentItem?.createTime }}</div>
          </div>

          <div class="info-group">
            <label>封面预览</label>
            <div class="cover-preview-box">
              <el-image 
                v-if="getCover(currentItem)"
                :src="getCover(currentItem)"
                :preview-src-list="[getCover(currentItem)]"
                fit="contain"
                class="w-full h-full"
              >
                <template #error>
                  <div class="flex flex-col items-center justify-center h-full text-gray-400">
                    <el-icon :size="24"><Picture /></el-icon>
                    <span class="text-xs mt-1">加载失败</span>
                  </div>
                </template>
              </el-image>
              <span v-else class="text-xs text-gray-400">无封面</span>
            </div>
          </div>
        </div>

        <!-- 右侧：内容预览面板 -->
        <div class="content-panel">
          <div id="markdown-preview-container"></div>
        </div>
      </div>

      <template #footer>
        <div class="workspace-footer">
          <div class="footer-input">
            <el-input 
              v-model="rejectReason" 
              placeholder="若驳回，请在此输入原因（选填）" 
              clearable
              class="reason-input"
            >
              <template #prefix>
                <el-icon><EditPen /></el-icon>
              </template>
            </el-input>
          </div>
          <div class="footer-actions">
            <el-button type="danger" plain :icon="Close" @click="handleReject">驳回文章</el-button>
            <el-button type="success" :icon="Check" @click="handlePass">通过并发布</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Stamp, Refresh, Picture, User, Clock, 
  Close, Check, EditPen 
} from '@element-plus/icons-vue'
import Cherry from 'cherry-markdown'
import { 
  queryPagePortApi, 
  updatePortApi, 
  getPortByIdApi 
} from '@/api/port'

// --- 状态定义 ---
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const detailLoading = ref(false)
const currentItem = ref<any>(null)
const rejectReason = ref('')
let cherryInstance: any = null

// --- 核心修改：完全复用 editor.vue 的图片解析逻辑 ---
const getCover = (item: any) => {
  if (!item) return ''

  // 1. 尝试获取所有可能的字段名 (editor.vue 逻辑)
  const raw = item.coverImages || item.coverImage || item.cover_images || item.cover_image || ''
  
  let imgArray: string[] = []
  
  // 2. 解析逻辑 (editor.vue 逻辑)
  if (Array.isArray(raw)) {
    imgArray = raw
  } else if (typeof raw === 'string' && raw.trim() !== '') {
    const str = raw.trim()
    // 尝试判断是否为 JSON 格式
    if (str.startsWith('[') && str.endsWith(']')) {
      try {
        const parsed = JSON.parse(str)
        if (Array.isArray(parsed)) {
          imgArray = parsed
        } else {
          imgArray = str.split(',')
        }
      } catch (e) {
        imgArray = str.split(',')
      }
    } else {
      imgArray = str.split(',')
    }
  }

  // 3. 过滤无效链接
  const validImages = imgArray
    .map(url => typeof url === 'string' ? url.trim() : '')
    .filter(url => url.length > 0)

  // 4. 返回第一张
  return validImages.length > 0 ? validImages[0] : ''
}

// --- 数据获取 ---
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: 1,
      pageSize: 50,
      status: 0 
    }
    
    const res = await queryPagePortApi(params)
    
    if (res?.code === 1 || res?.code === 200) {
      const rawData = res.data
      
      let list = []
      if (Array.isArray(rawData)) {
        list = rawData
      } else if (rawData) {
        list = rawData.records || rawData.list || rawData.rows || rawData.data || []
      }

      // 为每个 item 初始化 imgLoadError 状态
      tableData.value = list.map((item: any) => ({
        ...item,
        imgLoadError: false
      }))
      
      total.value = Number(rawData?.total || rawData?.totalRow || rawData?.count || list.length || 0)
    } else {
      tableData.value = []
      total.value = 0
      ElMessage.warning(res?.message || '获取数据异常')
    }
  } catch (error) {
    console.error('获取审核列表失败', error)
    ElMessage.error('数据加载失败')
  } finally {
    loading.value = false
  }
}

// --- 格式化时间 ---
const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  return timeStr.split(' ')[0]
}

// --- 打开审核工作台 ---
const openAuditWorkspace = async (item: any) => {
  currentItem.value = { ...item }
  rejectReason.value = ''
  dialogVisible.value = true
  detailLoading.value = true

  try {
    const res = await getPortByIdApi(item.id)
    if ((res?.code === 1 || res?.code === 200) && res.data) {
      currentItem.value = res.data
    }
  } catch (e) {
    ElMessage.error('获取文章详情失败')
  } finally {
    detailLoading.value = false
    nextTick(() => {
      initCherryPreview()
    })
  }
}

// --- 初始化 Markdown 预览 ---
const initCherryPreview = () => {
  const container = document.getElementById('markdown-preview-container')
  if (!container) return
  
  container.innerHTML = ''

  cherryInstance = new Cherry({
    id: 'markdown-preview-container',
    value: currentItem.value?.content || '> 该文章暂无内容',
    editor: {
      defaultModel: 'previewOnly',
      height: '100%',
    },
    toolbars: {
      toolbar: false,
    },
  })
}

// --- 审核操作 ---
const handlePass = async () => {
  try {
    const res = await updatePortApi({
      ...currentItem.value,
      status: 1 // 1: 已发布
    })
    
    if (res?.code === 1 || res?.code === 200) {
      ElMessage.success('审核通过，文章已发布')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('请求异常')
  }
}

const handleReject = async () => {
  try {
    await ElMessageBox.confirm('确定要驳回该文章吗？文章将移入回收站。', '驳回确认', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await updatePortApi({
      ...currentItem.value,
      status: 2 // 2: 已删除/驳回
    })

    if (res?.code === 1 || res?.code === 200) {
      ElMessage.info(`文章已驳回${rejectReason.value ? '：' + rejectReason.value : ''}`)
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e) {
    // 取消操作
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* 容器样式 */
.audit-container {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 头部样式 */
.header-section {
  background: white;
  padding: 20px 30px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
}

.title-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-box {
  width: 48px;
  height: 48px;
  background: #eff6ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  font-size: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.sub-title {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.highlight {
  color: #f59e0b;
  font-weight: 700;
  font-size: 16px;
}

/* 网格布局 */
.content-grid {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}

.grid-layout {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

/* 卡片样式 */
.task-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  position: relative;
}

.task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.06);
  border-color: #3b82f6;
}

.card-cover {
  height: 160px;
  background: #f1f5f9;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-cover {
  color: #cbd5e1;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
}

.hover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(59, 130, 246, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.2s;
}

.task-card:hover .hover-overlay {
  opacity: 1;
}

.card-body {
  padding: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  color: #94a3b8;
  font-size: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 工作台 Dialog 样式 */
:deep(.audit-workspace-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.audit-workspace-dialog .el-dialog__body) {
  padding: 0;
  height: calc(100vh - 60px - 70px);
  overflow: hidden;
}

:deep(.audit-workspace-dialog .el-dialog__footer) {
  padding: 0;
  border-top: 1px solid #e2e8f0;
}

.workspace-header {
  height: 60px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.header-left {
  display: flex;
  align-items: center;
}

.article-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.workspace-body {
  display: flex;
  height: 100%;
}

.meta-panel {
  width: 300px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  padding: 24px;
  overflow-y: auto;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 20px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-group {
  margin-bottom: 24px;
}

.info-group label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.info-group div {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.cover-preview-box {
  width: 100%;
  height: 140px;
  background: #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-panel {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
  background: white;
}

.workspace-footer {
  height: 70px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.reason-input {
  width: 400px;
}

</style>