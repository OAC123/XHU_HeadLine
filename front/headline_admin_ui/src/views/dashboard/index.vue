<template>
  <div class="dashboard-container">
    <!-- 顶部欢迎与日期 -->
    <div class="header-row">
      <div>
        <h1 class="welcome-title">仪表盘</h1>
        <p class="welcome-sub">欢迎回来，这里是系统运营概览</p>
      </div>
      <span class="date-badge">
        {{ currentDate }}
      </span>
    </div>

    <!-- 数据统计卡片 (3个) -->
    <div class="stats-grid">
      <!-- 文章总数 -->
      <div class="stat-card blue-theme group">
        <div class="card-bg-decoration"></div>
        <div class="card-content">
          <p class="stat-label">平台文章总数</p>
          <div class="stat-value-row">
            <p class="stat-num">{{ stats.articleTotal }}</p>
            <span class="stat-unit">篇</span>
          </div>
        </div>
        <div class="stat-footer">
          <el-icon class="mr-1"><Document /></el-icon>
          <span>内容库</span>
        </div>
      </div>

      <!-- 用户总数 -->
      <div class="stat-card purple-theme group">
        <div class="card-bg-decoration"></div>
        <div class="card-content">
          <p class="stat-label">注册用户总数</p>
          <div class="stat-value-row">
            <p class="stat-num">{{ stats.userTotal }}</p>
            <span class="stat-unit">人</span>
          </div>
        </div>
        <div class="stat-footer">
          <el-icon class="mr-1"><User /></el-icon>
          <span>用户池</span>
        </div>
      </div>

      <!-- 待审核 -->
      <div class="stat-card orange-theme group cursor-pointer" @click="$router.push('/audit')">
        <div class="card-bg-decoration"></div>
        <div class="card-content">
          <p class="stat-label">待审核文章</p>
          <div class="stat-value-row">
            <p class="stat-num text-orange">{{ stats.pendingTotal }}</p>
            <span class="stat-unit">篇待处理</span>
          </div>
        </div>
        <div class="stat-footer">
          <el-icon class="mr-1"><Stamp /></el-icon>
          <span>点击去审核</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-section">
      <div class="chart-header">
        <h2 class="chart-title">
          <span class="title-indicator"></span>
          近7日数据趋势
        </h2>
        <div class="chart-legend">
          <span class="legend-item">
            <span class="dot blue"></span> 新增文章
          </span>
          <span class="legend-item">
            <span class="dot purple"></span> 新增用户
          </span>
        </div>
      </div>
      
      <div ref="chartRef" class="chart-canvas"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue';
import { Document, User, Stamp } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { queryPageApi } from '@/api/user';
import { queryPagePortApi } from '@/api/port';

// --- 状态定义 ---
const chartRef = ref<HTMLElement | null>(null);
let myChart: echarts.ECharts | null = null;

const stats = reactive({
  articleTotal: 0,
  userTotal: 0,
  pendingTotal: 0
});

const currentDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN', { 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric', 
    weekday: 'long' 
  });
});

// --- 数据获取与处理 ---

// 辅助函数：生成最近7天的日期数组 ['MM-DD', ...]
const getLast7Days = () => {
  const days = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const month = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    days.push(`${month}-${day}`);
  }
  return days;
};

// 辅助函数：从 createTime 中提取 MM-DD
const extractDate = (timeStr: string) => {
  if (!timeStr) return '';
  // 假设格式为 "YYYY-MM-DD HH:mm:ss"
  const datePart = timeStr.split(' ')[0]; // YYYY-MM-DD
  const parts = datePart.split('-');
  if (parts.length >= 3) {
    return `${parts[1]}-${parts[2]}`; // MM-DD
  }
  return '';
};

// 核心逻辑：加载数据并聚合
const loadData = async () => {
  try {
    // 1. 并行请求数据
    // 为了统计趋势，我们拉取最近 100 条记录进行前端聚合
    // 注意：如果数据量巨大，应该由后端提供统计接口。这里是前端模拟统计。
    const [userRes, articleRes, pendingRes] = await Promise.all([
      queryPageApi({ page: 1, size: 100 }), 
      queryPagePortApi({ pageNum: 1, pageSize: 100 }),
      queryPagePortApi({ pageNum: 1, pageSize: 1, status: 0 }) // 只查数量
    ]);

    // 2. 解析总数
    // 兼容后端可能返回的结构 res.data 或 res
    const userData = userRes.data || userRes || {};
    const articleData = articleRes.data || articleRes || {};
    const pendingData = pendingRes.data || pendingRes || {};

    stats.userTotal = Number(userData.total || userData.totalRow || userData.count || 0);
    stats.articleTotal = Number(articleData.total || articleData.totalRow || articleData.count || 0);
    stats.pendingTotal = Number(pendingData.total || pendingData.totalRow || pendingData.count || 0);

    // 3. 聚合图表数据
    const dateLabels = getLast7Days();
    const userTrend = new Array(7).fill(0);
    const articleTrend = new Array(7).fill(0);

    // 解析用户列表 (适配 records/rows/list)
    const userList = userData.records || userData.rows || userData.list || [];
    userList.forEach((u: any) => {
      const dateStr = extractDate(u.createTime);
      const index = dateLabels.indexOf(dateStr);
      if (index !== -1) {
        userTrend[index]++;
      }
    });

    // 解析文章列表
    const articleList = articleData.records || articleData.rows || articleData.list || [];
    articleList.forEach((a: any) => {
      const dateStr = extractDate(a.createTime);
      const index = dateLabels.indexOf(dateStr);
      if (index !== -1) {
        articleTrend[index]++;
      }
    });

    // 4. 渲染图表
    initChart(dateLabels, articleTrend, userTrend);

  } catch (error) {
    console.error('Dashboard data load failed:', error);
  }
};

// --- 图表初始化 ---
const initChart = (dates: string[], articleData: number[], userData: number[]) => {
  if (!chartRef.value) return;
  
  // 如果已存在实例，先销毁
  if (myChart) myChart.dispose();

  myChart = echarts.init(chartRef.value);

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#eee',
      borderWidth: 1,
      textStyle: { color: '#333' }
    },
    legend: {
      data: ['新增文章', '新增用户'],
      bottom: 0,
      icon: 'circle'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#9ca3af' }
    },
    yAxis: {
      type: 'value',
      splitLine: {
        lineStyle: { type: 'dashed', color: '#f3f4f6' }
      },
      axisLabel: { color: '#9ca3af' },
      minInterval: 1 // 保证Y轴刻度为整数
    },
    series: [
      {
        name: '新增文章',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 8,
        data: articleData,
        itemStyle: { color: '#3b82f6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0)' }
          ])
        },
        lineStyle: { width: 3 }
      },
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        showSymbol: false,
        symbolSize: 8,
        data: userData,
        itemStyle: { color: '#a855f7' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(168, 85, 247, 0.2)' },
            { offset: 1, color: 'rgba(168, 85, 247, 0)' }
          ])
        },
        lineStyle: { width: 3 }
      }
    ]
  };

  myChart.setOption(option);
};

// --- 生命周期 ---
onMounted(() => {
  loadData();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (myChart) myChart.dispose();
});

const handleResize = () => {
  myChart?.resize();
};
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background-color: #f3f4f6;
}

/* 头部 */
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.welcome-sub {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.date-badge {
  font-size: 14px;
  color: #6b7280;
  background: white;
  padding: 4px 12px;
  border-radius: 9999px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 24px;
}

@media (min-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.card-bg-decoration {
  position: absolute;
  right: 0;
  top: 0;
  width: 96px;
  height: 96px;
  border-bottom-left-radius: 9999px;
  margin-right: -16px;
  margin-top: -16px;
  transition: transform 0.3s ease;
}

.stat-card:hover .card-bg-decoration {
  transform: scale(1.1);
}

.card-content {
  position: relative;
  z-index: 10;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
}

.stat-value-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 8px;
}

.stat-num {
  font-size: 30px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
}

.stat-unit {
  font-size: 12px;
  color: #9ca3af;
}

.stat-footer {
  margin-top: 16px;
  display: flex;
  align-items: center;
  font-size: 14px;
  width: fit-content;
  padding: 4px 8px;
  border-radius: 4px;
}

/* 主题色配置 */
.blue-theme .card-bg-decoration { background-color: #eff6ff; }
.blue-theme .stat-footer { color: #2563eb; background-color: #eff6ff; }

.purple-theme .card-bg-decoration { background-color: #faf5ff; }
.purple-theme .stat-footer { color: #9333ea; background-color: #faf5ff; }

.orange-theme .card-bg-decoration { background-color: #fff7ed; }
.orange-theme .stat-footer { color: #ea580c; background-color: #fff7ed; }
.orange-theme .stat-num { color: #ea580c; }

/* 图表区域 */
.chart-section {
  flex: 1;
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  border: 1px solid #f3f4f6;
  display: flex;
  flex-direction: column;
  min-height: 400px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.chart-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-indicator {
  width: 4px;
  height: 24px;
  background-color: #2563eb;
  border-radius: 9999px;
}

.chart-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #6b7280;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.dot.blue { background-color: #3b82f6; }
.dot.purple { background-color: #a855f7; }

.chart-canvas {
  flex: 1;
  min-height: 300px;
}
</style>
