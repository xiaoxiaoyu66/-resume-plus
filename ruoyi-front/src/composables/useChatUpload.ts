import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFileWithProgress } from '@/api/file'

export function useChatUpload(options) {
  const {
    hasStarted,
    uploadedFiles,
    fileInput,
    fileInputBottom,
    isUploading
  } = options

  const showUploadProgress = ref(false)
  const uploadingFileName = ref('')
  const uploadingFileSize = ref(0)
  const uploadProgress = ref(0)
  const uploadSpeed = ref('0 KB/s')
  const uploadedSize = ref(0)
  const remainingTime = ref(0)
  const abortController = ref(null)

  function getFileName(filePath) {
    if (!filePath) return ''
    const parts = filePath.split('/')
    return parts[parts.length - 1]
  }

  function handleFileFromSpace(event) {
    const file = event.detail
    if (!file) return
    const exists = uploadedFiles.value.some(f => f.fileName === file.fileName)
    if (exists) {
      ElMessage.warning('该文件已在当前会话中')
      return
    }
    uploadedFiles.value.push({
      fileName: file.fileName,
      originalName: file.originalName || getFileName(file.fileName),
      fileUrl: file.fileUrl,
      fileSize: file.fileSize,
      uploadTime: new Date().toISOString()
    })
    ElMessage.success(`已添加文件: ${file.originalName || getFileName(file.fileName)}`)
    if (!hasStarted.value) {
      hasStarted.value = true
    }
  }

  function triggerFileUpload() {
    console.log('触发文件上传 - 空状态', fileInput.value)
    if (fileInput.value) {
      fileInput.value.click()
    } else {
      console.error('fileInput ref 未找到')
      ElMessage.error('文件上传组件未初始化，请刷新页面重试')
    }
  }

  function triggerFileUploadBottom() {
    console.log('触发文件上传 - 对话状态', fileInputBottom.value)
    if (fileInputBottom.value) {
      fileInputBottom.value.click()
    } else {
      console.error('fileInputBottom ref 未找到')
      ElMessage.error('文件上传组件未初始化，请刷新页面重试')
    }
  }

  async function handleFileSelect(event) {
    const file = event.target.files[0]
    if (!file) return

    const maxSize = 5 * 1024 * 1024
    if (file.size > maxSize) {
      ElMessage.error('文件大小不能超过5MB')
      return
    }

    const allowedTypes = ['pdf', 'doc', 'docx', 'txt', 'xls', 'xlsx', 'ppt', 'pptx', 'md']
    const ext = file.name.split('.').pop().toLowerCase()
    if (!allowedTypes.includes(ext)) {
      ElMessage.error(`不支持的文件类型，仅支持: ${allowedTypes.join(', ')}`)
      return
    }

    uploadingFileName.value = file.name
    uploadingFileSize.value = file.size
    uploadProgress.value = 0
    uploadSpeed.value = '0 KB/s'
    uploadedSize.value = 0
    remainingTime.value = 0
    showUploadProgress.value = true
    isUploading.value = true
    abortController.value = new AbortController()

    try {
      const res = await uploadFileWithProgress(
        file,
        (progressData) => {
          uploadProgress.value = progressData.progress
          uploadSpeed.value = progressData.speed
          uploadedSize.value = progressData.loaded
          remainingTime.value = progressData.remainingTime
        },
        abortController.value.signal
      ) as any

      if (res.code === 200) {
        uploadedFiles.value.push({
          fileName: res.data.tempFileId,
          originalName: res.data.originalName || file.name,
          fileSize: res.data.fileSize,
          contentType: res.data.contentType,
          isTemp: res.data.isTemp,
          uploadTime: new Date().toISOString()
        })
        ElMessage.success(`文件 "${res.data.originalName || file.name}" 上传成功`)
        if (!hasStarted.value) {
          hasStarted.value = true
        }
      } else {
        ElMessage.error(res.msg || '上传失败')
      }
    } catch (e) {
      if (e.name === 'AbortError') {
        ElMessage.info('上传已取消')
      } else {
        console.error('上传失败', e)
        ElMessage.error('上传失败: ' + (e.message || '未知错误'))
      }
    } finally {
      isUploading.value = false
      showUploadProgress.value = false
      abortController.value = null
      event.target.value = ''
    }
  }

  function cancelUpload() {
    if (abortController.value) {
      abortController.value.abort()
    }
  }

  function removeFile(index) {
    uploadedFiles.value.splice(index, 1)
  }

  return {
    showUploadProgress,
    uploadingFileName,
    uploadingFileSize,
    uploadProgress,
    uploadSpeed,
    uploadedSize,
    remainingTime,
    handleFileFromSpace,
    triggerFileUpload,
    triggerFileUploadBottom,
    handleFileSelect,
    cancelUpload,
    removeFile
  }
}

