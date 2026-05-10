import request from '@/utils/request'

export interface SchoolItem {
  id: number
  name: string
  city: string
  province: string
  level: string
}

/**
 * 搜索学校（带 localStorage 缓存，有效期 1 小时）
 */
export async function searchSchools(keyword: string, limit = 5): Promise<SchoolItem[]> {
  const cacheKey = `school_cache:${keyword}_${limit}`
  const cached = getCache(cacheKey)
  if (cached) return cached

  try {
    const res = await request({
      url: '/ai/schools/search',
      method: 'get',
      params: { keyword, limit }
    }) as any
    const list = (res.data || []) as SchoolItem[]
    setCache(cacheKey, list)
    return list
  } catch (e) {
    console.error('搜索学校失败', e)
    return []
  }
}

// ---- localStorage 缓存工具 ----

const CACHE_PREFIX = 'school_cache:'
const CACHE_TTL = 60 * 60 * 1000 // 1 小时

function getCache(key: string): SchoolItem[] | null {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const { data, time } = JSON.parse(raw)
    if (Date.now() - time > CACHE_TTL) {
      localStorage.removeItem(key)
      return null
    }
    return data
  } catch {
    return null
  }
}

function setCache(key: string, data: SchoolItem[]) {
  try {
    localStorage.setItem(key, JSON.stringify({ data, time: Date.now() }))
  } catch {
    // localStorage 满时忽略
  }
}
