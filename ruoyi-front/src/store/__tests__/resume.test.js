import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useResumeStore } from '../resume'

// Mock API module
vi.mock('@/api/resume', () => ({
  getResume: vi.fn(),
  createResume: vi.fn(),
  updateResume: vi.fn(),
}))

describe('resume store', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useResumeStore()
  })

  describe('initNewResume', () => {
    it('should reset to default state', () => {
      // Mutate state first
      store.title = 'modified'
      store.content.baseInfo.name = 'John'
      store.undoStack.push({ some: 'state' })
      store.redoStack.push({ some: 'redo' })

      store.initNewResume()

      expect(store.resumeId).toBeNull()
      expect(store.templateId).toBe('modern')
      expect(store.title).toBe('我的简历')
      expect(store.content.baseInfo.name).toBe('')
      expect(store.undoStack).toEqual([])
      expect(store.redoStack).toEqual([])
      expect(store.moduleVisibility.baseInfo).toBe(true)
      expect(store.moduleVisibility.projects).toBe(false)
      expect(store.style.fontSize).toBe(12)
    })

    it('should not share defaultContent reference across instances', () => {
      const expectedDefault = {
        name: '', phone: '', email: '', avatar: '', gender: '', birth: '', city: ''
      }
      expect(store.content.baseInfo).toEqual(expectedDefault)
      store.content.baseInfo.name = 'changed'
      store.initNewResume()
      expect(store.content.baseInfo.name).toBe('')
    })
  })

  describe('undo / redo', () => {
    it('should capture and undo a field change', () => {
      store.captureUndo(true)          // save current (name: '')
      store.content.baseInfo.name = 'Alice'

      const result = store.undo()
      expect(result).toBe(true)
      // undo restores snapshot from before the change
      expect(store.content.baseInfo.name).toBe('')
    })

    it('should redo after undo', () => {
      store.captureUndo(true)               // save: name=''
      store.content.baseInfo.name = 'Bob'
      store.captureUndo(true)               // save: name='Bob'
      store.content.baseInfo.name = 'Charlie'

      store.undo()
      // undo pops {Bob}, pushes {Charlie} to redo, restores {Bob}
      expect(store.content.baseInfo.name).toBe('Bob')

      const result = store.redo()
      expect(result).toBe(true)
      expect(store.content.baseInfo.name).toBe('Charlie')
    })

    it('should clear redo stack on new capture', () => {
      store.captureUndo(true)
      store.content.baseInfo.name = 'v1'
      store.captureUndo(true)
      store.content.baseInfo.name = 'v2'
      store.undo()

      expect(store.redoStack.length).toBeGreaterThan(0)
      store.captureUndo(true)
      expect(store.redoStack).toEqual([])
    })

    it('should protect against empty undo stack', () => {
      store.undoStack = []
      const result = store.undo()
      expect(result).toBe(false)
    })

    it('should protect against empty redo stack', () => {
      store.redoStack = []
      const result = store.redo()
      expect(result).toBe(false)
    })

    it('should limit undo stack to 50 entries', () => {
      for (let i = 0; i < 60; i++) {
        store.captureUndo(true)
      }
      expect(store.undoStack.length).toBeLessThanOrEqual(50)
    })

    it('should throttle captures without force flag', () => {
      store._lastCapture = Date.now()
      store.captureUndo()
      expect(store.undoStack.length).toBe(0)

      store.captureUndo(true)
      expect(store.undoStack.length).toBe(1)
    })
  })

  describe('updateField / updateModule', () => {
    it('should update a single field and capture undo', () => {
      store.updateField('baseInfo', 'name', 'Alice')
      expect(store.content.baseInfo.name).toBe('Alice')
      expect(store.undoStack.length).toBe(1)
    })

    it('should update an entire module', () => {
      const eduData = [
        { school: 'MIT', major: 'CS', degree: '本科', start: '2020', end: '2024' }
      ]
      store.updateModule('education', eduData)
      expect(store.content.education).toEqual(eduData)
      expect(store.undoStack.length).toBe(1)
    })
  })

  describe('toggleModule', () => {
    it('should toggle visibility', () => {
      expect(store.moduleVisibility.baseInfo).toBe(true)
      store.toggleModule('baseInfo')
      expect(store.moduleVisibility.baseInfo).toBe(false)
      store.toggleModule('baseInfo')
      expect(store.moduleVisibility.baseInfo).toBe(true)
    })

    it('should not add non-existent modules', () => {
      store.toggleModule('nonexistent')
      expect(store.moduleVisibility.nonexistent).toBeUndefined()
    })
  })

  describe('addArrayEntry / removeArrayEntry', () => {
    it('should add an education entry with defaults', () => {
      expect(store.content.education.length).toBe(0)
      store.addArrayEntry('education')
      expect(store.content.education.length).toBe(1)
      expect(store.content.education[0]).toEqual({
        school: '', major: '', degree: '', start: '', end: '', gpa: ''
      })
    })

    it('should add an experience entry with defaults', () => {
      store.addArrayEntry('experience')
      expect(store.content.experience.length).toBe(1)
      expect(store.content.experience[0]).toEqual({
        company: '', position: '', start: '', end: '', desc: ''
      })
    })

    it('should add a projects entry with defaults', () => {
      store.addArrayEntry('projects')
      expect(store.content.projects.length).toBe(1)
      expect(store.content.projects[0]).toEqual({
        name: '', role: '', start: '', end: '', desc: ''
      })
    })

    it('should remove an array entry by index', () => {
      store.addArrayEntry('education')
      store.addArrayEntry('education')
      expect(store.content.education.length).toBe(2)
      store.removeArrayEntry('education', 0)
      expect(store.content.education.length).toBe(1)
    })

    it('should not remove if array is empty', () => {
      store.removeArrayEntry('education', 0)
      expect(store.content.education.length).toBe(0)
    })

    it('should capture undo on add and remove', () => {
      store.addArrayEntry('education')
      expect(store.undoStack.length).toBeGreaterThanOrEqual(1)
    })

    it('should deep clone template entries (no shared refs)', () => {
      store.addArrayEntry('education')
      store.content.education[0].school = 'Peking'
      store.addArrayEntry('education')
      expect(store.content.education[1].school).toBe('')
    })
  })

  describe('moveModule', () => {
    it('should reorder modules', () => {
      const original = [...store.moduleOrder]
      store.moveModule(0, 2)
      expect(store.moduleOrder[2]).toBe(original[0])
    })

    it('should no-op when fromIndex equals toIndex', () => {
      const original = [...store.moduleOrder]
      store.moveModule(1, 1)
      expect(store.moduleOrder).toEqual(original)
    })

    it('should capture undo', () => {
      store.moveModule(0, 1)
      expect(store.undoStack.length).toBe(1)
    })

    it('should produce a reversible move', () => {
      const original = [...store.moduleOrder]
      store.moveModule(0, 3)
      store.undo()
      expect(store.moduleOrder).toEqual(original)
    })
  })

  describe('setTemplate', () => {
    it('should change template and capture undo', () => {
      store.setTemplate('classic')
      expect(store.templateId).toBe('classic')
      expect(store.undoStack.length).toBe(1)
    })
  })

  describe('visibleModules getter', () => {
    it('should return only visible modules in order', () => {
      store.moduleVisibility.baseInfo = true
      store.moduleVisibility.skills = false
      const visible = store.visibleModules
      expect(visible).toContain('baseInfo')
      expect(visible).not.toContain('skills')
    })

    it('should maintain module order', () => {
      const visible = store.visibleModules
      for (let i = 1; i < visible.length; i++) {
        const idxA = store.moduleOrder.indexOf(visible[i - 1])
        const idxB = store.moduleOrder.indexOf(visible[i])
        expect(idxA).toBeLessThan(idxB)
      }
    })
  })

  describe('loadResume', () => {
    it('should handle API returning data with parsed JSON content', async () => {
      const { getResume } = await import('@/api/resume')
      const apiData = {
        id: 42,
        templateId: 'classic',
        title: 'My Resume',
        content: JSON.stringify({
          baseInfo: { name: 'Alice', phone: '13800138000', email: '', avatar: '', gender: '', birth: '', city: '' },
          intention: { position: 'FE', city: '', salary: '', entryTime: '' },
          education: [{ school: 'MIT', major: 'CS', degree: '本科', start: '2020', end: '2024', gpa: '' }],
          experience: [],
          projects: [],
          skills: [{ name: 'Vue', level: '' }],
          evaluation: 'Good',
          _moduleOrder: ['baseInfo', 'skills', 'education'],
          _style: { fontFamily: 'serif', fontSize: 14, lineHeight: 2, primaryColor: '', color: '' }
        })
      }
      getResume.mockResolvedValue({ data: apiData })

      const result = await store.loadResume(42)

      expect(result).toBe(true)
      expect(store.resumeId).toBe(42)
      expect(store.templateId).toBe('classic')
      expect(store.title).toBe('My Resume')
      expect(store.content.baseInfo.name).toBe('Alice')
      expect(store.content.skills[0].name).toBe('Vue')
      expect(store.moduleOrder).toEqual(['baseInfo', 'skills', 'education'])
      expect(store.style.fontSize).toBe(14)
    })

    it('should handle load failure gracefully', async () => {
      const { getResume } = await import('@/api/resume')
      getResume.mockRejectedValue(new Error('Network error'))

      const result = await store.loadResume(999)
      expect(result).toBe(false)
    })
  })
})
