<template>
  <div class="resume-list-page">
    <div class="page-header">
      <h2>我的简历</h2>
      <el-button type="primary" @click="createNewResume">新建简历</el-button>
    </div>

    <el-empty v-if="!loading && resumeList.length === 0" description="暂无简历，点击上方新建">
      <el-button type="primary" @click="createNewResume">新建简历</el-button>
    </el-empty>

    <div v-else class="resume-cards">
      <el-card
        v-for="resume in resumeList"
        :key="resume.id"
        class="resume-card"
        shadow="hover"
        @click="editResume(resume.id)"
      >
        <div class="card-content">
          <div class="card-left">
            <h3 class="resume-title">{{ resume.title || '未命名简历' }}</h3>
            <p class="resume-meta">
              <el-tag size="small" :type="getTemplateType(resume.templateId)">
                {{ getTemplateLabel(resume.templateId) }}
              </el-tag>
              <el-tag v-if="resume.isDefault" size="small" type="success">默认</el-tag>
              <span class="update-time">更新于 {{ formatTime(resume.updateTime) }}</span>
            </p>
          </div>
          <div class="card-actions" @click.stop>
            <el-button type="primary" link size="small" @click="editResume(resume.id)">编辑</el-button>
            <el-button type="success" link size="small" @click="quickExport(resume.id)">导出</el-button>
            <el-button type="danger" link size="small" @click="confirmDelete(resume)">删除</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getResumeList, deleteResume } from '@/api/resume'

const router = useRouter()
const resumeList = ref<any[]>([])
const loading = ref(false)

onMounted(() => {
  loadResumes()
})

const loadResumes = async () => {
  loading.value = true
  try {
    const res = await getResumeList() as any
    resumeList.value = res.data || []
  } catch (error) {
    console.error('加载简历列表失败:', error)
  } finally {
    loading.value = false
  }
}

const createNewResume = () => {
  router.push('/resume/edit/new')
}

const editResume = (id: number) => {
  router.push(`/resume/edit/${id}`)
}

const quickExport = (id: number) => {
  router.push(`/resume/edit/${id}?export=true`)
}

const confirmDelete = (resume: any) => {
  deleteResume(resume.id).then(() => {
    loadResumes()
  }).catch(() => {})
}

const getTemplateLabel = (id: string) => {
  const map: Record<string, string> = { modern: '现代', classic: '经典', minimal: '极简' }
  return map[id] || id
}

const getTemplateType = (id: string) => {
  const map: Record<string, string> = { modern: 'primary', classic: 'info', minimal: 'info' }
  return map[id] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.resume-list-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
}

.resume-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resume-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.resume-card:hover {
  transform: translateY(-2px);
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.resume-title {
  margin: 0 0 8px;
  font-size: 16px;
}

.resume-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.update-time {
  font-size: 12px;
  color: #999;
}
</style>
