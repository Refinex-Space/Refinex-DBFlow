import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import type {ConfigPage} from '@/types/config'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {ConfigPageView} from './index'

const mocks = vi.hoisted(() => ({
    fetchConfigPage: vi.fn(),
}))

vi.mock('@/api/config', () => ({
    fetchConfigPage: mocks.fetchConfigPage,
    configQueryKey: ['config'],
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

describe('ConfigPageView', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchConfigPage.mockResolvedValue(configFixture)
    })

    it('renders source label and sanitized config table fields', async () => {
        const screen = await renderConfigPage()

        const breadcrumb = screen.getByRole('navigation', {name: '页面路径'})
        await expect.element(breadcrumb.getByText('配置与策略')).toBeInTheDocument()
        await expect.element(breadcrumb.getByText('配置查看')).toBeInTheDocument()
        await expect.element(screen.getByText('Local application config')).toBeInTheDocument()
        const table = screen.getByRole('table')
        for (const text of [
            'ops',
            'Operations',
            'prod',
            'Production',
            'ops/prod',
            'mysql',
            'db.internal',
            '3306',
            'ops_schema',
            'dbflow_reader',
            'maxPool=2 minIdle=0',
            '已同步',
        ]) {
            await expect
                .element(table.getByRole('cell', {name: text, exact: true}))
                .toBeInTheDocument()
        }
        await expect.element(screen.getByText('jdbc:mysql://db.internal:3306/ops_schema')).not.toBeInTheDocument()
        await expect.element(screen.getByText('secret-password')).not.toBeInTheDocument()
    })

    it('refreshes config data when clicking the refresh button', async () => {
        const screen = await renderConfigPage()

        await expect.element(screen.getByText('Local application config')).toBeInTheDocument()
        await userEvent.click(screen.getByRole('button', {name: '刷新配置'}))

        await vi.waitFor(() => expect(mocks.fetchConfigPage).toHaveBeenCalledTimes(2))
    })

    it('opens a sanitized detail sheet when clicking a config row', async () => {
        const screen = await renderConfigPage()

        await userEvent.click(screen.getByRole('button', {name: '查看 ops/prod 配置详情'}))

        await expect.element(screen.getByRole('heading', {name: 'ops/prod'})).toBeInTheDocument()
        const detailSheet = screen.getByRole('dialog')
        await expect.element(detailSheet.getByText('db.internal')).toBeInTheDocument()
        await expect.element(detailSheet.getByText('maxPool=2 minIdle=0')).toBeInTheDocument()
        await expect.element(screen.getByText('jdbc:mysql://db.internal:3306/ops_schema')).not.toBeInTheDocument()
        await expect.element(screen.getByText('secret-password')).not.toBeInTheDocument()
    })

    it('renders configured empty state when no projects exist', async () => {
        mocks.fetchConfigPage.mockResolvedValue({
            sourceLabel: 'Local application config',
            rows: [],
            emptyHint: '当前未配置 dbflow.projects。',
        })

        const screen = await renderConfigPage()

        await expect
            .element(screen.getByRole('heading', {name: '当前未配置 dbflow.projects。'}))
            .toBeInTheDocument()
    })
})

async function renderConfigPage(): Promise<RenderResult> {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })

    return render(
        <QueryClientProvider client={queryClient}>
            <ConfigPageView/>
        </QueryClientProvider>
    )
}

const configFixture: ConfigPage = {
    sourceLabel: 'Local application config',
    rows: [
        {
            project: 'ops',
            projectName: 'Operations',
            env: 'prod',
            envName: 'Production',
            datasource: 'ops/prod',
            type: 'mysql',
            host: 'db.internal',
            port: '3306',
            schema: 'ops_schema',
            username: 'dbflow_reader',
            limits: 'maxPool=2 minIdle=0',
            syncStatus: '已同步',
        },
    ],
    emptyHint: '',
}
