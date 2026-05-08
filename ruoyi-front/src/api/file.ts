import request from '@/utils/request'

interface ProgressData {
  progress: number
  speed: string
  loaded: number
  total: number
  remainingTime?: number
}

interface FileListParams {
  pageNum?: number
  pageSize?: number
  timeRange?: string
  startTime?: string
  endTime?: string
  keyword?: string
}

/**
 * 上传文件到 MinIO（支持进度和取消）
 */
export function uploadFileWithProgress(file: File, onProgress?: (data: ProgressData) => void, signal?: AbortSignal) {
  const formData = new FormData()
  formData.append('file', file)
  
  const startTime = Date.now()
  let lastLoaded = 0
  let lastTime = startTime
  
  return request({
    url: '/ai/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    signal: signal,
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        
        // 计算上传速度
        const currentTime = Date.now()
        const timeDiff = (currentTime - lastTime) / 1000 // 秒
        const loadedDiff = progressEvent.loaded - lastLoaded
        
        let speed = 0
        if (timeDiff > 0) {
          speed = loadedDiff / timeDiff // bytes/s
        }
        
        // 更新上次记录
        lastLoaded = progressEvent.loaded
        lastTime = currentTime
        
        // 格式化速度
        let speedText = ''
        if (speed > 1024 * 1024) {
          speedText = (speed / (1024 * 1024)).toFixed(2) + ' MB/s'
        } else if (speed > 1024) {
          speedText = (speed / 1024).toFixed(2) + ' KB/s'
        } else {
          speedText = Math.round(speed) + ' B/s'
        }
        
        // 计算剩余时间
        const remainingBytes = progressEvent.total - progressEvent.loaded
        const remainingTime = speed > 0 ? remainingBytes / speed : 0
        
        onProgress && onProgress({
          progress,
          speed: speedText,
          loaded: progressEvent.loaded,
          total: progressEvent.total,
          remainingTime: remainingTime > 0 ? Math.ceil(remainingTime) : 0
        })
      }
    }
  })
}

/**
 * 上传文件到 MinIO（兼容旧版本）
 * @param {File} file - 文件对象
 * @returns {Promise} - 返回上传结果
 */
export function uploadFile(file) {
  return uploadFileWithProgress(file)
}

/**
 * 批量上传文件（支持进度）
 * @param {File[]} files - 文件列表
 * @param {Function} onProgress - 进度回调函数 (fileIndex, progress, speed)
 * @param {AbortSignal} signal - 取消信号
 * @returns {Promise} - 返回上传结果
 */
export function uploadFilesBatch(files: File[], onProgress?: (data: ProgressData) => void, signal?: AbortSignal) {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  
  const startTime = Date.now()
  let lastLoaded = 0
  let lastTime = startTime
  
  return request({
    url: '/ai/file/upload/batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    signal: signal,
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        
        const currentTime = Date.now()
        const timeDiff = (currentTime - lastTime) / 1000
        const loadedDiff = progressEvent.loaded - lastLoaded
        
        let speed = 0
        if (timeDiff > 0) {
          speed = loadedDiff / timeDiff
        }
        
        lastLoaded = progressEvent.loaded
        lastTime = currentTime
        
        let speedText = ''
        if (speed > 1024 * 1024) {
          speedText = (speed / (1024 * 1024)).toFixed(2) + ' MB/s'
        } else if (speed > 1024) {
          speedText = (speed / 1024).toFixed(2) + ' KB/s'
        } else {
          speedText = Math.round(speed) + ' B/s'
        }
        
        onProgress && onProgress({
          progress,
          speed: speedText,
          loaded: progressEvent.loaded,
          total: progressEvent.total
        })
      }
    }
  })
}

/**
 * 获取文件列表（支持分页和筛选）
 * @param {Object} params - 查询参数
 * @param {number} params.pageNum - 页码
 * @param {number} params.pageSize - 每页大小
 * @param {string} params.timeRange - 时间范围：1hour, today, week, month, custom
 * @param {string} params.startTime - 自定义开始时间（ISO格式）
 * @param {string} params.endTime - 自定义结束时间（ISO格式）
 * @param {string} params.keyword - 搜索关键词
 * @returns {Promise} - 返回文件列表
 */
export function listFiles(params: FileListParams = {}) {
  const queryParams: Record<string, unknown> = {
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 10,
    timeRange: params.timeRange || '',
    keyword: params.keyword || ''
  }

  // 如果是自定义时间范围，添加起止时间参数
  if (params.timeRange === 'custom' && params.startTime && params.endTime) {
    queryParams.startTime = params.startTime
    queryParams.endTime = params.endTime
  }
  
  return request({
    url: '/ai/file/list',
    method: 'get',
    params: queryParams
  })
}

/**
 * 删除文件
 * @param {string} fileName - 文件名
 * @returns {Promise} - 返回删除结果
 */
export function deleteFile(fileName: string) {
  return request({
    url: '/ai/file/delete',
    method: 'delete',
    params: { fileName }
  })
}

/**
 * 获取文件预签名URL
 * @param {string} fileName - 文件名
 * @returns {Promise} - 返回文件URL
 */
export function getFileUrl(fileName: string) {
  return request({
    url: '/ai/file/url',
    method: 'get',
    params: { fileName }
  })
}

/**
 * 获取当前会话的文件列表
 * @param {string} sessionId - 会话ID
 * @returns {Promise} - 返回文件列表
 */
export function listSessionFiles(sessionId: string) {
  return request({
    url: '/ai/file/session',
    method: 'get',
    params: { sessionId }
  })
}

/**
 * 将临时文件持久化到 MinIO
 * @param {string} tempFileId - 临时文件ID
 * @returns {Promise} - 返回持久化后的文件信息
 */
export function persistTempFile(tempFileId: string) {
  return request({
    url: '/ai/file/persist',
    method: 'post',
    params: { tempFileId }
  })
}

/**
 * 批量将临时文件持久化到 MinIO
 * @param {string[]} tempFileIds - 临时文件ID列表
 * @returns {Promise} - 返回持久化后的文件信息列表
 */
export function persistTempFiles(tempFileIds: string[]) {
  return request({
    url: '/ai/file/persist/batch',
    method: 'post',
    params: { tempFileIds }
  })
}
