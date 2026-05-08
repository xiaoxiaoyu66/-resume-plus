import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useChatUpload } from '../useChatUpload'

vi.mock('@/api/file', () => ({
  uploadFileWithProgress: vi.fn(),
}))

describe('useChatUpload', () => {
  let hasStarted, uploadedFiles, fileInput, fileInputBottom, isUploading
  let upload

  beforeEach(() => {
    vi.clearAllMocks()
    hasStarted = ref(false)
    uploadedFiles = ref([])
    fileInput = ref({ click: vi.fn() })
    fileInputBottom = ref({ click: vi.fn() })
    isUploading = ref(false)

    upload = useChatUpload({
      hasStarted,
      uploadedFiles,
      fileInput,
      fileInputBottom,
      isUploading,
    })
  })

  describe('removeFile', () => {
    it('should remove file by index', () => {
      uploadedFiles.value = [
        { fileName: 'a.pdf', originalName: 'a.pdf' },
        { fileName: 'b.pdf', originalName: 'b.pdf' },
      ]
      upload.removeFile(0)
      expect(uploadedFiles.value.length).toBe(1)
      expect(uploadedFiles.value[0].fileName).toBe('b.pdf')
    })
  })

  describe('handleFileFromSpace', () => {
    it('should add file from custom event', () => {
      const file = { fileName: 'test.pdf', originalName: 'Test File.pdf', fileUrl: '/uploads/test.pdf', fileSize: 1024 }
      upload.handleFileFromSpace({ detail: file })

      expect(uploadedFiles.value.length).toBe(1)
      expect(uploadedFiles.value[0].fileName).toBe('test.pdf')
      expect(uploadedFiles.value[0].originalName).toBe('Test File.pdf')
    })

    it('should reject duplicate files', () => {
      uploadedFiles.value = [{ fileName: 'test.pdf', originalName: 'Test File.pdf' }]
      upload.handleFileFromSpace({ detail: { fileName: 'test.pdf', originalName: 'Test File.pdf' } })

      expect(uploadedFiles.value.length).toBe(1)
    })

    it('should set hasStarted when adding first file', () => {
      upload.handleFileFromSpace({ detail: { fileName: 'doc.pdf' } })
      expect(hasStarted.value).toBe(true)
    })

    it('should skip when event has no detail', () => {
      upload.handleFileFromSpace({})
      expect(uploadedFiles.value.length).toBe(0)
    })
  })

  describe('triggerFileUpload', () => {
    it('should click the file input', () => {
      upload.triggerFileUpload()
      expect(fileInput.value.click).toHaveBeenCalled()
    })

    it('should click the bottom file input', () => {
      upload.triggerFileUploadBottom()
      expect(fileInputBottom.value.click).toHaveBeenCalled()
    })
  })

  describe('handleFileSelect', () => {
    function createFileEvent(name, size) {
      return {
        target: {
          files: [new File(['test'], name, { type: 'application/pdf' })],
          value: '',
        },
      }
    }

    it('should reject unsupported file types', async () => {
      const event = createFileEvent('test.exe', 1024)
      Object.defineProperty(event.target.files[0], 'name', { value: 'test.exe' })

      await upload.handleFileSelect(event)
      expect(uploadedFiles.value.length).toBe(0)
      expect(isUploading.value).toBe(false)
    })

    it('should reject files over 5MB', async () => {
      const event = createFileEvent('test.pdf', 6 * 1024 * 1024)

      await upload.handleFileSelect(event)
      expect(uploadedFiles.value.length).toBe(0)
      expect(isUploading.value).toBe(false)
    })

    it('should upload and add file on success', async () => {
      const { uploadFileWithProgress } = await import('@/api/file')
      uploadFileWithProgress.mockResolvedValue({
        code: 200,
        data: { tempFileId: 'temp-123', originalName: 'resume.pdf', fileSize: 1024, contentType: 'application/pdf', isTemp: true },
      })

      const event = createFileEvent('resume.pdf', 1024)
      await upload.handleFileSelect(event)

      expect(uploadedFiles.value.length).toBe(1)
      expect(uploadedFiles.value[0].fileName).toBe('temp-123')
      expect(uploadedFiles.value[0].originalName).toBe('resume.pdf')
      expect(isUploading.value).toBe(false)
    })

    it('should handle upload API failure', async () => {
      const { uploadFileWithProgress } = await import('@/api/file')
      uploadFileWithProgress.mockResolvedValue({ code: 500, msg: 'Server error' })

      const event = createFileEvent('test.pdf', 1024)
      await upload.handleFileSelect(event)

      expect(uploadedFiles.value.length).toBe(0)
    })

    it('should handle abort error gracefully', async () => {
      const { uploadFileWithProgress } = await import('@/api/file')
      const abortError = new Error('The user aborted a request.')
      abortError.name = 'AbortError'
      uploadFileWithProgress.mockRejectedValue(abortError)

      const event = createFileEvent('test.pdf', 1024)
      await upload.handleFileSelect(event)

      expect(isUploading.value).toBe(false)
    })

    it('should skip when no file selected', async () => {
      const event = { target: { files: [], value: '' } }
      await upload.handleFileSelect(event)
      expect(uploadedFiles.value.length).toBe(0)
    })
  })

  describe('cancelUpload', () => {
    it('should not throw when no active upload', () => {
      expect(() => upload.cancelUpload()).not.toThrow()
    })
  })

  describe('showUploadProgress reactive state', () => {
    it('should show progress during upload', async () => {
      const { uploadFileWithProgress } = await import('@/api/file')
      uploadFileWithProgress.mockImplementation((_file, onProgress) => {
        onProgress({ progress: 50, speed: '1 MB/s', loaded: 512000, total: 1024000, remainingTime: 5 })
        return Promise.resolve({ code: 200, data: { tempFileId: 't1', originalName: 'f.pdf', fileSize: 1024, contentType: 'pdf', isTemp: true } })
      })

      const event = {
        target: { files: [new File(['test'], 'f.pdf', { type: 'application/pdf' })], value: '' },
      }

      const uploadPromise = upload.handleFileSelect(event)

      expect(upload.uploadProgress.value).toBe(50)

      await uploadPromise
      expect(upload.showUploadProgress.value).toBe(false)
    })
  })
})
