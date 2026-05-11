<template>
  <div class="jc-page">
    <!-- ========== HEADER ========== -->
    <header class="jc-header" :class="{ 'mc-active': mountainCityActive }">
      <div class="jc-landscape" aria-hidden="true">
        <div class="jc-mtn jc-mtn--far"></div>
        <div class="jc-mtn jc-mtn--mid"></div>
        <div class="jc-mtn jc-mtn--near"></div>
        <div class="jc-mist-1"></div>
        <div class="jc-mist-2"></div>
      </div>

      <!-- ====== 山城压迫感特效 ====== -->
      <template v-if="mountainCityActive">
        <div class="mc-overlay"></div>

        <div class="mc-buildings" aria-hidden="true">
          <div
            v-for="(layer, li) in buildingLayers"
            :key="li"
            class="mc-b-layer"
            :style="{ animationDelay: (0.3 + li * 0.18) + 's', zIndex: li }"
          >
            <div
              v-for="(b, bi) in layer"
              :key="bi"
              class="mc-b"
              :style="{
                left: b.left, width: b.width, height: b.height,
                '--b-op': b.opacity, background: b.background,
                borderRadius: b.borderRadius,
                animationDelay: (0.3 + li * 0.18 + bi * 0.02) + 's'
              }"
            ></div>
          </div>
        </div>

        <div class="mc-ink-glow" aria-hidden="true"></div>
        <div class="mc-ink-seal" aria-hidden="true"></div>

        <div class="mc-particles" aria-hidden="true">
          <div
            v-for="(p, pi) in particles"
            :key="pi"
            class="mc-p"
            :style="{
              left: p.left, top: p.top, width: p.width, height: p.height,
              animationDelay: p.delay, animationDuration: p.duration
            }"
          ></div>
        </div>
      </template>

      <div class="jc-header-content">
        <h1 class="jc-title" @click="toggleMountainCity" style="cursor: pointer;">
          <span
            v-for="(c, i) in '江城聘'"
            :key="i"
            class="jc-t-char"
            :style="{ animationDelay: i * 0.15 + 's' }"
          >{{ c }}</span>
        </h1>
        <p class="jc-subtitle">AI 驿栈 · 职达渝城</p>
        <p class="jc-desc">聚焦职业岗位，让机会与才华精准相遇</p>
      </div>
    </header>

    <!-- ========== 分类标签 ========== -->
    <div class="jc-filter-bar">
      <button
        v-for="cat in categories"
        :key="cat.key"
        :class="['filter-tag', { active: activeCat === cat.key }]"
        @click="activeCat = cat.key"
      >{{ cat.label }}</button>
    </div>

    <!-- ========== 搜索栏 ========== -->
    <div class="jc-search">
      <div class="jc-search-input">
        <el-icon class="s-icon"><Search /></el-icon>
        <input
          v-model="query"
          placeholder="搜索公司、职位、技能…"
          class="s-input"
        />
        <span v-if="query" class="s-clear" @click="query = ''">
          <el-icon><Close /></el-icon>
        </span>
      </div>
      <div class="jc-search-stats">
        共 <strong>{{ filtered.length }}</strong> 个岗位
      </div>
    </div>

    <!-- ========== 筛选栏（多维度） ========== -->
    <div class="jc-filters-row">
      <!-- 地区 -->
      <div class="filter-select">
        <span class="fs-label">地区</span>
        <select v-model="filterLocation" @change="onFilterChange">
          <option value="">全部</option>
          <option v-for="loc in locationOptions" :key="loc" :value="loc">{{ loc }}</option>
        </select>
      </div>

      <!-- 学历 -->
      <div class="filter-select">
        <span class="fs-label">学历</span>
        <select v-model="filterEducation" @change="onFilterChange">
          <option value="">全部</option>
          <option value="本科">本科</option>
          <option value="硕士">硕士</option>
          <option value="博士">博士</option>
          <option value="不限">不限</option>
        </select>
      </div>

      <!-- 来源类型 -->
      <div class="filter-source">
        <span class="fs-label">数据来源</span>
        <div class="source-tags">
          <button
            :class="['source-tag', { active: filterSource === '' }]"
            @click="filterSource = ''"
          >全部</button>
          <button
            :class="['source-tag', 'source-tag--official', { active: filterSource === 'official' }]"
            @click="filterSource = 'official'"
          ><span class="st-seal">🏛</span> 官方</button>
          <button
            :class="['source-tag', 'source-tag--commercial', { active: filterSource === 'commercial' }]"
            @click="filterSource = 'commercial'"
          ><span class="st-line">🏢</span> 商业</button>
        </div>
      </div>

      <!-- 墨滴开关 -->
      <div class="filter-toggle">
        <span class="fs-label">实时关注</span>
        <button
          :class="['ink-toggle', { on: realtimeOn }]"
          @click="realtimeOn = !realtimeOn"
          :title="realtimeOn ? '关闭实时关注' : '开启实时关注'"
        >
          <span class="ink-drop"></span>
          <span class="ink-wave" v-if="realtimeOn"></span>
        </button>
      </div>
    </div>

    <!-- ========== 岗位列表 ========== -->
    <div v-if="loading" class="jc-loading">
      <div class="loading-brush"></div>
      <p class="loading-text">墨迹未干，正在加载…</p>
    </div>

    <div v-else class="jc-list">
      <div
        v-for="(job, idx) in displayed"
        :key="job.id || idx"
        :class="['job-card']"
        @click="openDetail(job)"
      >
        <div class="jc-ink-bottom"></div>

        <!-- 来源标识 -->
        <span
          v-if="job.sourceType !== undefined"
          :class="['job-source-badge', sourceBadgeClass(job)]"
        >{{ job.source || '未知' }}</span>

        <!-- 头部 -->
        <div class="job-header">
          <div class="job-logo">{{ job.company?.charAt(0) || '?' }}</div>
          <div class="job-main">
            <h3 class="job-title">{{ job.title }}</h3>
            <p class="job-company">{{ job.company }}</p>
          </div>
          <span class="job-seal">{{ job.experience || job.education || '经验不限' }}</span>
        </div>

        <!-- 元信息 -->
        <div class="job-meta">
          <span class="jm-item jm-salary">{{ displaySalary(job) }}</span>
          <span class="jm-dot"></span>
          <span class="jm-item">{{ job.education || '学历不限' }}</span>
          <span class="jm-dot"></span>
          <span class="jm-item">{{ job.location || '重庆' }}</span>
        </div>

        <!-- 技能标签 -->
        <div class="job-tags">
          <span v-for="tag in parseTags(job.tags)" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filtered.length === 0" class="jc-empty">
        <div class="empty-brush"></div>
        <p class="empty-text">墨未浓，暂无匹配岗位</p>
        <p class="empty-hint">试试调整筛选条件</p>
      </div>
    </div>

    <!-- ========== 分页 ========== -->
    <div v-if="total > pageSize" class="jc-pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- ========== FOOTER ========== -->
    <footer class="jc-footer">
      <span class="jc-footer-seal">简历+</span>
      <span class="jc-footer-text">以墨为引 · 以智为舟</span>
    </footer>

    <!-- ========== 岗位详情侧滑面板 ========== -->
    <JobDetailPanel
      :is-open="detailPanelOpen"
      :job="selectedJob"
      @close="detailPanelOpen = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { Search, Close } from '@element-plus/icons-vue'
import { listJobs } from '@/api/jiangcheng'
import JobDetailPanel from './JobDetailPanel.vue'

/* ====== 筛选状态 ====== */
const activeCat = ref('all')
const query = ref('')
const filterLocation = ref('')
const filterEducation = ref('')
const filterSource = ref('')
const realtimeOn = ref(false)
const loading = ref(false)

/* ====== 山城压迫感特效 ====== */
const mountainCityActive = ref(false)

function toggleMountainCity() {
  mountainCityActive.value = !mountainCityActive.value
}

// 种子随机数 — 保证每次生成一致
function mulberry32(seed) {
  return function() {
    seed |= 0; seed = seed + 0x6D2B79F5 | 0
    let t = Math.imul(seed ^ seed >>> 15, 1 | seed)
    t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t
    return ((t ^ t >>> 14) >>> 0) / 4294967296
  }
}

// 生成建筑层数据（点击后常驻，不会重新生成）
function genBuildings() {
  const configs = [
    { count: 8,  maxH: 160, minH: 40,  op: 0.35, colors: ['#0A1E3D', '#0E3265'] },
    { count: 10, maxH: 230, minH: 55,  op: 0.22, colors: ['#0E3265', '#1a3a5a'] },
    { count: 12, maxH: 310, minH: 70,  op: 0.15, colors: ['#152d4a', '#2d5a8a'] },
    { count: 14, maxH: 400, minH: 85,  op: 0.09, colors: ['#1a3a5a', '#4a6a8a'] },
  ]
  return configs.map((cfg) => {
    const rng = mulberry32(42 + cfg.count)
    return Array.from({ length: cfg.count }, () => {
      const h = cfg.minH + rng() * (cfg.maxH - cfg.minH)
      return {
        left: `${2 + rng() * 93}%`,
        width: `${3 + rng() * 6}%`,
        height: `${h}px`,
        opacity: cfg.op * (0.7 + rng() * 0.3),
        background: `linear-gradient(180deg, ${cfg.colors[0]} 0%, ${cfg.colors[1]} 100%)`,
        borderRadius: `${2 + rng() * 6}px ${2 + rng() * 10}px 0 0`,
      }
    })
  })
}
const buildingLayers = genBuildings()

// 雾粒子
function genParticles() {
  const rng = mulberry32(99)
  return Array.from({ length: 18 }, () => ({
    left: `${rng() * 100}%`,
    top: `${15 + rng() * 70}%`,
    width: `${10 + rng() * 24}px`,
    height: `${6 + rng() * 14}px`,
    delay: `${rng() * 8}s`,
    duration: `${12 + rng() * 12}s`,
  }))
}
const particles = genParticles()

/* ====== 分页 ====== */
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

/* ====== 详情面板 ====== */
const detailPanelOpen = ref(false)
const selectedJob = ref(null)

function openDetail(job) {
  selectedJob.value = job
  detailPanelOpen.value = true
}

/* ====== 类别 ====== */
const categories = [
  { key: 'all', label: '全部岗位' },
  { key: 'school', label: '校招' },
  { key: 'social', label: '社招' },
  { key: 'state', label: '国企' },
  { key: 'intern', label: '实习' },
]

/* ====== 地区选项 ====== */
const locationOptions = ['渝北区', '渝中区', '两江新区', '南岸区', '九龙坡区', '沙坪坝区']

/* ====== 岗位数据（从后端获取） ====== */
const rawJobs = ref<any[]>([])
const typeMap = { school: '校招', social: '社招', intern: '实习', state: '国企' }

// 从后端加载数据
async function loadJobs() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (query.value.trim()) params.title = query.value.trim()
    if (filterLocation.value) params.location = filterLocation.value
    if (filterEducation.value) params.education = filterEducation.value
    if (filterSource.value === 'official') params.sourceType = 0
    else if (filterSource.value === 'commercial') params.sourceType = 1

    const res: Record<string, any> = await listJobs(params)
    rawJobs.value = (res.rows || [])
    total.value = res.total || 0
  } catch (e) {
    console.error('加载岗位数据失败', e)
    rawJobs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 筛选后的数据（客户端二次筛选：类别、关键词、匹配度等）
const filtered = computed(() => {
  let list = rawJobs.value

  // 类别筛选（基于 experience 字段关键词匹配）
  if (activeCat.value !== 'all') {
    const catLabel = typeMap[activeCat.value]
    if (catLabel === '校招') {
      list = list.filter(j => (j.experience || '').includes('应届') || (j.experience || '').includes('校招'))
    } else if (catLabel === '社招') {
      list = list.filter(j => !(j.experience || '').includes('应届') && !(j.experience || '').includes('校招'))
    } else if (catLabel === '实习') {
      list = list.filter(j => (j.experience || '').includes('实习'))
    } else if (catLabel === '国企') {
      // 国企：公司名或来源名包含关键词（客户端粗略筛选）
      list = list.filter(j => (j.company || '').includes('集团') || (j.company || '').includes('国有') || (j.source || '').includes('官方'))
    }
  }

  // 关键词搜索（补充前端搜索，弥补服务端LIKE的不足）
  if (query.value.trim()) {
    const q = query.value.trim().toLowerCase()
    const tags = j => {
      try { return JSON.parse(j.tags || '[]') } catch { return [] }
    }
    list = list.filter(j =>
      (j.company || '').toLowerCase().includes(q) ||
      (j.title || '').toLowerCase().includes(q) ||
      tags(j).some(t => t.toLowerCase().includes(q))
    )
  }

  return list
})

const displayed = computed(() => filtered.value)

// 筛选变化时重新请求后端（回到第1页）
let debounceTimer
watch([query, filterLocation, filterEducation, filterSource], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    pageNum.value = 1
    loadJobs()
  }, 300)
}, { deep: true })

/* ====== 分页事件 ====== */
function handlePageChange(page) {
  pageNum.value = page
  loadJobs()
}
function handleSizeChange(size) {
  pageSize.value = size
  pageNum.value = 1
  loadJobs()
}

onMounted(() => {
  loadJobs()
})

/* ====== 工具函数 ====== */
function onFilterChange() {
  // 由 watch 自动触发 loadJobs
}

function displaySalary(job) {
  if (job.salary) return job.salary
  if (job.salaryMin && job.salaryMax) {
    const min = job.salaryMin >= 10000 ? (job.salaryMin / 10000).toFixed(0) + '万' : job.salaryMin
    const max = job.salaryMax >= 10000 ? (job.salaryMax / 10000).toFixed(0) + '万' : job.salaryMax
    return `${min}-${max}`
  }
  return '薪资面议'
}

function sourceBadgeClass(job) {
  if (job.sourceType === 0) return 'badge-official'
  if (job.sourceType === 1) return 'badge-commercial'
  return 'badge-other'
}

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try { return JSON.parse(tags) } catch { return [] }
}
</script>

<style scoped lang="scss">
@import '@/components/ShuimoUI/styles/variables.scss';
@import '@/components/ShuimoUI/styles/mixins.scss';

/* ==================== 页面容器 ==================== */
.jc-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px 40px;
  min-height: 100vh;
}

/* ==================== HEADER (不变) ==================== */
.jc-header {
  position: relative;
  text-align: center;
  padding: 80px 20px 50px;
  overflow: hidden;
  min-height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.jc-landscape {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.jc-mtn {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 50% 50% 0 0;

  &--far { width: 140%; height: 260px; background: $ink-pale; opacity: 0.15; filter: blur(40px); }
  &--mid { width: 110%; height: 180px; background: $ink-light; opacity: 0.1; filter: blur(28px); }
  &--near { width: 80%; height: 100px; background: $ink-mid; opacity: 0.06; filter: blur(16px); }
}

.jc-mist-1, .jc-mist-2 {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  opacity: 0.06;
  animation: jc-mist 22s ease-in-out infinite;
}
.jc-mist-1 { width: 450px; height: 100px; background: $ink-pale; top: 35%; left: -5%; }
.jc-mist-2 { width: 350px; height: 80px; background: $ink-light; top: 50%; right: -5%; animation-delay: -11s; }

@keyframes jc-mist {
  0%, 100% { transform: translateX(0) scale(1); opacity: 0.06; }
  50% { transform: translateX(50px) scale(1.1); opacity: 0.1; }
}

.jc-header-content { position: relative; z-index: 1; }

.jc-title {
  display: flex; justify-content: center; gap: 6px; margin-bottom: 16px;
  .jc-t-char {
    font-size: 52px; font-weight: 700; color: $ink-deep; font-family: $font-family;
    letter-spacing: 6px;
    opacity: 0; transform: translateY(-30px) scale(1.15); filter: blur(4px);
    animation: jc-char-in 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
    &:nth-child(2) { color: $accent-red; }
  }
}

@keyframes jc-char-in {
  to { opacity: 1; transform: translateY(0) scale(1); filter: blur(0); }
}

.jc-subtitle {
  font-size: 17px; color: $ink-light; font-family: $font-family; letter-spacing: 6px;
  margin-bottom: 12px;
  opacity: 0; animation: jc-fade-in 0.6s ease 0.6s forwards;
}
.jc-desc {
  font-size: 14px; color: $ink-pale; letter-spacing: 2px;
  opacity: 0; animation: jc-fade-in 0.6s ease 0.8s forwards;
}
@keyframes jc-fade-in { to { opacity: 1; } }

/* ==================== 分类标签 ==================== */
.jc-filter-bar {
  display: flex; justify-content: center; gap: 12px; margin-bottom: 24px; flex-wrap: wrap;
}

.filter-tag {
  padding: 8px 22px; border: 1px solid $border-mid; border-radius: 20px;
  background: transparent; color: $ink-mid; font-size: 14px; font-family: $font-family;
  letter-spacing: 1px; cursor: pointer; transition: all $transition-base;

  &:hover { border-color: $ink-primary; color: $ink-primary; }

  &.active {
    background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
    border-color: $ink-primary; color: $paper-white; font-weight: 500;
    box-shadow: 0 2px 8px rgba(14, 50, 101, 0.25);
  }
}

/* ==================== 搜索栏 ==================== */
.jc-search {
  display: flex; align-items: center; gap: 16px; margin-bottom: 16px; flex-wrap: wrap;
}

.jc-search-input {
  flex: 1; position: relative; min-width: 220px;

  .s-icon {
    position: absolute; left: 14px; top: 50%; transform: translateY(-50%);
    color: $ink-pale; font-size: 18px; pointer-events: none;
  }

  .s-input {
    width: 100%; padding: 11px 38px 11px 44px;
    border: 1px solid $border-light; border-radius: 10px;
    font-size: 14px; font-family: $font-family; color: $ink-deep;
    background: $paper-white; outline: none;
    transition: border-color $transition-base, box-shadow $transition-base;

    &::placeholder { color: $ink-pale; }

    &:focus {
      border-color: $ink-primary;
      box-shadow: 0 0 0 3px rgba(14, 50, 101, 0.08);
    }
  }

  .s-clear {
    position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
    color: $ink-pale; cursor: pointer; display: flex; align-items: center;
    transition: color $transition-fast;
    &:hover { color: $ink-mid; }
  }
}

.jc-search-stats {
  font-size: 13px; color: $ink-pale; white-space: nowrap;
  strong { color: $ink-mid; font-weight: 600; }
}

/* ==================== 多维度筛选栏 ==================== */
.jc-filters-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 28px;
  padding: 16px 20px;
  background: rgba(14, 50, 101, 0.02);
  border: 1px solid rgba(14, 50, 101, 0.06);
  border-radius: 12px;
  flex-wrap: wrap;
}

.fs-label {
  font-size: 12px;
  color: $ink-pale;
  font-family: $font-family;
  margin-bottom: 6px;
  display: block;
}

/* ---- 下拉选择框 ---- */
.filter-select {
  select {
    padding: 7px 28px 7px 12px;
    border: 1px solid $border-light;
    border-radius: 6px;
    background: $paper-white;
    font-size: 13px;
    font-family: $font-family;
    color: $ink-mid;
    cursor: pointer;
    outline: none;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%238aa0b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 8px center;
    transition: border-color $transition-fast;

    &:focus {
      border-color: $ink-primary;
    }
  }
}

/* ---- 来源筛选 ---- */
.filter-source {
  .source-tags {
    display: flex;
    gap: 6px;
  }

  .source-tag {
    padding: 5px 14px;
    border-radius: 6px;
    font-size: 13px;
    font-family: $font-family;
    cursor: pointer;
    transition: all $transition-base;
    display: flex;
    align-items: center;
    gap: 4px;

    // 默认（全部）
    border: 1px solid $border-light;
    background: transparent;
    color: $ink-mid;

    &:hover { border-color: $ink-primary; color: $ink-primary; }

    &.active {
      background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
      border-color: $ink-primary; color: $paper-white;
    }

    // 官方 — 印章风格
    &--official {
      border-color: rgba(196, 92, 72, 0.25);
      color: $accent-red;

      &:hover { border-color: $accent-red; background: rgba(196, 92, 72, 0.04); }

      &.active {
        background: $accent-red;
        border-color: $accent-red;
        color: white;
        transform: rotate(-1deg);
        box-shadow: 0 2px 6px rgba(196, 92, 72, 0.2);
      }
    }

    // 商业 — 水墨线框
    &--commercial {
      border-style: dashed;
      border-color: rgba(14, 50, 101, 0.15);
      color: $ink-light;

      &:hover { border-color: $ink-pale; background: rgba(14, 50, 101, 0.03); }

      &.active {
        border-style: solid;
        border-color: $ink-light;
        background: rgba(14, 50, 101, 0.06);
        color: $ink-deep;
      }
    }
  }
}

/* ---- 墨滴开关 ---- */
.filter-toggle {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ink-toggle {
  position: relative;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.ink-drop {
  width: 14px;
  height: 18px;
  border-radius: 50% 50% 50% 50% / 40% 40% 60% 60%;
  background: $ink-pale;
  position: relative;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  // 水滴尖
  &::before {
    content: '';
    position: absolute;
    top: -6px;
    left: 50%;
    transform: translateX(-50%);
    width: 6px;
    height: 8px;
    background: inherit;
    border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  }
}

.ink-wave {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  border: 2px solid rgba(14, 50, 101, 0.15);
  animation: wave-pulse 2s ease-out infinite;
}

.ink-toggle.on {
  .ink-drop {
    background: $ink-primary;
    width: 18px;
    height: 22px;
    box-shadow: 0 0 12px rgba(14, 50, 101, 0.3);
  }

  .ink-wave {
    border-color: rgba(14, 50, 101, 0.2);
  }
}

@keyframes wave-pulse {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.5); opacity: 0; }
}

/* ==================== 岗位列表 ==================== */
.jc-list {
  display: flex; flex-direction: column; gap: 18px; padding-bottom: 40px;
}

/* ---- 单岗位卡片 ---- */
.job-card {
  position: relative;
  background: $paper-white;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 24px 28px 20px;
  box-shadow: 0 2px 8px $shadow-soft;
  cursor: pointer;
  transition: all $transition-base;
  overflow: visible;

  .jc-ink-bottom {
    position: absolute; bottom: -50px; left: 50%;
    transform: translateX(-50%) scale(0.5);
    width: 240px; height: 240px; border-radius: 50%;
    background: radial-gradient(circle, rgba(14, 50, 101, 0.05) 0%, transparent 70%);
    opacity: 0; transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1); pointer-events: none;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px $shadow-mid;
    border-color: $border-mid;

    .jc-ink-bottom { opacity: 1; transform: translateX(-50%) scale(1); }
  }

  &--highlight { border-left: 3px solid $accent-red; }
}

/* ---- 来源标识 ---- */
.job-source-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 3px;
  letter-spacing: 1px;
  font-family: $font-family;
  z-index: 1;

  &.badge-official {
    border: 1.5px solid $accent-red;
    color: $accent-red;
    background: rgba(196, 92, 72, 0.05);
    transform: rotate(2deg);
    font-weight: 600;
  }

  &.badge-commercial {
    border: 1px dashed rgba(14, 50, 101, 0.2);
    color: $ink-light;
    background: transparent;
  }

  &.badge-other {
    border: 1px solid $border-light;
    color: $ink-pale;
    background: transparent;
  }
}

/* ---- 卡片头部 ---- */
.job-header {
  display: flex; align-items: center; gap: 16px; margin-bottom: 14px; padding-right: 80px;
}

.job-logo {
  width: 44px; height: 44px; border-radius: 8px;
  background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
  color: $paper-white; display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; font-family: $font-family; flex-shrink: 0;
}

.job-main { flex: 1; min-width: 0; }

.job-title {
  font-size: 18px; font-weight: 600; color: $ink-deep; font-family: $font-family;
  letter-spacing: 1px; margin-bottom: 3px;
}

.job-company { font-size: 13px; color: $ink-light; }

.job-seal {
  @include seal($accent-red, 26px);
  flex-shrink: 0;
  position: absolute;
  right: 80px;
  top: 50%;
  transform: translateY(-50%) rotate(-3deg);
}

/* ---- 元信息 ---- */
.job-meta {
  display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap;
}

.jm-item { font-size: 14px; color: $ink-mid; }

.jm-salary { font-weight: 600; color: $accent-red; }

.jm-dot {
  width: 3px; height: 3px; border-radius: 50%; background: $ink-pale; flex-shrink: 0;
}

/* ---- 技能标签 ---- */
.job-tags {
  display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap;
}

.tag {
  padding: 3px 12px; background: rgba(14, 50, 101, 0.06);
  border: 1px solid rgba(14, 50, 101, 0.1); border-radius: 4px;
  font-size: 12px; color: $ink-light; letter-spacing: 0.5px;
  transition: all $transition-fast;

  &:hover { background: rgba(14, 50, 101, 0.1); border-color: rgba(14, 50, 101, 0.2); }
}


/* ---- 加载状态 ---- */
.jc-loading {
  text-align: center; padding: 80px 20px;
  .loading-brush {
    width: 80px; height: 4px; margin: 0 auto 20px;
    background: linear-gradient(90deg, transparent 0%, $ink-primary 30%, $ink-primary 70%, transparent 100%);
    border-radius: 2px; opacity: 0.5;
    animation: loading-sweep 1.6s ease-in-out infinite;
  }
  .loading-text { font-size: 14px; color: $ink-pale; font-family: $font-family; letter-spacing: 2px; }
}

@keyframes loading-sweep {
  0% { transform: scaleX(0.3); opacity: 0.3; }
  50% { transform: scaleX(1); opacity: 0.7; }
  100% { transform: scaleX(0.3); opacity: 0.3; }
}

/* ---- 空状态 ---- */
.jc-empty {
  text-align: center; padding: 80px 20px;

  .empty-brush {
    width: 60px; height: 4px; margin: 0 auto 20px;
    background: linear-gradient(90deg, transparent 0%, $ink-pale 50%, transparent 100%);
    border-radius: 2px; opacity: 0.4;
  }

  .empty-text { font-size: 16px; color: $ink-light; font-family: $font-family; letter-spacing: 3px; margin-bottom: 8px; }
  .empty-hint { font-size: 13px; color: $ink-pale; }
}

/* ==================== 分页 ==================== */
.jc-pagination {
  display: flex;
  justify-content: center;
  padding: 30px 20px;
}

/* ==================== FOOTER ==================== */
.jc-footer {
  text-align: center; padding: 30px 20px; display: flex;
  align-items: center; justify-content: center; gap: 16px;
}

.jc-footer-seal { @include seal($accent-red, 24px); font-size: 11px; }
.jc-footer-text { font-size: 12px; color: $ink-pale; font-family: $font-family; letter-spacing: 4px; opacity: 0.6; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .jc-title .jc-t-char { font-size: 36px; }
  .jc-subtitle { font-size: 14px; letter-spacing: 4px; }

  .job-card { padding: 18px; }
  .job-header { padding-right: 0; flex-wrap: wrap; }
  .job-seal { position: static; transform: rotate(-3deg); }

  .jc-filters-row { flex-direction: column; gap: 12px; }

  .filter-source .source-tags { flex-wrap: wrap; }

  .jm-btn { width: 100%; text-align: center; }
}

/* ===================== 山城压迫感特效 — 水墨版 ===================== */

/* --- 触发 — 似笔触落纸 --- */
.jc-header.mc-active {
  animation: mc-stroke 0.9s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards,
             mc-breathe 5s ease-in-out infinite 1.2s;
}

@keyframes mc-stroke {
  0%   { transform: translateY(0); filter: brightness(1); }
  30%  { transform: translateY(6px) scale(0.98); filter: brightness(0.7); }
  70%  { transform: translateY(-1px) scale(1.005); filter: brightness(0.9); }
  100% { transform: translateY(0) scale(1); filter: brightness(1); }
}

@keyframes mc-breathe {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.012); }
}

/* --- 墨色晕染覆盖层 --- */
.mc-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(10, 30, 61, 0.08) 15%,
    rgba(10, 30, 61, 0.25) 35%,
    rgba(10, 30, 61, 0.5) 55%,
    rgba(10, 30, 61, 0.8) 80%,
    rgba(10, 30, 61, 0.95) 100%
  );
  pointer-events: none;
  animation: mc-wash-in 1s ease forwards;
  z-index: 2;
}

@keyframes mc-wash-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

/* --- 建筑/山城剪影 --- */
.mc-buildings {
  position: absolute; inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 3;
}

.mc-b-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  transform: translateY(100%);
  animation: mc-rise 1s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

@keyframes mc-rise {
  to { transform: translateY(0); }
}

.mc-b {
  position: absolute;
  bottom: 0;
  filter: blur(0.5px);           /* 墨渗纸感 */
  transform-origin: bottom center;
  animation: mc-b-skew 1.2s ease-out forwards;
}

@keyframes mc-b-skew {
  0%   { transform: skewY(0deg) scaleY(0.4); opacity: 0; filter: blur(2px); }
  50%  { opacity: var(--b-op, 0.3); filter: blur(0.8px); }
  100% { transform: skewY(0deg) scaleY(1); opacity: var(--b-op, 0.3); filter: blur(0.3px); }
}

/* --- 墨韵底层光晕 --- */
.mc-ink-glow {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 100px;
  background: linear-gradient(0deg,
    $ink-deep 0%,
    $ink-mid 25%,
    rgba(14, 50, 101, 0.3) 50%,
    transparent 100%
  );
  filter: blur(12px);
  pointer-events: none;
  z-index: 1;
  animation: mc-wash-in 0.8s ease 0.3s forwards;
  opacity: 0;
}

/* --- 朱砂印落款 --- */
.mc-ink-seal {
  position: absolute;
  bottom: 20px;
  right: 30px;
  width: 32px;
  height: 32px;
  border: 2px solid $accent-red;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $accent-red;
  font-size: 10px;
  font-family: $font-family;
  letter-spacing: 1px;
  writing-mode: vertical-rl;
  opacity: 0;
  z-index: 5;
  pointer-events: none;
  animation: mc-seal-in 0.5s ease 0.7s forwards;
  box-shadow: 0 0 6px rgba(196, 92, 72, 0.15);
  transform: rotate(-3deg);

  &::after {
    content: '山城';
  }
}

@keyframes mc-seal-in {
  from { opacity: 0; transform: rotate(-6deg) scale(0.8); }
  to   { opacity: 0.7; transform: rotate(-3deg) scale(1); }
}

/* --- 墨雾 --- */
.mc-particles {
  position: absolute; inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 4;
}

.mc-p {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(138, 160, 184, 0.12), transparent);
  filter: blur(12px);
  animation: mc-p-drift linear infinite;
  opacity: 0;
}

@keyframes mc-p-drift {
  0%   { transform: translateY(0) translateX(0); opacity: 0; }
  15%  { opacity: 0.5; }
  75%  { opacity: 0.3; }
  100% { transform: translateY(-240px) translateX(30px); opacity: 0; }
}
</style>
