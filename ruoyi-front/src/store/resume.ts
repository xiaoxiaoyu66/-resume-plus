import { defineStore } from 'pinia'
import { getResume, createResume, updateResume } from '@/api/resume'
import type {
  ResumeContent,
  ResumeStyle,
  ModuleKey,
  ModuleVisibility,
  ResumeSnapshot
} from '@/types/resume'

const defaultContent: ResumeContent = {
  baseInfo: { name: '', phone: '', email: '', avatar: '', gender: '', birth: '', city: '' },
  intention: { position: '', city: '', salary: '', entryTime: '' },
  education: [],
  experience: [],
  projects: [],
  skills: [],
  evaluation: ''
}

const defaultModuleOrder: ModuleKey[] = ['baseInfo', 'intention', 'education', 'experience', 'projects', 'skills', 'evaluation']

const defaultModuleVisibility: ModuleVisibility = {
  baseInfo: true,
  intention: true,
  education: true,
  experience: true,
  projects: false,
  skills: true,
  evaluation: false
}

const defaultStyle: ResumeStyle = {
  fontFamily: '',
  fontSize: 12,
  lineHeight: 1.7,
  primaryColor: '',
  color: ''
}

interface ResumeState {
  resumeId: number | null
  templateId: string
  title: string
  content: ResumeContent
  moduleVisibility: ModuleVisibility
  moduleOrder: ModuleKey[]
  style: ResumeStyle
  saving: boolean
  lastSaved: Date | null
  autoSaveTimer: ReturnType<typeof setTimeout> | null
  undoStack: ResumeSnapshot[]
  redoStack: ResumeSnapshot[]
  _lastCapture: number
}

export const useResumeStore = defineStore('resume', {
  state: (): ResumeState => ({
    resumeId: null,
    templateId: 'modern',
    title: '我的简历',
    content: JSON.parse(JSON.stringify(defaultContent)),
    moduleVisibility: { ...defaultModuleVisibility },
    moduleOrder: [...defaultModuleOrder],
    style: { ...defaultStyle },
    saving: false,
    lastSaved: null,
    autoSaveTimer: null,
    undoStack: [],
    redoStack: [],
    _lastCapture: 0
  }),

  getters: {
    visibleModules: (state): ModuleKey[] => {
      return state.moduleOrder.filter(key => state.moduleVisibility[key] !== false)
    }
  },

  actions: {
    captureUndo(force = false) {
      const now = Date.now()
      if (!force && this._lastCapture && now - this._lastCapture < 1000) return
      this._lastCapture = now
      this.undoStack.push(this._snapshot())
      if (this.undoStack.length > 50) this.undoStack.shift()
      this.redoStack = []
    },

    undo(): boolean {
      if (this.undoStack.length === 0) return false
      this.redoStack.push(this._snapshot())
      const prev = this.undoStack.pop()!
      this._restore(prev)
      return true
    },

    redo(): boolean {
      if (this.redoStack.length === 0) return false
      this.undoStack.push(this._snapshot())
      const next = this.redoStack.pop()!
      this._restore(next)
      return true
    },

    _snapshot(): ResumeSnapshot {
      return JSON.parse(JSON.stringify({
        content: this.content,
        moduleOrder: this.moduleOrder,
        moduleVisibility: this.moduleVisibility,
        style: this.style
      }))
    },

    _restore(snap: ResumeSnapshot) {
      Object.assign(this.content, snap.content)
      this.moduleOrder = snap.moduleOrder
      this.moduleVisibility = snap.moduleVisibility
      this.style = snap.style
    },

    initNewResume() {
      this.undoStack = []
      this.redoStack = []
      this.resumeId = null
      this.templateId = 'modern'
      this.title = '我的简历'
      this.content = JSON.parse(JSON.stringify(defaultContent))
      this.moduleOrder = [...defaultModuleOrder]
      this.moduleVisibility = { ...defaultModuleVisibility }
      this.style = { ...defaultStyle }
    },

    async loadResume(id: number): Promise<boolean> {
      try {
        const res = await getResume(id) as any
        const data = res.data || res
        this.resumeId = data.id
        this.templateId = data.templateId || 'modern'
        this.title = data.title || '我的简历'
        if (data.content) {
          const parsed = typeof data.content === 'string' ? JSON.parse(data.content) : data.content
          const savedOrder = parsed._moduleOrder
          if (Array.isArray(savedOrder)) {
            this.moduleOrder = savedOrder
          }
          const savedStyle = parsed._style
          if (savedStyle) {
            this.style = { ...this.style, ...savedStyle }
          }
          delete parsed._moduleOrder
          delete parsed._style
          this.content = { ...defaultContent, ...parsed }
        }
        this.undoStack = []
        this.redoStack = []
        return true
      } catch (error) {
        console.error('加载简历失败:', error)
        return false
      }
    },

    updateField(module: string, field: string, value: string) {
      this.captureUndo(true)
      if (this.content[module as keyof ResumeContent]) {
        (this.content[module as keyof ResumeContent] as Record<string, string>)[field] = value
      }
      this.scheduleAutoSave()
    },

    updateModule(module: string, data: unknown) {
      this.captureUndo(true)
      ;(this.content as Record<string, unknown>)[module] = data
      this.scheduleAutoSave()
    },

    setTemplate(templateId: string) {
      this.captureUndo(true)
      this.templateId = templateId
      this.scheduleAutoSave()
    },

    toggleModule(module: string) {
      this.captureUndo(true)
      if (Object.prototype.hasOwnProperty.call(this.moduleVisibility, module)) {
        this.moduleVisibility[module as ModuleKey] = !this.moduleVisibility[module as ModuleKey]
      }
    },

    addArrayEntry(module: string) {
      this.captureUndo(true)
      const templates: Record<string, Record<string, string>> = {
        education: { school: '', major: '', degree: '', start: '', end: '', gpa: '' },
        experience: { company: '', position: '', start: '', end: '', desc: '' },
        projects: { name: '', role: '', start: '', end: '', desc: '' }
      }
      if (templates[module] && Array.isArray(this.content[module as keyof ResumeContent])) {
        ;(this.content[module as keyof ResumeContent] as unknown[]).push(JSON.parse(JSON.stringify(templates[module])))
        this.scheduleAutoSave()
      }
    },

    removeArrayEntry(module: string, index: number) {
      this.captureUndo(true)
      const arr = this.content[module as keyof ResumeContent]
      if (Array.isArray(arr) && arr.length > 0) {
        arr.splice(index, 1)
        this.scheduleAutoSave()
      }
    },

    moveModule(fromIndex: number, toIndex: number) {
      if (fromIndex === toIndex) return
      this.captureUndo(true)
      const order = [...this.moduleOrder]
      const [moved] = order.splice(fromIndex, 1)
      order.splice(toIndex, 0, moved)
      this.moduleOrder = order
      this.scheduleAutoSave()
    },

    scheduleAutoSave() {
      if (this.autoSaveTimer) {
        clearTimeout(this.autoSaveTimer)
      }
      this.autoSaveTimer = setTimeout(() => {
        this.saveResume()
      }, 3000)
    },

    async saveResume() {
      if (this.saving) return
      this.saving = true
      try {
        const payload: Record<string, unknown> = {
          templateId: this.templateId,
          title: this.title,
          content: JSON.stringify({
            ...this.content,
            _moduleOrder: this.moduleOrder,
            _style: this.style
          })
        }
        if (this.resumeId) {
          payload.id = this.resumeId
          await updateResume(payload)
        } else {
          const res = await createResume(payload) as any
          const data = res.data || res
          if (data && data.id) {
            this.resumeId = data.id
          }
        }
        this.lastSaved = new Date()
      } catch (error) {
        console.error('保存简历失败:', error)
      } finally {
        this.saving = false
      }
    }
  }
})
