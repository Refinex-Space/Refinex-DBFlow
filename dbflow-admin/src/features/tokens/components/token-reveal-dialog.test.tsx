import {useState} from 'react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {installClipboardMock} from '@/test-utils/clipboard'
import {createIssuedToken} from '@/test-utils/tokens'
import type {IssuedTokenResponse} from '@/types/token'
import {TokenRevealDialog} from './token-reveal-dialog'

const toastSuccess = vi.hoisted(() => vi.fn())
const toastError = vi.hoisted(() => vi.fn())

vi.mock('sonner', () => ({
    toast: {
        success: toastSuccess,
        error: toastError,
    },
}))

describe('TokenRevealDialog', () => {
    beforeEach(() => {
        installClipboardMock()
    })

    it('copies the one-time plaintext token through the clipboard API', async () => {
        const writeText = installClipboardMock()
        const token = createIssuedToken({
            plaintextToken: 'dbf_copy_once',
        })
        const screen = await render(
            <TokenRevealDialog token={token} open={true} onOpenChange={vi.fn()}/>
        )

        await userEvent.click(screen.getByRole('button', {name: '复制明文'}))

        await vi.waitFor(() => expect(writeText).toHaveBeenCalledWith('dbf_copy_once'))
        expect(toastSuccess).toHaveBeenCalledWith('Token 明文已复制')
        expect(toastError).not.toHaveBeenCalled()
    })

    it('lets the parent clear plaintext state when the dialog closes', async () => {
        const screen = await render(<RevealHarness/>)

        await expect.element(screen.getByText('dbf_plaintext_once')).toBeInTheDocument()
        await userEvent.click(screen.getByRole('button', {name: '我已保存，关闭'}))

        await expect.element(screen.getByText('dbf_plaintext_once')).not.toBeInTheDocument()
        await expect.element(screen.getByText('保存 MCP Token 明文')).not.toBeInTheDocument()
    })
})

function RevealHarness() {
    const [open, setOpen] = useState(true)
    const [token, setToken] = useState<IssuedTokenResponse | null>(
        createIssuedToken()
    )

    function handleOpenChange(nextOpen: boolean) {
        setOpen(nextOpen)
        if (!nextOpen) {
            setToken(null)
        }
    }

    return (
        <TokenRevealDialog
            token={token}
            open={open}
            onOpenChange={handleOpenChange}
        />
    )
}
