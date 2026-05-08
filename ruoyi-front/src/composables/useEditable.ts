import { useResumeStore } from '@/store/resume'

export function useEditable() {
  const store = useResumeStore()

  function setDeep(path, value) {
    store.captureUndo()
    const keys = path.split('.')
    let obj = store.content
    for (let i = 0; i < keys.length - 1; i++) {
      if (obj[keys[i]] === undefined || obj[keys[i]] === null) {
        obj[keys[i]] = {}
      }
      obj = obj[keys[i]]
    }
    obj[keys[keys.length - 1]] = value
    store.scheduleAutoSave()
  }

  function getDeep(path) {
    const keys = path.split('.')
    let val = store.content
    for (const key of keys) {
      if (val == null) return ''
      val = val[key]
    }
    return val ?? ''
  }

  return { setDeep, getDeep }
}
