import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {type Locator, userEvent} from 'vitest/browser'
import {ApiClientError} from '@/lib/errors'
import {useSessionStore} from '@/stores/session-store'
import type {AdminSession} from '@/types/session'
import {LoginPage} from './login-page'

const loginMock = vi.hoisted(() => vi.fn())
const getCurrentSessionMock = vi.hoisted(() => vi.fn())
const navigateMock = vi.hoisted(() => vi.fn())
const toastSuccessMock = vi.hoisted(() => vi.fn())
const toastErrorMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/session', () => ({
    login: loginMock,
    getCurrentSession: getCurrentSessionMock,
}))

vi.mock('@tanstack/react-router', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-router')>()
    return {
        ...actual,
        useNavigate: () => navigateMock,
    }
})

vi.mock('sonner', () => ({
    toast: {
        success: toastSuccessMock,
        error: toastErrorMock,
    },
}))

const authenticatedSession: AdminSession = {
    authenticated: true,
    username: 'admin',
    displayName: 'DBFlow Administrator',
    roles: ['ROLE_ADMIN'],
    shell: {
        adminName: 'admin',
        mcpStatus: 'HEALTHY',
        mcpTone: 'ok',
        configSourceLabel: 'Local application config',
    },
}

describe('LoginPage', () => {
    beforeEach(() => {
        loginMock.mockReset()
        getCurrentSessionMock.mockReset()
        navigateMock.mockReset()
        toastSuccessMock.mockReset()
        toastErrorMock.mockReset()
        useSessionStore.setState({
            session: null,
            status: 'idle',
            error: null,
        })
    })

    it('renders minimal Chinese DBFlow login content and controls', async () => {
        const screen = await render(<LoginPage/>)

        await expect.element(screen.getByText('DBFlow Admin')).toBeInTheDocument()
        await expect.element(screen.getByText('管理员登录')).toBeInTheDocument()
        await expect
            .element(screen.getByRole('textbox', {name: /^用户名$/i}))
            .toBeInTheDocument()
        await expect.element(screen.getByLabelText(/^密码$/i)).toBeInTheDocument()
        await expect
            .element(screen.getByRole('button', {name: /^登录$/i}))
            .toBeInTheDocument()
    })

    it('toggles password visibility', async () => {
        const screen = await render(<LoginPage/>)
        const passwordInput = screen.getByLabelText(/^密码$/i)

        await expect.element(passwordInput).toHaveAttribute('type', 'password')
        await userEvent.click(screen.getByRole('button', {name: '显示密码'}))

        await expect.element(passwordInput).toHaveAttribute('type', 'text')
        await expect
            .element(screen.getByRole('button', {name: '隐藏密码'}))
            .toBeInTheDocument()
    })

    it('shows an error toast when login fails', async () => {
        loginMock.mockRejectedValue(
            new ApiClientError({
                message: 'Invalid username or password',
                errorCode: 'UNAUTHENTICATED',
                status: 401,
            })
        )
        const screen = await renderLoginPage()

        await submitLogin(screen, 'admin', 'wrong-password')

        await vi.waitFor(() =>
            expect(toastErrorMock).toHaveBeenCalledWith('Invalid username or password')
        )
        expect(navigateMock).not.toHaveBeenCalled()
        expect(useSessionStore.getState().session).toBeNull()
    })

    it('shows the generic error message for non-API login failures', async () => {
        loginMock.mockRejectedValue(new Error('network is hidden from users'))
        const screen = await renderLoginPage()

        await submitLogin(screen, 'admin', 'wrong-password')

        await vi.waitFor(() =>
            expect(toastErrorMock).toHaveBeenCalledWith(
                '登录失败，请检查用户名和密码。'
            )
        )
        expect(navigateMock).not.toHaveBeenCalled()
        expect(useSessionStore.getState().session).toBeNull()
    })

    it('stores session and navigates to redirect after successful login', async () => {
        loginMock.mockResolvedValue(authenticatedSession)
        const screen = await renderLoginPage('/users')

        await submitLogin(screen, 'admin', 'secret')

        await vi.waitFor(() =>
            expect(navigateMock).toHaveBeenCalledWith({
                to: '/users',
                replace: true,
            })
        )
        expect(useSessionStore.getState().session).toEqual(authenticatedSession)
        expect(toastSuccessMock).toHaveBeenCalledWith('已登录 DBFlow Admin')
    })
})

async function renderLoginPage(redirectTo?: string) {
    return render(<LoginPage redirectTo={redirectTo}/>)
}

async function submitLogin(
    screen: RenderResult,
    username: string,
    password: string
) {
    const usernameInput: Locator = screen.getByRole('textbox', {
        name: /^用户名$/i,
    })
    const passwordInput: Locator = screen.getByLabelText(/^密码$/i)

    await userEvent.fill(usernameInput, username)
    await userEvent.fill(passwordInput, password)
    await userEvent.click(screen.getByRole('button', {name: /^登录$/i}))
}
