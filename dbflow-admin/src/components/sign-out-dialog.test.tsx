import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {useSessionStore} from '@/stores/session-store'
import {SignOutDialog} from './sign-out-dialog'

const mocks = vi.hoisted(() => ({
    navigate: vi.fn(),
    logout: vi.fn(),
    toastError: vi.fn(),
}))

const MOCK_HREF = 'https://app.test/dashboard?tab=1'

vi.mock('@/api/session', () => ({
    getCurrentSession: vi.fn(),
    logout: mocks.logout,
}))

vi.mock('sonner', () => ({
    toast: {error: mocks.toastError},
}))

vi.mock('@tanstack/react-router', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-router')>()
    return {
        ...actual,
        useNavigate: () => mocks.navigate,
        useLocation: () => ({href: MOCK_HREF}),
    }
})

describe('SignOutDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.logout.mockResolvedValue(undefined)
        useSessionStore.setState({
            session: {
                authenticated: true,
                username: 'admin',
                displayName: 'admin',
                roles: ['ROLE_ADMIN'],
                shell: {
                    adminName: 'admin',
                    mcpStatus: 'HEALTHY',
                    mcpTone: 'ok',
                    configSourceLabel: 'Local application config',
                },
            },
            status: 'authenticated',
            error: null,
        })
    })

    it('calls JSON logout and navigates to login with current location as redirect', async () => {
        const onOpenChange = vi.fn()
        const {getByRole} = await render(
            <SignOutDialog open onOpenChange={onOpenChange}/>
        )

        await userEvent.click(getByRole('button', {name: /^退出登录$/i}))

        await vi.waitFor(() => expect(mocks.logout).toHaveBeenCalledOnce())
        expect(useSessionStore.getState().status).toBe('anonymous')
        expect(onOpenChange).toHaveBeenCalledWith(false)
        expect(mocks.navigate).toHaveBeenCalledWith({
            to: '/login',
            search: {redirect: MOCK_HREF},
            replace: true,
        })
    })

    it('does not call logout or navigate when cancel is clicked', async () => {
        const {getByRole} = await render(
            <SignOutDialog open onOpenChange={vi.fn()}/>
        )

        await userEvent.click(getByRole('button', {name: /^取消$/i}))

        expect(mocks.logout).not.toHaveBeenCalled()
        expect(mocks.navigate).not.toHaveBeenCalled()
    })

    it('keeps the session when JSON logout fails', async () => {
        mocks.logout.mockRejectedValue(new Error('network'))
        const {getByRole} = await render(
            <SignOutDialog open onOpenChange={vi.fn()}/>
        )

        await userEvent.click(getByRole('button', {name: /^退出登录$/i}))

        await vi.waitFor(() => expect(mocks.toastError).toHaveBeenCalledOnce())
        expect(useSessionStore.getState().status).toBe('authenticated')
        expect(mocks.navigate).not.toHaveBeenCalled()
    })
})
