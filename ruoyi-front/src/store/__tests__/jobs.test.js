import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useJobsStore } from '../jobs'

vi.mock('@/api/jiangcheng', () => ({
  listJobs: vi.fn(),
}))

describe('jobs store', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useJobsStore()
    vi.clearAllMocks()
  })

  it('should start with default state', () => {
    expect(store.matchResults).toEqual([])
    expect(store.allJobs).toEqual([])
    expect(store.loading).toBe(false)
  })

  describe('setMatchResults', () => {
    it('should set match results', () => {
      const results = [
        { score: 85, job: { id: 1, title: 'FE', company: 'A', salaryMin: 10, salaryMax: 20, location: 'BJ', tags: [], education: '本科', experience: '1-3' } }
      ]
      store.setMatchResults(results)
      expect(store.matchResults).toEqual(results)
    })
  })

  describe('displayedJobs', () => {
    const mockJobs = [
      { id: 1, title: 'FE', company: 'A', salaryMin: 10, salaryMax: 20, location: 'BJ', tags: [], education: '本科', experience: '1-3' },
      { id: 2, title: 'BE', company: 'B', salaryMin: 15, salaryMax: 25, location: 'SH', tags: [], education: '本科', experience: '3-5' },
      { id: 3, title: 'FS', company: 'C', salaryMin: 20, salaryMax: 30, location: 'SZ', tags: [], education: '硕士', experience: '5+' },
      { id: 4, title: 'DevOps', company: 'D', salaryMin: 12, salaryMax: 22, location: 'BJ', tags: [], education: '本科', experience: '1-3' },
      { id: 5, title: 'QA', company: 'E', salaryMin: 8, salaryMax: 15, location: 'CD', tags: [], education: '大专', experience: '1-3' },
      { id: 6, title: 'PM', company: 'F', salaryMin: 18, salaryMax: 28, location: 'BJ', tags: [], education: '本科', experience: '3-5' },
      { id: 7, title: 'Designer', company: 'G', salaryMin: 14, salaryMax: 24, location: 'SH', tags: [], education: '本科', experience: '1-3' },
    ]

    it('should return match results when available', () => {
      const match = [{ score: 90, job: mockJobs[0] }]
      store.setMatchResults(match)
      expect(store.displayedJobs).toEqual(match)
    })

    it('should fallback to first 6 jobs when no match results', () => {
      store.allJobs = mockJobs
      expect(store.displayedJobs.length).toBe(6)
      expect(store.displayedJobs[0].job.id).toBe(1)
      expect(store.displayedJobs[5].job.id).toBe(6)
    })

    it('should return empty array when no jobs and no matches', () => {
      expect(store.displayedJobs).toEqual([])
    })
  })

  describe('fetchAllJobs', () => {
    it('should populate allJobs on success', async () => {
      const { listJobs } = await import('@/api/jiangcheng')
      const jobData = [{ id: 1, title: 'FE' }]
      listJobs.mockResolvedValue({ rows: jobData })

      await store.fetchAllJobs()
      expect(store.allJobs).toEqual(jobData)
      expect(store.loading).toBe(false)
    })

    it('should handle empty response', async () => {
      const { listJobs } = await import('@/api/jiangcheng')
      listJobs.mockResolvedValue({ rows: [] })

      await store.fetchAllJobs()
      expect(store.allJobs).toEqual([])
    })

    it('should set loading to false on error', async () => {
      const { listJobs } = await import('@/api/jiangcheng')
      listJobs.mockRejectedValue(new Error('API error'))

      await store.fetchAllJobs()
      expect(store.allJobs).toEqual([])
      expect(store.loading).toBe(false)
    })

    it('should not re-fetch when already loading', async () => {
      const { listJobs } = await import('@/api/jiangcheng')
      store.loading = true

      await store.fetchAllJobs()
      expect(listJobs).not.toHaveBeenCalled()
    })
  })
})
