import { describe, it, expect } from 'vitest'
import { getRandomQuote, buildQuotePrompt, goldBlessing } from '../quoteLibrary'

describe('getRandomQuote', () => {
  it('should return a high score quote for score >= 80', () => {
    const quote = getRandomQuote(95)
    expect(typeof quote).toBe('string')
    expect(quote.length).toBeGreaterThan(0)
  })

  it('should return a high score quote for score 80', () => {
    const quote = getRandomQuote(80)
    expect(typeof quote).toBe('string')
    expect(quote.length).toBeGreaterThan(0)
  })

  it('should return a mid score quote for score 60-79', () => {
    // multiple calls to verify range
    for (let i = 0; i < 20; i++) {
      const quote = getRandomQuote(70)
      expect(typeof quote).toBe('string')
      expect(quote.length).toBeGreaterThan(0)
    }
  })

  it('should return a mid score quote for score 60', () => {
    const quote = getRandomQuote(60)
    expect(typeof quote).toBe('string')
    expect(quote.length).toBeGreaterThan(0)
  })

  it('should return a low score quote for score < 60', () => {
    const quote = getRandomQuote(30)
    expect(typeof quote).toBe('string')
    expect(quote.length).toBeGreaterThan(0)
  })

  it('should return a low score quote for score 0', () => {
    const quote = getRandomQuote(0)
    expect(typeof quote).toBe('string')
    expect(quote.length).toBeGreaterThan(0)
  })

  it('should handle edge case score 59', () => {
    const quote = getRandomQuote(59)
    expect(typeof quote).toBe('string')
    expect(quote.length).toBeGreaterThan(0)
  })

  it('should return different quotes on multiple calls (not always the same)', () => {
    const quotes = new Set()
    for (let i = 0; i < 50; i++) {
      quotes.add(getRandomQuote(95))
    }
    // With 10 quotes in high score pool, there should be variety
    expect(quotes.size).toBeGreaterThan(1)
  })
})

describe('buildQuotePrompt', () => {
  it('should include example quotes from high score pool', () => {
    const prompt = buildQuotePrompt(95)
    expect(prompt).toContain('金简历')
    expect(prompt).toMatch(/^你是一个幽默/)
  })

  it('should include example quotes from mid score pool', () => {
    const prompt = buildQuotePrompt(70)
    expect(prompt).toContain('还行')
    expect(prompt).toMatch(/^你是一个幽默/)
  })

  it('should include example quotes from low score pool', () => {
    const prompt = buildQuotePrompt(30)
    expect(prompt).toContain('菜就多去学习')
  })

  it('should start with the instruction prefix', () => {
    const prompt = buildQuotePrompt(80)
    expect(prompt).toMatch(/^你是一个幽默/)
  })
})

describe('goldBlessing', () => {
  it('should be the expected blessing string', () => {
    expect(goldBlessing).toBe('小何在此，祝您offer拿到手软')
  })
})
