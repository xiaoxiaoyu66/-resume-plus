import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { listJobs } from '@/api/jiangcheng'

export interface JobItem {
  id: number
  title: string
  company: string
  salaryMin: number
  salaryMax: number
  location: string
  tags: string[]
  education: string
  experience: string
  description?: string
  requirements?: string
}

export interface MatchResult {
  score: number
  job: JobItem
}

export const useJobsStore = defineStore('jobs', () => {
  const matchResults = ref<MatchResult[]>([])
  const allJobs = ref<JobItem[]>([])
  const loading = ref(false)

  const displayedJobs = computed(() => {
    if (matchResults.value.length > 0) {
      return matchResults.value
    }
    return allJobs.value.slice(0, 6).map(job => ({ job, score: 0 }))
  })

  function setMatchResults(results: MatchResult[]) {
    matchResults.value = results
  }

  async function fetchAllJobs() {
    if (loading.value) return
    loading.value = true
    try {
      const res = await listJobs({ pageNum: 1, pageSize: 20 })
      allJobs.value = (res.rows || []) as JobItem[]
    } catch (e) {
      console.error('加载岗位列表失败', e)
      allJobs.value = []
    } finally {
      loading.value = false
    }
  }

  return {
    matchResults,
    allJobs,
    loading,
    displayedJobs,
    setMatchResults,
    fetchAllJobs
  }
})
