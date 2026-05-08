import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { persistTempFiles } from '@/api/file'
import SseChatClient from '@/utils/sse-client'

export function useChatSse(options) {
  const {
    userInput,
    messages,
    isThinking,
    hasStarted,
    uploadedFiles,
    currentSessionId,
    scrollToBottom,
    escapeHtml,
    getCurrentTime,
    currentScene
  } = options

  const sseClient = ref(null)
  let firstTokenTimer = null

  function clearFirstTokenTimer() {
    if (firstTokenTimer) {
      clearTimeout(firstTokenTimer)
      firstTokenTimer = null
    }
  }

  function cancelCurrentReply() {
    stopSse()
    clearFirstTokenTimer()
    if (isThinking.value) {
      isThinking.value = false
      ElMessage.info('已取消本次回答')
    }
  }

  async function sendMessage() {
    const content = userInput.value.trim()
    if (isThinking.value) {
      cancelCurrentReply()
      return
    }
    if (!content && uploadedFiles.value.length === 0) return

    hasStarted.value = true
    const displayContent = content || `已上传 ${uploadedFiles.value.length} 个文件，请分析`
    const sendContent = content || '请分析这些文件的内容'

    messages.value.push({
      role: 'user',
      content: escapeHtml(displayContent),
      time: getCurrentTime()
    })

    userInput.value = ''
    scrollToBottom()
    isThinking.value = true

    let fileNames = []
    try {
      const tempFiles = uploadedFiles.value.filter(f => f.isTemp !== false && !f.fileName.startsWith('ai-uploads/'))
      const persistedFiles = uploadedFiles.value.filter(f => f.isTemp === false || f.fileName.startsWith('ai-uploads/'))

      if (tempFiles.length > 0) {
        console.log('持久化临时文件:', tempFiles.map(f => f.fileName))
        const tempFileIds = tempFiles.map(f => f.fileName)
        const persistRes = await persistTempFiles(tempFileIds) as any

        if (persistRes.code === 200) {
          const persistedFileNames = persistRes.data
            .filter(item => item.success)
            .map(item => item.fileName)

          persistRes.data.forEach((item, index) => {
            if (item.success) {
              const originalFile = tempFiles[index]
              originalFile.fileName = item.fileName
              originalFile.isTemp = false
              originalFile.fileUrl = item.fileUrl
            }
          })

          fileNames = [...persistedFileNames, ...persistedFiles.map(f => f.fileName)]
          console.log('文件持久化成功:', fileNames)
        } else {
          console.error('文件持久化失败:', persistRes.msg)
          ElMessage.warning('文件处理失败，将尝试直接发送')
          fileNames = uploadedFiles.value.map(f => f.fileName)
        }
      } else {
        fileNames = uploadedFiles.value.map(f => f.fileName)
      }
    } catch (e) {
      console.error('文件持久化异常:', e)
      fileNames = uploadedFiles.value.map(f => f.fileName)
    }

    try {
      let gotFirstToken = false
      sseClient.value = new SseChatClient({
        baseUrl: '',
        onToken: (token, fullContent) => {
          console.log('SSE onToken:', { token, fullContentLength: fullContent?.length })
          if (!gotFirstToken) {
            gotFirstToken = true
            clearFirstTokenTimer()
          }
          if (isThinking.value) {
            isThinking.value = false
          }
          const lastIndex = messages.value.length - 1
          const lastMsg = messages.value[lastIndex]
          if (lastMsg && lastMsg.role === 'ai') {
            messages.value.splice(lastIndex, 1, {
              ...lastMsg,
              content: fullContent
            })
          } else {
            messages.value.push({
              role: 'ai',
              content: fullContent,
              time: getCurrentTime()
            })
          }
          console.log('消息已更新:', messages.value[lastIndex]?.content?.substring(0, 50))
          scrollToBottom()
        },
        onComplete: (fullContent, data) => {
          clearFirstTokenTimer()
          isThinking.value = false
          if (data && typeof data === 'object') {
            if (data.sessionId) {
              currentSessionId.value = data.sessionId
            } else if (data.fullContent) {
              try {
                const parsed = JSON.parse((data as any).fullContent)
                if (parsed.sessionId) {
                  currentSessionId.value = parsed.sessionId
                }
              } catch (e) {
              }
            }
          }
          uploadedFiles.value = []
          scrollToBottom()
          console.log('SSE 对话完成:', fullContent)
        },
        onError: (errorMsg) => {
          clearFirstTokenTimer()
          isThinking.value = false
          console.error('SSE 对话错误:', errorMsg)
          ElMessage.error('对话失败: ' + errorMsg)
          messages.value.push({
            role: 'ai',
            content: '抱歉，我遇到了一些问题，请稍后重试。',
            time: getCurrentTime()
          })
          scrollToBottom()
        }
      })

      firstTokenTimer = setTimeout(() => {
        if (!gotFirstToken && isThinking.value) {
          ElMessage.warning('响应较慢，正在处理中。可再次点击发送按钮取消本次回答。')
        }
      }, 10000)

      const userId = JSON.parse(localStorage.getItem('userInfo'))?.userId || 1
      await sseClient.value.streamChat({
        userId: userId,
        sessionId: currentSessionId.value,
        message: sendContent,
        fileNames: fileNames.length > 0 ? fileNames : undefined,
        scene: currentScene?.value || 'default'
      })
    } catch (e) {
      clearFirstTokenTimer()
      console.error('发送消息失败', e)
      isThinking.value = false
      ElMessage.error('发送失败，请稍后重试')
      messages.value.push({
        role: 'ai',
        content: '抱歉，我遇到了一些问题，请稍后重试。',
        time: getCurrentTime()
      })
      scrollToBottom()
    }
  }

  function stopSse() {
    if (sseClient.value) {
      sseClient.value.close()
      sseClient.value = null
    }
  }

  return {
    sendMessage,
    stopSse,
    cancelCurrentReply
  }
}

