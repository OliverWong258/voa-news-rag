import type { RagQuestionRequest } from '../../api/contracts'

export interface QaFormValues {
  question: string
  category: string
  startDate: string
  endDate: string
  topK: number
}

export const DEFAULT_QA_FORM: QaFormValues = {
  question: '',
  category: '',
  startDate: '',
  endDate: '',
  topK: 8,
}

export function validateQaForm(values: QaFormValues): string | null {
  if (!values.question.trim()) return '请输入一个问题'
  if (values.question.trim().length > 1000) return '问题不能超过 1000 个字符'
  if (values.startDate && values.endDate && values.startDate > values.endDate) {
    return '开始日期不能晚于结束日期'
  }
  return null
}

export function toRagQuestionRequest(values: QaFormValues): RagQuestionRequest {
  return {
    question: values.question.trim(),
    topK: values.topK,
    category: values.category || undefined,
    startDate: values.startDate || undefined,
    endDate: values.endDate || undefined,
  }
}

