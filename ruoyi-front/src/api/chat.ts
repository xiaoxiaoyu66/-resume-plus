import request from '@/utils/request'

export function chat(data: Record<string, unknown>) {
  return request({
    url: '/ai/chat/ask',
    method: 'post',
    data,
    timeout: 120000
  })
}

export function listHistory() {
  return request({
    url: '/ai/chat/history',
    method: 'get'
  })
}

export function getSessionDetail(sessionId: number | string) {
  return request({
    url: '/ai/chat/session/' + sessionId,
    method: 'get'
  })
}

export function delSession(sessionId: number | string) {
  return request({
    url: '/ai/chat/session/' + sessionId,
    method: 'delete'
  })
}

export function getQuota() {
  return request({
    url: '/ai/chat/quota',
    method: 'get'
  })
}

export function updateSessionScene(sessionId: number | string, scene: string) {
  return request({
    url: '/ai/chat/session/' + sessionId + '/scene',
    method: 'put',
    data: { scene }
  })
}
