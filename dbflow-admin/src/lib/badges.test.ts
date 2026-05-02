import {describe, expect, it} from 'vitest'
import {getDecisionBadgeMeta, getRiskBadgeMeta, getStatusBadgeMeta,} from './badges'

describe('DBFlow badge metadata', () => {
    it.each(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const)(
        'maps risk level %s to a stable visual class',
        (risk) => {
            const meta = getRiskBadgeMeta(risk)

            expect(meta.label).toBeTruthy()
            expect(meta.className).toContain('border-')
            expect(meta.className).toContain('text-')
        }
    )

    it.each([
        'EXECUTED',
        'POLICY_DENIED',
        'REQUIRES_CONFIRMATION',
        'FAILED',
    ] as const)('maps decision %s to a stable visual class', (decision) => {
        const meta = getDecisionBadgeMeta(decision)

        expect(meta.label).toBeTruthy()
        expect(meta.className).toContain('border-')
        expect(meta.className).toContain('text-')
    })

    it.each([
        'ACTIVE',
        'DISABLED',
        'REVOKED',
        'EXPIRED',
        'HEALTHY',
        'UNHEALTHY',
    ] as const)('maps status %s to a stable visual class', (status) => {
        const meta = getStatusBadgeMeta(status)

        expect(meta.label).toBeTruthy()
        expect(meta.className).toContain('border-')
        expect(meta.className).toContain('text-')
    })

    it('returns neutral metadata for unknown values', () => {
        expect(getStatusBadgeMeta('SOMETHING_NEW')).toMatchObject({
            label: 'SOMETHING_NEW',
        })
    })
})
