export const CATEGORIES = ['Africa', 'Asia', 'Politics', 'United States'] as const

export type Category = (typeof CATEGORIES)[number]

