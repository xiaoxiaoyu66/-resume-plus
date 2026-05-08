import request from '@/utils/request'

export function listJobs(params?: Record<string, unknown>) {
  return request({
    url: '/ai/jobs/list',
    method: 'get',
    params
  })
}

export function getJob(id: number | string) {
  return request({
    url: `/ai/jobs/${id}`,
    method: 'get'
  })
}

export function addJob(data: Record<string, unknown>) {
  return request({
    url: '/ai/jobs',
    method: 'post',
    data
  })
}

export function updateJob(id: number | string, data: Record<string, unknown>) {
  return request({
    url: `/ai/jobs/${id}`,
    method: 'put',
    data
  })
}

export function delJob(id: number | string) {
  return request({
    url: `/ai/jobs/${id}`,
    method: 'delete'
  })
}

export function importJobs(data: Record<string, unknown>) {
  return request({
    url: '/ai/jobs/import',
    method: 'post',
    data
  })
}

export function analyzeJob(id: number | string) {
  return request({
    url: `/ai/jobs/${id}/analyze`,
    method: 'post'
  })
}

export function getVectorScores(profileText: string) {
  return request({
    url: '/ai/jobs/vector-scores',
    method: 'post',
    data: { profileText }
  })
}
