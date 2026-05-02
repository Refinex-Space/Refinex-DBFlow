import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {ApiClientError} from '@/lib/errors'
import type {AdminUserRow} from '@/types/access'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {UsersPage} from './index'

const mocks = vi.hoisted(() => ({
    fetchUsers: vi.fn(),
    createUser: vi.fn(),
    disableUser: vi.fn(),
    enableUser: vi.fn(),
    resetUserPassword: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock('@/api/users', () => ({
    fetchUsers: mocks.fetchUsers,
    createUser: mocks.createUser,
    disableUser: mocks.disableUser,
    enableUser: mocks.enableUser,
    resetUserPassword: mocks.resetUserPassword,
    usersQueryKey: (filters?: unknown) => ['users', filters],
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

describe('UsersPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchUsers.mockResolvedValue(userRows)
        mocks.createUser.mockResolvedValue(userRows[0])
        mocks.disableUser.mockResolvedValue({disabled: true})
        mocks.enableUser.mockResolvedValue({enabled: true})
        mocks.resetUserPassword.mockResolvedValue({reset: true})
    })

    it('renders safe user rows and never displays passwordHash', async () => {
        const screen = await renderUsersPage()

        await expect
            .element(screen.getByRole('heading', {name: '用户管理'}))
            .toBeInTheDocument()
        const table = screen.getByRole('table')
        await expect.element(table.getByText('授权数')).toBeInTheDocument()
        await expect.element(table.getByText('活跃 Token')).toBeInTheDocument()
        await expect.element(table.getByText(/^alice$/)).toBeInTheDocument()
        await expect.element(table.getByText('Alice Admin')).toBeInTheDocument()
        await expect.element(table.getByText('角色')).toBeInTheDocument()
        await expect.element(table.getByText('ACTIVE')).toBeInTheDocument()
        await expect.element(table.getByText('DISABLED')).toBeInTheDocument()
        await expect.element(screen.getByText('passwordHash')).not.toBeInTheDocument()
    })

    it('submits username and status filters to the route search callback', async () => {
        const onSearchChange = vi.fn()
        const screen = await renderUsersPage({
            search: {username: 'alice', status: 'ACTIVE'},
            onSearchChange,
        })

        await userEvent.fill(
            screen.getByRole('textbox', {name: '筛选用户名'}),
            'bob'
        )
        await userEvent.click(screen.getByRole('button', {name: '应用筛选'}))

        expect(onSearchChange).toHaveBeenCalledWith({
            username: 'bob',
            status: 'ACTIVE',
        })
    })

    it('creates a user from the sheet and shows success feedback', async () => {
        const screen = await renderUsersPage()

        await userEvent.click(screen.getByRole('button', {name: '新建用户'}))
        await userEvent.fill(screen.getByRole('textbox', {name: '用户名'}), 'carol')
        await userEvent.fill(
            screen.getByRole('textbox', {name: '显示名'}),
            'Carol'
        )
        await userEvent.fill(screen.getByLabelText('初始密码'), 'Secret123!')
        await userEvent.click(screen.getByRole('button', {name: '创建用户'}))

        await vi.waitFor(() => expect(mocks.createUser).toHaveBeenCalledOnce())
        expect(mocks.createUser.mock.calls[0][0]).toEqual({
            username: 'carol',
            displayName: 'Carol',
            password: 'Secret123!',
        })
        expect(mocks.toastSuccess).toHaveBeenCalledWith('用户已创建')
    })

    it('requires confirmation before disabling a user', async () => {
        const screen = await renderUsersPage()

        await userEvent.click(screen.getByRole('button', {name: '禁用 alice'}))
        await userEvent.click(screen.getByRole('button', {name: '确认禁用'}))

        await vi.waitFor(() => expect(mocks.disableUser).toHaveBeenCalledWith(1))
        expect(mocks.toastSuccess).toHaveBeenCalledWith('用户已禁用')
    })

    it('resets a password and displays backend errors', async () => {
        mocks.resetUserPassword.mockRejectedValue(
            new ApiClientError({
                message: '密码强度不足',
                errorCode: 'INVALID_REQUEST',
                status: 400,
            })
        )
        const screen = await renderUsersPage()

        await userEvent.click(screen.getByRole('button', {name: '重置 alice 密码'}))
        await userEvent.fill(screen.getByLabelText('新密码'), 'weak')
        await userEvent.click(screen.getByRole('button', {name: '重置密码'}))

        await vi.waitFor(() =>
            expect(mocks.resetUserPassword).toHaveBeenCalledWith(1, {
                newPassword: 'weak',
            })
        )
        await expect.element(screen.getByText('密码强度不足')).toBeInTheDocument()
        expect(mocks.toastError).toHaveBeenCalledWith('密码强度不足')
    })
})

async function renderUsersPage({
                                   search = {},
                                   onSearchChange = vi.fn(),
                               }: {
    search?: { username?: string; status?: string }
    onSearchChange?: (search: { username?: string; status?: string }) => void
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
            <UsersPage search={search} onSearchChange={onSearchChange}/>
        </QueryClientProvider>
    )
}

const userRows: AdminUserRow[] = [
    {
        id: 1,
        username: 'alice',
        displayName: 'Alice Admin',
        role: 'ROLE_ADMIN',
        status: 'ACTIVE',
        grantCount: 2,
        activeTokenCount: 1,
    },
    {
        id: 2,
        username: 'bob',
        displayName: 'Bob Operator',
        role: 'ROLE_ADMIN',
        status: 'DISABLED',
        grantCount: 0,
        activeTokenCount: 0,
    },
]
