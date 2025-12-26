<script lang="ts">
export default { name: 'CommentView' }
</script>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatLineRound,
  Search,
  CircleCloseFilled,
  View,
  Delete,
  UserFilled,
  Clock
} from '@element-plus/icons-vue'
import { curd } from '@/api/curd'
import { queryPagePortApi } from '@/api/port'
import { queryCommentPageApi, deleteCommentApi } from '@/api/comment'

// --- 1. 文章列表逻辑 (Level 1) ---

// 搜索配置
const formSchemaConfig = [
  { prop: 'title', label: '文章标题', type: 'input', placeholder: '搜索文章标题' },
  { prop: 'authorId', label: '作者ID', type: 'input', placeholder: '搜索作者ID' },
]

// 复用 curd.ts 获取文章列表
const {
  searchForm,
  formSchema,
  list: articleList,
  currentPage,
  pageSize,
  total,
  search: searchArticles,
  handleSizeChange,
  handleCurrentChange,
} = curd(
  { queryPage: queryPagePortApi },
  {
    initialItem: {},
    formSchema: formSchemaConfig,
    title: '评论管理',
    // 自动计算列宽的样本数据
    labels: {
      title: '文章标题',
      authorId: '作者ID',
      categoryName: '分类',
    },
  }
)

// 清空搜索
const clearSearch = () => {
  formSchema.forEach((f: any) => {
    searchForm[f.prop] = ''
  })
  currentPage.value = 1
  searchArticles()
}

// --- 2. 评论列表逻辑 (Level 2) ---

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const currentArticle = ref<any>(null)
const commentList = ref<any[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentSize = ref(10)

// 打开评论抽屉
const openComments = (row: any) => {
  currentArticle.value = row
  drawerVisible.value = true
  commentPage.value = 1
  loadComments()
}

// 加载评论数据
const loadComments = async () => {
  if (!currentArticle.value) return
  drawerLoading.value = true
  try {
    const res = await queryCommentPageApi({
      pageNum: commentPage.value,
      pageSize: commentSize.value,
      articleId: currentArticle.value.id
    })
    
    // 兼容多种后端返回格式
    const payload = res as any
    let rawList: any[] = []
    let totalVal = 0

    // 情况1: 标准结构 { code: 1, data: { list: [], total: 0 } }
    if (payload?.code === 1 || payload?.code === 200) {
      const data = payload.data || {}
      // 检查 data 是否本身就是数组
      if (Array.isArray(data)) {
        rawList = data
        totalVal = data.length
      } else {
        rawList = data.records || data.list || data.rows || []
        totalVal = Number(data.total || data.totalRow || data.count || 0)
      }
    } 
    // 情况2: 直接返回 Map { list: [], total: 0 } (对应 UserPostController 直接返回 service 结果)
    else if (payload && (payload.list || payload.records || Array.isArray(payload))) {
      rawList = payload.list || payload.records || (Array.isArray(payload) ? payload : [])
      totalVal = Number(payload.total || payload.totalCount || payload.count || rawList.length || 0)
    }

    // 数据清洗：给缺失的字段加上默认值
    const list = rawList.map((item: any) => {
      // 昵称兜底
      const finalNickName = item.nickName || item.nickname || item.userName || item.user_name || `用户${item.userId || item.user_id || '未知'}`
      
      // 头像兜底
      const finalAvatar = item.avatarUrl || item.avatar_url || item.headImg || item.head_img || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

      return {
        ...item,
        id: item.id || item.commentId,
        userName: finalNickName,
        userAvatar: finalAvatar,
        content: item.content,
        createTime: item.createTime || item.create_time || '',
        userId: item.userId || item.user_id,
      }
    })

    commentList.value = list
    commentTotal.value = totalVal

  } catch (e) {
    console.error(e)
    ElMessage.error('评论加载失败')
    commentList.value = []
    commentTotal.value = 0
  }  finally {
    drawerLoading.value = false
  }
}

// 删除评论
const handleDeleteComment = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？此操作不可恢复。', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    
    const res = await deleteCommentApi(row.id)
    if (res?.code === 1 || res?.code === 200) {
      ElMessage.success('删除成功')
      loadComments() // 刷新列表
    } else {
      ElMessage.error(res?.message || '删除失败')
    }
  } catch (e) {
    // 取消或出错
  }
}

const handleCommentPageChange = (val: number) => {
  commentPage.value = val
  loadComments()
}
</script>

<template>
  <div class="comment-wrapper">
    <h2>评论管理</h2>

    <!-- 顶部搜索栏 -->
    <div class="container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item v-for="field in formSchema" :key="field.prop" :label="field.label">
          <el-input
            v-if="field.type === 'input'"
            v-model="searchForm[field.prop]"
            :placeholder="field.placeholder"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="searchArticles">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="clearSearch">
            <el-icon><CircleCloseFilled /></el-icon> 清空
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 文章列表 (Level 1) -->
    <div class="container mt-4">
      <el-table :data="articleList" style="width: 100%" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <el-table-column label="文章信息" min-width="300">
          <template #default="{ row }">
            <div class="article-info">
              <div class="article-title">{{ row.title }}</div>
              <div class="article-meta">
                <el-tag size="small" effect="plain">{{ row.categoryName || '默认分类' }}</el-tag>
                <span class="meta-time">{{ row.createTime }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="authorId" label="作者ID" width="100" align="center" />
        
        <!-- 假设后端有返回 commentCount，如果没有则显示 -- -->
        <el-table-column label="评论数" width="100" align="center">
          <template #default="{ row }">
            <span class="comment-count-badge" :class="{ 'has-comments': row.commentCount > 0 }">
              {{ row.commentCount ?? '--' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="openComments(row)">
              <el-icon class="mr-1"><ChatLineRound /></el-icon> 查看评论
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 文章分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 评论详情抽屉 (Level 2) -->
    <el-drawer
      v-model="drawerVisible"
      title="评论详情"
      size="600px"
      destroy-on-close
    >
      <template #header>
        <div class="drawer-header">
          <span class="drawer-title">评论列表</span>
          <span class="drawer-subtitle" v-if="currentArticle">
            所属文章：{{ currentArticle.title }}
          </span>
        </div>
      </template>

      <div class="drawer-content" v-loading="drawerLoading">
        <el-empty v-if="!drawerLoading && commentList.length === 0" description="暂无评论数据" />
        
        <div v-else class="comment-list">
          <div v-for="item in commentList" :key="item.id" class="comment-item">
            <div class="comment-header">
              <div class="user-info">
                <el-avatar :size="24" :src="item.userAvatar" :icon="UserFilled" />
                <span class="username">{{ item.userName || item.userId || '匿名用户' }}</span>
              </div>
              <div class="time-info">
                <el-icon><Clock /></el-icon>
                {{ item.createTime }}
              </div>
            </div>
            
            <div class="comment-body">
              {{ item.content }}
            </div>
            
            <div class="comment-footer">
              <el-button 
                type="danger" 
                link 
                size="small" 
                :icon="Delete"
                @click="handleDeleteComment(item)"
              >
                删除评论
              </el-button>
            </div>
          </div>
        </div>

        <!-- 评论分页 -->
        <div class="drawer-pagination" v-if="commentTotal > 0">
          <el-pagination
            v-model:current-page="commentPage"
            :page-size="commentSize"
            :total="commentTotal"
            layout="prev, pager, next"
            small
            @current-change="handleCommentPageChange"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.container {
  margin: 12px 0;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.article-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.article-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-time {
  font-size: 12px;
  color: #909399;
}

.comment-count-badge {
  font-weight: bold;
  color: #909399;
}
.comment-count-badge.has-comments {
  color: #409eff;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 抽屉样式 */
.drawer-header {
  display: flex;
  flex-direction: column;
}
.drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}
.drawer-subtitle {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 500px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}
.comment-item:hover {
  background: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.username {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.time-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.comment-body {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 12px;
  word-break: break-all;
}

.comment-footer {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
  padding-top: 8px;
}

.drawer-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>