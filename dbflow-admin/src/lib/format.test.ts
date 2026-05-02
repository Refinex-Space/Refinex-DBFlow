import {describe, expect, it} from 'vitest'
import {formatDateTime, formatDurationMs, formatNumber, formatPercent, formatText,} from './format'

describe('DBFlow format helpers', () => {
    it('formats missing text with a fallback dash', () => {
        expect(formatText(null)).toBe('-')
        expect(formatText('')).toBe('-')
        expect(formatText(' admin ')).toBe('admin')
    })

    it('formats common numeric values consistently', () => {
        expect(formatNumber(12345)).toBe('12,345')
        expect(formatPercent(0.375)).toBe('37.5%')
        expect(formatDurationMs(25)).toBe('25 ms')
        expect(formatDurationMs(2500)).toBe('2.5 s')
    })

    it('formats invalid dates with the fallback dash', () => {
        expect(formatDateTime(null)).toBe('-')
        expect(formatDateTime('not-a-date')).toBe('-')
    })
})
