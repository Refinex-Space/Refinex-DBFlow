import {describe, expect, it} from 'vitest'
import {render} from 'vitest-browser-react'
import {RiskBadge} from './risk-badge'

describe('RiskBadge', () => {
    it.each([
        ['LOW', 'text-sky'],
        ['MEDIUM', 'text-amber'],
        ['HIGH', 'text-orange'],
        ['CRITICAL', 'text-red'],
    ])('renders %s with the mapped DBFlow risk tone', async (risk, tone) => {
        const screen = await render(<RiskBadge risk={risk}/>)
        const badge = screen.getByText(risk)

        await expect.element(badge).toBeInTheDocument()
        expect(badge.element().className).toContain(tone)
    })

    it('renders unknown risk values with neutral metadata', async () => {
        const screen = await render(<RiskBadge risk='future-risk'/>)
        const badge = screen.getByText('FUTURE-RISK')

        await expect.element(badge).toBeInTheDocument()
        expect(badge.element().className).toContain('text-muted-foreground')
    })
})
