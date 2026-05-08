import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useResumeStore } from '@/store/resume'
import { useEditable } from '../useEditable'

vi.mock('@/api/resume', () => ({
  getResume: vi.fn(),
  createResume: vi.fn(),
  updateResume: vi.fn(),
}))

describe('useEditable', () => {
  let store
  let editable

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useResumeStore()
    editable = useEditable()
  })

  describe('setDeep', () => {
    it('should set a top-level field', () => {
      editable.setDeep('baseInfo.name', 'Alice')
      expect(store.content.baseInfo.name).toBe('Alice')
    })

    it('should set a deeply nested field', () => {
      editable.setDeep('education.0.school', 'MIT')
      expect(store.content.education[0].school).toBe('MIT')
    })

    it('should create intermediate objects for missing paths', () => {
      editable.setDeep('custom.deep.field', 'value')
      expect(store.content.custom.deep.field).toBe('value')
    })

    it('should capture undo state', () => {
      const undoSpy = vi.spyOn(store, 'captureUndo')
      editable.setDeep('baseInfo.name', 'Bob')
      expect(undoSpy).toHaveBeenCalled()
    })

    it('should schedule auto-save', () => {
      const saveSpy = vi.spyOn(store, 'scheduleAutoSave')
      editable.setDeep('baseInfo.name', 'Charlie')
      expect(saveSpy).toHaveBeenCalled()
    })
  })

  describe('getDeep', () => {
    it('should read a top-level field', () => {
      store.content.baseInfo.name = 'Alice'
      expect(editable.getDeep('baseInfo.name')).toBe('Alice')
    })

    it('should read a nested field', () => {
      store.content.education = [{ school: 'MIT', major: 'CS', degree: '本科', start: '2020', end: '2024', gpa: '' }]
      expect(editable.getDeep('education.0.school')).toBe('MIT')
    })

    it('should return empty string for missing path', () => {
      expect(editable.getDeep('nonexistent.field')).toBe('')
    })

    it('should return empty string when intermediate is null', () => {
      store.content.baseInfo = null
      expect(editable.getDeep('baseInfo.name')).toBe('')
    })
  })
})
