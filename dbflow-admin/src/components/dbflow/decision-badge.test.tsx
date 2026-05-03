import {describe, expect, it} from 'vitest'
import {render} from 'vitest-browser-react'
import {DecisionBadge} from './decision-badge'

describe('DecisionBadge', () => {
    it.each([
        ['EXECUTED', 'text-emerald'],
        ['POLICY_DENIED', 'text-red'],
        ['REQUIRES_CONFIRMATION', 'text-amber'],
        ['FAILED', 'text-rose'],
    ])('renders %s with the mapped DBFlow decision tone', async (decision, tone) => {
        const screen = await render(<DecisionBadge decision={decision}/>)
        const badge = screen.getByText(decision)

        await expect.element(badge).toBeInTheDocument()
        expect(badge.element().className).toContain(tone)
    })

    it('renders unknown decision values with neutral metadata', async () => {
        const screen = await render(<DecisionBadge decision='QUEUED'/>)
        const badge = screen.getByText('QUEUED')

        await expect.element(badge).toBeInTheDocument()
        expect(badge.element().className).toContain('text-muted-foreground')
    })
})
