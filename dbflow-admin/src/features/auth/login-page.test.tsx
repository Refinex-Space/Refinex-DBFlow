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

    it('renders DBFlow governance content and login controls', async () => {
        const screen = await render(<LoginPage/>)

        await expect
            .element(screen.getByRole('heading', {name: 'MCP SQL Gateway'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('Database Operation Governance'))
            .toBeInTheDocument()
        await expect.element(screen.getByText('Audit Ready')).toBeInTheDocument()
        await expect
            .element(screen.getByRole('textbox', {name: /^Username$/i}))
            .toBeInTheDocument()
        await expect.element(screen.getByLabelText(/^Password$/i)).toBeInTheDocument()
        await expect
            .element(screen.getByRole('button', {name: /^Sign in$/i}))
            .toBeInTheDocument()
    })

    it('toggles password visibility', async () => {
        const screen = await render(<LoginPage/>)
        const passwordInput = screen.getByLabelText(/^Password$/i)

        await expect.element(passwordInput).toHaveAttribute('type', 'password')
        await userEvent.click(screen.getByRole('button', {name: 'Show password'}))

        await expect.element(passwordInput).toHaveAttribute('type', 'text')
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
        expect(toastSuccessMock).toHaveBeenCalledWith('Welcome to DBFlow Admin')
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
        name: /^Username$/i,
    })
    const passwordInput: Locator = screen.getByLabelText(/^Password$/i)

    await userEvent.fill(usernameInput, username)
    await userEvent.fill(passwordInput, password)
    await userEvent.click(screen.getByRole('button', {name: /^Sign in$/i}))
}
