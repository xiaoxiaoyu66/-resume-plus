export interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  time: string
}

export interface ChatSessionItem {
  id: number
  sessionTitle: string
  scene: string
  updateTime: string
  createTime: string
}

export interface UploadedFile {
  fileName: string
  originalName: string
  fileUrl?: string
  fileSize?: number
  contentType?: string
  isTemp?: boolean
  uploadTime?: string
}
