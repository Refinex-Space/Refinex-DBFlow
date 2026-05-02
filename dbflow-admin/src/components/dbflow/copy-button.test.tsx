import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {CopyButton} from './copy-button'

const toastSuccess = vi.hoisted(() => vi.fn())
const toastError = vi.hoisted(() => vi.fn())

vi.mock('sonner', () => ({
    toast: {
        success: toastSuccess,
        error: toastError,
    },
}))

describe('CopyButton', () => {
    const writeText = vi.fn()

    beforeEach(() => {
        vi.clearAllMocks()
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true,
            value: {writeText},
        })
        writeText.mockResolvedValue(undefined)
    })

    it('copies text and shows a success toast', async () => {
        const screen = await render(<CopyButton value='dbflow-token'/>)

        await userEvent.click(screen.getByRole('button', {name: '复制'}))

        await vi.waitFor(() =>
            expect(writeText).toHaveBeenCalledWith('dbflow-token')
        )
        expect(toastSuccess).toHaveBeenCalledWith('已复制到剪贴板')
    })

    it('shows an error toast when clipboard copy fails', async () => {
        writeText.mockRejectedValue(new Error('denied'))
        const screen = await render(<CopyButton value='dbflow-token'/>)

        await userEvent.click(screen.getByRole('button', {name: '复制'}))

        await vi.waitFor(() => expect(toastError).toHaveBeenCalledWith('复制失败'))
    })
})
