import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {ApiClientError} from '@/lib/errors'
import type {GrantGroupRow, GrantOptionsResponse} from '@/types/access'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {GrantsPage} from './index'

const mocks = vi.hoisted(() => ({
    fetchGrantGroups: vi.fn(),
    fetchGrantOptions: vi.fn(),
    updateProjectGrants: vi.fn(),
    revokeGrant: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock('@/api/grants', () => ({
    fetchGrantGroups: mocks.fetchGrantGroups,
    fetchGrantOptions: mocks.fetchGrantOptions,
    updateProjectGrants: mocks.updateProjectGrants,
    revokeGrant: mocks.revokeGrant,
    grantsQueryKey: (filters?: unknown) => ['grants', filters],
    grantOptionsQueryKey: ['grants', 'options'],
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

describe('GrantsPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchGrantGroups.mockResolvedValue(grantRows)
        mocks.fetchGrantOptions.mockResolvedValue(grantOptions)
        mocks.updateProjectGrants.mockResolvedValue({updated: true})
        mocks.revokeGrant.mockResolvedValue({revoked: true})
    })

    it('renders grouped grants without sensitive connection or credential text', async () => {
        const screen = await renderGrantsPage()

        await expect
            .element(screen.getByRole('heading', {name: '项目授权'}))
            .toBeInTheDocument()
        const table = screen.getByRole('table')
        await expect.element(table.getByText(/^alice$/)).toBeInTheDocument()
        await expect.element(table.getByText('billing-core')).toBeInTheDocument()
        await expect.element(table.getByText('staging')).toBeInTheDocument()
        await expect.element(table.getByText('prod')).toBeInTheDocument()
        await expect.element(table.getByText('WRITE')).toBeInTheDocument()
        await expect.element(table.getByText('READ')).toBeInTheDocument()
        await expect.element(screen.getByText('jdbc:h2')).not.toBeInTheDocument()
        await expect.element(screen.getByText('password')).not.toBeInTheDocument()
        await expect.element(screen.getByText('Token')).not.toBeInTheDocument()
    })

    it('submits all filters to the route search callback', async () => {
        const onSearchChange = vi.fn()
        const screen = await renderGrantsPage({
            search: {
                username: 'alice',
                projectKey: 'billing-core',
                environmentKey: 'staging',
                status: 'ACTIVE',
            },
            onSearchChange,
        })

        await userEvent.fill(
            screen.getByRole('textbox', {name: '用户'}),
            'bob'
        )
        await userEvent.fill(
            screen.getByRole('textbox', {name: '项目'}),
            'analytics'
        )
        await userEvent.fill(
            screen.getByRole('textbox', {name: '环境'}),
            'prod'
        )
        await userEvent.click(screen.getByRole('button', {name: '应用筛选'}))

        expect(onSearchChange).toHaveBeenCalledWith({
            username: 'bob',
            projectKey: 'analytics',
            environmentKey: 'prod',
            status: 'ACTIVE',
        })
    })

    it('creates project grants with multiple selected environments', async () => {
        const screen = await renderGrantsPage()

        await userEvent.click(screen.getByRole('button', {name: '新建授权'}))
        await userEvent.selectOptions(
            screen.getByLabelText('授权用户'),
            '1'
        )
        await userEvent.selectOptions(
            screen.getByLabelText('授权项目'),
            'billing-core'
        )
        await userEvent.selectOptions(screen.getByLabelText('授权类型'), 'WRITE')
        await userEvent.click(screen.getByRole('checkbox', {name: /staging/}))
        await userEvent.click(screen.getByRole('checkbox', {name: /prod/}))
        await userEvent.click(screen.getByRole('button', {name: '保存授权'}))

        await vi.waitFor(() =>
            expect(mocks.updateProjectGrants).toHaveBeenCalledOnce()
        )
        expect(mocks.updateProjectGrants.mock.calls[0][0]).toEqual({
            userId: 1,
            projectKey: 'billing-core',
            environmentKeys: ['staging', 'prod'],
            grantType: 'WRITE',
        })
        expect(mocks.toastSuccess).toHaveBeenCalledWith('授权已保存')
    })

    it('edits a user project grant set and supports empty environment lists', async () => {
        const screen = await renderGrantsPage()

        await userEvent.click(
            screen.getByRole('button', {name: '编辑 alice / billing-core'})
        )
        await userEvent.click(screen.getByRole('checkbox', {name: /staging/}))
        await userEvent.click(screen.getByRole('checkbox', {name: /prod/}))
        await userEvent.click(screen.getByRole('button', {name: '保存变更'}))

        await vi.waitFor(() =>
            expect(mocks.updateProjectGrants).toHaveBeenCalledOnce()
        )
        expect(mocks.updateProjectGrants.mock.calls[0][0]).toEqual({
            userId: 1,
            projectKey: 'billing-core',
            environmentKeys: [],
            grantType: 'WRITE',
        })
    })

    it('revokes a single environment grant with confirmation', async () => {
        const screen = await renderGrantsPage()

        await userEvent.click(
            screen.getByRole('button', {name: '撤销 alice staging'})
        )
        await userEvent.click(screen.getByRole('button', {name: '确认撤销'}))

        await vi.waitFor(() => expect(mocks.revokeGrant).toHaveBeenCalledWith(11))
        expect(mocks.toastSuccess).toHaveBeenCalledWith('授权已撤销')
    })

    it('shows configuration guidance when no environment options exist', async () => {
        mocks.fetchGrantGroups.mockResolvedValue([])
        mocks.fetchGrantOptions.mockResolvedValue({
            ...grantOptions,
            environments: [],
        })

        const screen = await renderGrantsPage()

        await expect
            .element(screen.getByText('尚未配置可授权项目环境'))
            .toBeInTheDocument()
        await userEvent.click(screen.getByRole('button', {name: '新建授权'}))
        await expect
            .element(screen.getByText('请先在 dbflow.projects 中配置项目环境。'))
            .toBeInTheDocument()
    })

    it('displays backend error messages from project grant updates', async () => {
        mocks.updateProjectGrants.mockRejectedValue(
            new ApiClientError({
                message: '项目不存在或不可用',
                errorCode: 'INVALID_REQUEST',
                status: 400,
            })
        )
        const screen = await renderGrantsPage()

        await userEvent.click(
            screen.getByRole('button', {name: '编辑 alice / billing-core'})
        )
        await userEvent.click(screen.getByRole('button', {name: '保存变更'}))

        await expect
            .element(screen.getByText('项目不存在或不可用'))
            .toBeInTheDocument()
        expect(mocks.toastError).toHaveBeenCalledWith('项目不存在或不可用')
    })
})

async function renderGrantsPage({
                                    search = {},
                                    onSearchChange = vi.fn(),
                                }: {
    search?: {
        username?: string
        projectKey?: string
        environmentKey?: string
        status?: string
    }
    onSearchChange?: (search: {
        username?: string
        projectKey?: string
        environmentKey?: string
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
            <GrantsPage search={search} onSearchChange={onSearchChange}/>
        </QueryClientProvider>
    )
}

const grantRows: GrantGroupRow[] = [
    {
        userId: 1,
        username: 'alice',
        projectKey: 'billing-core',
        environments: [
            {
                grantId: 11,
                environmentKey: 'staging',
                grantType: 'WRITE',
                status: 'ACTIVE',
            },
            {
                grantId: 12,
                environmentKey: 'prod',
                grantType: 'READ',
                status: 'ACTIVE',
            },
        ],
    },
]

const grantOptions: GrantOptionsResponse = {
    users: [{id: 1, username: 'alice', displayName: 'Alice'}],
    environments: [
        {
            projectKey: 'billing-core',
            projectName: 'Billing Core',
            environmentKey: 'staging',
            environmentName: 'Staging',
        },
        {
            projectKey: 'billing-core',
            projectName: 'Billing Core',
            environmentKey: 'prod',
            environmentName: 'Production',
        },
    ],
}
