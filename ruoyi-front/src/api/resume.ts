import request from '@/utils/request'

export function getResumeList() {
  return request({
    url: '/resume/list',
    method: 'get'
  })
}

export function getResume(id: number) {
  return request({
    url: `/resume/${id}`,
    method: 'get'
  })
}

export function createResume(data: Record<string, unknown>) {
  return request({
    url: '/resume',
    method: 'post',
    data
  })
}

export function updateResume(data: Record<string, unknown>) {
  return request({
    url: '/resume',
    method: 'put',
    data
  })
}

export function deleteResume(id: number) {
  return request({
    url: `/resume/${id}`,
    method: 'delete'
  })
}

export function setDefaultResume(id: number) {
  return request({
    url: `/resume/default/${id}`,
    method: 'put'
  })
}

export function aiResumeAction(data: Record<string, unknown>) {
  return request({
    url: '/resume/ai',
    method: 'post',
    data,
    timeout: 120000
  })
}

export function exportResumePdf(data: Record<string, unknown>) {
  return request({
    url: '/resume/export/pdf',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 60000
  })
}

export function exportResumeWord(data: Record<string, unknown>) {
  return request({
    url: '/resume/export/word',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 30000
  })
}

export function exportResumeWordFromHtml(data: Record<string, unknown>) {
  return request({
    url: '/resume/export/word-from-html',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 60000
  })
}

export function parseResumeFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/resume/parse',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
