import { describe, expect, it } from 'vitest'
import { toRagQuestionRequest, validateQaForm } from './qaFormModel'

describe('Q&A form', () => {
  it('normalizes the streaming request', () => {
    expect(
      toRagQuestionRequest({
        question: '  发生了什么？ ',
        category: 'Politics',
        startDate: '',
        endDate: '2026-07-12',
        topK: 8,
      }),
    ).toEqual({
      question: '发生了什么？',
      category: 'Politics',
      startDate: undefined,
      endDate: '2026-07-12',
      topK: 8,
    })
  })

  it('validates a blank question and reversed dates', () => {
    expect(validateQaForm({ question: ' ', category: '', startDate: '', endDate: '', topK: 8 })).toBe(
      '请输入一个问题',
    )
    expect(
      validateQaForm({
        question: '问题',
        category: '',
        startDate: '2026-07-12',
        endDate: '2026-07-01',
        topK: 8,
      }),
    ).toBe('开始日期不能晚于结束日期')
  })
})
