import request from '@/utils/request'

export function getProfile() {
  return request({
    url: '/ai/profile',
    method: 'get'
  })
}

export function saveProfile(data: Record<string, unknown>) {
  return request({
    url: '/ai/profile/save',
    method: 'post',
    data
  })
}

export function uploadResume(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/ai/profile/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
