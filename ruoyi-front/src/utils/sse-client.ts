/**
 * SSE 流式对话客户端
 * 支持打字机效果和实时消息接收
 * 使用 fetch API 支持自定义 headers（认证）
 */

import { getToken } from './auth'

interface SseChatOptions {
  baseUrl?: string
  onToken?: (token: string, fullContent: string) => void
  onComplete?: (fullContent: string, data?: Record<string, unknown>) => void
  onError?: (errorMsg: string) => void
  onStart?: (data: Record<string, unknown>) => void
}

interface StreamChatParams {
  sessionId?: number | null
  message: string
  fileNames?: string[]
  scene?: string
  resumeContext?: string
}

class SseChatClient {
  private baseUrl: string
  onToken: (token: string, fullContent: string) => void
  onComplete: (fullContent: string, data?: Record<string, unknown>) => void
  onError: (errorMsg: string) => void
  onStart: (data: Record<string, unknown>) => void
  abortController: AbortController | null = null

  constructor(options: SseChatOptions = {}) {
    this.baseUrl = options.baseUrl || '';
    this.onToken = options.onToken || (() => {});
    this.onComplete = options.onComplete || (() => {});
    this.onError = options.onError || (() => {});
    this.onStart = options.onStart || (() => {});
  }

  /**
   * 发送流式对话请求
   */
  async streamChat(params: StreamChatParams) {
    // 关闭之前的连接
    this.close();

    const { sessionId, message, fileNames, scene = 'default', resumeContext } = params;

    // 构建 URL（用户身份由后端从 JWT 获取）
    const queryParams = new URLSearchParams();
    queryParams.append('message', message);
    queryParams.append('scene', scene);
    if (sessionId) queryParams.append('sessionId', String(sessionId));
    if (fileNames && fileNames.length > 0) {
      fileNames.forEach(name => queryParams.append('fileNames', name));
    }
    if (resumeContext) queryParams.append('resumeContext', resumeContext);

    // 使用 /api 前缀，让 Vite 代理转发到后端
    const url = `${this.baseUrl}/api/ai/chat/stream?${queryParams.toString()}`;

    // 获取认证 token
    const token = getToken();

    return new Promise((resolve, reject) => {
      let fullContent = '';
      let sseBuffer = '';
      let currentEvent = null;
      this.abortController = new AbortController();

      // 使用 fetch API 发送 SSE 请求，支持自定义 headers
      fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'text/event-stream',
        },
        signal: this.abortController.signal,
      })
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }

          // 获取 reader 读取流
          const reader = response.body.getReader();
          const decoder = new TextDecoder();

          // 读取流数据
          const readStream = () => {
            reader.read().then(({ done, value }) => {
              if (done) {
                // 流结束（兜底完成）
                this.onComplete(fullContent, { status: 'completed' });
                resolve({
                  success: true,
                  content: fullContent,
                });
                return;
              }

              // 解码并累积（处理 chunk 边界）
              const chunk = decoder.decode(value, { stream: true });
              sseBuffer += chunk;
              const lines = sseBuffer.split(/\r?\n/);
              sseBuffer = lines.pop() || '';

              for (const rawLine of lines) {
                const line = rawLine.trimEnd();

                // 空行：一个事件结束，重置事件类型
                if (line === '') {
                  currentEvent = null;
                  continue;
                }

                // 注释/心跳
                if (line.startsWith(':')) {
                  continue;
                }

                if (line.startsWith('event:')) {
                  currentEvent = line.substring(6).trim();
                  continue;
                }

                if (!line.startsWith('data:')) {
                  continue;
                }

                const data = line.substring(5).trim();
                console.log('SSE 收到数据:', { event: currentEvent, data: data.substring(0, 50) });

                if (data === '[DONE]') {
                  this.onComplete(fullContent, { status: 'completed' });
                  resolve({
                    success: true,
                    content: fullContent,
                  });
                  return;
                }

                try {
                  const parsed = JSON.parse(data);
                  const eventType = currentEvent || parsed.event;

                  if (eventType === 'start') {
                    this.onStart(parsed);
                  } else if (eventType === 'token' && parsed.token) {
                    fullContent += parsed.token;
                    this.onToken(parsed.token, fullContent);
                  } else if (eventType === 'complete') {
                    this.onComplete(fullContent, parsed);
                    resolve({
                      success: true,
                      content: fullContent,
                      data: parsed,
                    });
                    return;
                  } else if (eventType === 'error') {
                    this.onError(parsed.message || 'Unknown error');
                    reject(new Error(parsed.message || 'SSE error'));
                    return;
                  }
                } catch (e) {
                  if (data && data.trim()) {
                    fullContent += data;
                    this.onToken(data, fullContent);
                  }
                }
              }

              // 继续读取
              readStream();
            }).catch(error => {
              if (error.name === 'AbortError') {
                console.log('SSE connection aborted');
              } else {
                console.error('SSE read error:', error);
                this.onError(error.message);
                reject(error);
              }
            });
          };

          // 开始读取
          readStream();
        })
        .catch(error => {
          console.error('SSE fetch error:', error);
          this.onError(error.message);
          reject(error);
        });
    });
  }

  /**
   * 关闭连接
   */
  close() {
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
  }

  /**
   * 检查服务健康状态
   */
  async healthCheck() {
    try {
      const token = getToken();
      const response = await fetch(`${this.baseUrl}/api/ai/chat/health`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      return response.ok;
    } catch (e) {
      return false;
    }
  }
}

export default SseChatClient;
