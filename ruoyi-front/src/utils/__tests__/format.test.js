import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { escapeHtml, getCurrentTime } from '../format'

describe('escapeHtml', () => {
  it('should return empty string for null/undefined', () => {
    expect(escapeHtml(null)).toBe('')
    expect(escapeHtml(undefined)).toBe('')
    expect(escapeHtml('')).toBe('')
  })

  it('should escape HTML special characters', () => {
    expect(escapeHtml('<script>alert("xss")</script>'))
      .toBe('&lt;script&gt;alert("xss")&lt;/script&gt;')
  })

  it('should escape &', () => {
    expect(escapeHtml('a & b')).toBe('a &amp; b')
  })

  it('should leave double quotes as-is (innerHTML behavior)', () => {
    expect(escapeHtml('say "hello"')).toBe('say "hello"')
  })

  it('should return plain text unchanged', () => {
    expect(escapeHtml('Hello World')).toBe('Hello World')
    expect(escapeHtml('普通中文')).toBe('普通中文')
    expect(escapeHtml('12345')).toBe('12345')
  })
})

describe('getCurrentTime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should return time in HH:mm format', () => {
    vi.setSystemTime(new Date('2026-05-08T09:05:00'))
    expect(getCurrentTime()).toBe('09:05')
  })

  it('should pad single digit hours and minutes', () => {
    vi.setSystemTime(new Date('2026-05-08T01:02:00'))
    expect(getCurrentTime()).toBe('01:02')
  })

  it('should handle midnight', () => {
    vi.setSystemTime(new Date('2026-05-08T00:00:00'))
    expect(getCurrentTime()).toBe('00:00')
  })

  it('should handle noon', () => {
    vi.setSystemTime(new Date('2026-05-08T12:30:00'))
    expect(getCurrentTime()).toBe('12:30')
  })
})
