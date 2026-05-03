import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {ApiClientError} from '@/lib/errors'
import type {IssuedTokenResponse, TokenOptionsResponse, TokenRow,} from '@/types/token'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {TokensPage} from './index'

const mocks = vi.hoisted(() => ({
    fetchTokens: vi.fn(),
    fetchTokenOptions: vi.fn(),
    issueToken: vi.fn(),
    reissueToken: vi.fn(),
    revokeToken: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock('@/api/tokens', () => ({
    fetchTokens: mocks.fetchTokens,
    fetchTokenOptions: mocks.fetchTokenOptions,
    issueToken: mocks.issueToken,
    reissueToken: mocks.reissueToken,
    revokeToken: mocks.revokeToken,
    tokensQueryKey: (filters?: unknown) => ['tokens', filters],
    tokenOptionsQueryKey: ['tokens', 'options'],
}))

vi.mock('sonner', () => ({
    toast: {
        success: mocks.toastSuccess,
        error: mocks.toastError,
    },
}))

vi.mock('@/components/layout/header', () => ({
    Header: ({children}: { children: ReactNode }) => <header>{children}</header>,
}))

vi.mock('@/components/layout/main', () => ({
    Main: ({children}: { children: ReactNode }) => <main>{children}</main>,
}))

vi.mock('@/components/search', () => ({
    Search: () => null,
}))

vi.mock('@/components/theme-switch', () => ({
    ThemeSwitch: () => null,
}))

vi.mock('@/components/config-drawer', () => ({
    ConfigDrawer: () => null,
}))

vi.mock('@/components/profile-dropdown', () => ({
    ProfileDropdown: () => null,
}))

describe('TokensPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchTokens.mockResolvedValue(tokenRows)
        mocks.fetchTokenOptions.mockResolvedValue(tokenOptions)
        mocks.issueToken.mockResolvedValue(issuedToken)
        mocks.reissueToken.mockResolvedValue(reissuedToken)
        mocks.revokeToken.mockResolvedValue({revoked: true})
    })

    it('renders safe token rows without plaintext, hash, password, or connection text', async () => {
        const screen = await renderTokensPage()

        const breadcrumb = screen.getByRole('navigation', {name: '页面路径'})
        await expect.element(breadcrumb.getByText('身份与访问')).toBeInTheDocument()
        await expect.element(breadcrumb.getByText('Token 管理')).toBeInTheDocument()
        const table = screen.getByRole('table')
        await expect.element(table.getByText(/^10$/)).toBeInTheDocument()
        await expect.element(table.getByText(/^alice$/)).toBeInTheDocument()
        await expect.element(table.getByText('dbf_live_123456')).toBeInTheDocument()
        await expect.element(table.getByText('ACTIVE')).toBeInTheDocument()
        await expect.element(screen.getByText('dbf_plaintext_once')).not.toBeInTheDocument()
        await expect.element(screen.getByText('tokenHash')).not.toBeInTheDocument()
        await expect.element(screen.getByText('password')).not.toBeInTheDocument()
        await expect.element(screen.getByText('jdbc:mysql')).not.toBeInTheDocument()
    })

    it('submits username and status filters to the route search callback', async () => {
        const onSearchChange = vi.fn()
        const screen = await renderTokensPage({
            search: {
                username: 'alice',
                status: 'ACTIVE',
            },
            onSearchChange,
        })

        await userEvent.fill(
            screen.getByRole('textbox', {name: '用户名'}),
            'bob'
        )
        await userEvent.click(screen.getByRole('button', {name: '应用筛选'}))

        expect(onSearchChange).toHaveBeenCalledWith({
            username: 'bob',
            status: 'ACTIVE',
        })
    })

    it('issues a token and reveals plaintext only in the reveal dialog', async () => {
        const screen = await renderTokensPage()

        await userEvent.click(screen.getByRole('button', {name: '颁发 Token'}))
        await userEvent.selectOptions(screen.getByLabelText('授权用户'), '1')
        await userEvent.fill(screen.getByLabelText('有效天数'), '7')
        await userEvent.click(screen.getByRole('button', {name: '确认颁发'}))

        await vi.waitFor(() => expect(mocks.issueToken).toHaveBeenCalledOnce())
        expect(mocks.issueToken.mock.calls[0][0]).toEqual({
            userId: 1,
            expiresInDays: 7,
        })
        await expect.element(screen.getByText('dbf_plaintext_once')).toBeInTheDocument()
        await expect.element(screen.getByText('Token ID')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', {name: '我已保存，关闭'}))
        await expect.element(screen.getByText('dbf_plaintext_once')).not.toBeInTheDocument()
    })

    it('reissues a token with default 30 days and clears plaintext after closing reveal', async () => {
        const screen = await renderTokensPage()

        await userEvent.click(screen.getByRole('button', {name: '重发 alice Token'}))
        await expect.element(screen.getByLabelText('有效天数')).toHaveValue(30)
        await userEvent.click(screen.getByRole('button', {name: '确认重发'}))

        await vi.waitFor(() =>
            expect(mocks.reissueToken).toHaveBeenCalledWith(1, {
                expiresInDays: 30,
            })
        )
        await expect.element(screen.getByText('dbf_reissued_once')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', {name: '我已保存，关闭'}))
        await expect.element(screen.getByText('dbf_reissued_once')).not.toBeInTheDocument()
    })

    it('revokes a token through a dangerous confirmation action', async () => {
        const screen = await renderTokensPage()

        await userEvent.click(screen.getByRole('button', {name: '吊销 alice Token'}))
        await userEvent.click(screen.getByRole('button', {name: '确认吊销'}))

        await vi.waitFor(() => expect(mocks.revokeToken).toHaveBeenCalledWith(10))
        expect(mocks.toastSuccess).toHaveBeenCalledWith('Token 已吊销')
    })

    it('shows backend issue errors without opening a plaintext dialog', async () => {
        mocks.issueToken.mockRejectedValue(
            new ApiClientError({
                message: '用户不存在或不可用',
                errorCode: 'INVALID_REQUEST',
                status: 400,
            })
        )
        const screen = await renderTokensPage()

        await userEvent.click(screen.getByRole('button', {name: '颁发 Token'}))
        await userEvent.selectOptions(screen.getByLabelText('授权用户'), '1')
        await userEvent.click(screen.getByRole('button', {name: '确认颁发'}))

        await expect.element(screen.getByText('用户不存在或不可用')).toBeInTheDocument()
        await expect.element(screen.getByText('dbf_plaintext_once')).not.toBeInTheDocument()
    })
})

async function renderTokensPage({
                                    search = {},
                                    onSearchChange = vi.fn(),
                                }: {
    search?: {
        username?: string
        status?: string
    }
    onSearchChange?: (search: {
        username?: string
        status?: string
    }) => void
} = {}): Promise<RenderResult> {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
            mutations: {
                retry: false,
            },
        },
    })

    return render(
        <QueryClientProvider client={queryClient}>
            <TokensPage search={search} onSearchChange={onSearchChange}/>
        </QueryClientProvider>
    )
}

const tokenRows: TokenRow[] = [
    {
        id: 10,
        userId: 1,
        username: 'alice',
        tokenPrefix: 'dbf_live_123456',
        status: 'ACTIVE',
        expiresAt: '2026-06-01T00:00:00Z',
        lastUsedAt: null,
    },
]

const tokenOptions: TokenOptionsResponse = {
    users: [{id: 1, username: 'alice', displayName: 'Alice'}],
}

const issuedToken: IssuedTokenResponse = {
    tokenId: 10,
    userId: 1,
    username: 'alice',
    plaintextToken: 'dbf_plaintext_once',
    tokenPrefix: 'dbf_plaintext_on',
    expiresAt: '2026-06-01T00:00:00Z',
}

const reissuedToken: IssuedTokenResponse = {
    tokenId: 11,
    userId: 1,
    username: 'alice',
    plaintextToken: 'dbf_reissued_once',
    tokenPrefix: 'dbf_reissued_on',
    expiresAt: '2026-06-30T00:00:00Z',
}
