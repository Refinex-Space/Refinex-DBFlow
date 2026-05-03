import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import type {Overview} from '@/types/overview'
import {describe, expect, it, vi} from 'vitest'
import {render} from 'vitest-browser-react'
import {Dashboard} from './index'

const fetchOverviewMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/overview', () => ({
    fetchOverview: fetchOverviewMock,
}))

vi.mock('@/components/layout/header', () => ({
    Header: ({children}: { children: ReactNode }) => (
        <header>{children}</header>
    ),
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

describe('Dashboard', () => {
    it('renders real overview metrics, recent audit rows, and attention items', async () => {
        fetchOverviewMock.mockResolvedValue(overviewFixture)

        const screen = await renderDashboard()

        const breadcrumb = screen.getByRole('navigation', {name: '页面路径'})
        await expect.element(breadcrumb.getByText('工作台')).toBeInTheDocument()
        await expect.element(breadcrumb.getByText('总览')).toBeInTheDocument()
        await expect.element(screen.getByText('SQL 请求')).toBeInTheDocument()
        await expect.element(screen.getByText('12')).toBeInTheDocument()
        await expect.element(screen.getByText('alice')).toBeInTheDocument()
        await expect
            .element(screen.getByRole('table').getByText('demo / prod'))
            .toBeInTheDocument()
        const table = screen.getByRole('table')
        await expect.element(table.getByText('CRITICAL')).toBeInTheDocument()
        await expect.element(table.getByText('POLICY_DENIED')).toBeInTheDocument()
        await expect.element(table.getByText('sha256:abc')).toBeInTheDocument()
        await expect
            .element(table.getByRole('link', {name: '详情'}))
            .toHaveAttribute('href', '/admin/audit/42')
        await expect
            .element(screen.getByText('最近 24 小时有 3 条策略拒绝'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('combobox', {name: '环境范围'}))
            .toBeDisabled()
        expect(fetchOverviewMock).toHaveBeenCalledTimes(1)
    })

    it('shows a loading skeleton before overview data resolves', async () => {
        fetchOverviewMock.mockReturnValue(new Promise(() => undefined))

        const screen = await renderDashboard()

        await expect
            .element(screen.getByRole('status', {name: '正在加载总览'}))
            .toBeInTheDocument()
    })

    it('renders empty states when overview lists are empty', async () => {
        fetchOverviewMock.mockResolvedValue({
            ...overviewFixture,
            recentAuditRows: [],
            attentionItems: [],
        })

        const screen = await renderDashboard()

        await expect
            .element(screen.getByText('当前暂无审计事件。'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('当前无需要关注的运行时事项。'))
            .toBeInTheDocument()
    })
})

async function renderDashboard() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })

    return render(
        <QueryClientProvider client={queryClient}>
            <Dashboard/>
        </QueryClientProvider>
    )
}

const overviewFixture: Overview = {
    metrics: [
        {
            label: 'SQL 请求',
            value: '12',
            hint: '最近 24 小时 execute / explain / inspect',
            tone: 'neutral',
        },
    ],
    recentAuditRows: [
        {
            id: 42,
            time: '2026-05-02 21:15:30',
            user: 'alice',
            project: 'demo',
            env: 'prod',
            operation: 'DDL',
            risk: 'CRITICAL',
            riskTone: 'bad',
            decision: 'POLICY_DENIED',
            decisionTone: 'bad',
            sqlHash: 'sha256:abc',
        },
    ],
    attentionItems: [
        {
            label: '最近 24 小时有 3 条策略拒绝',
            status: 'POLICY_DENIED',
            tone: 'bad',
            href: '/admin/audit?decision=POLICY_DENIED',
        },
    ],
    environmentOptions: [
        {
            value: '',
            label: '全部环境',
        },
        {
            value: 'demo/prod',
            label: 'demo / prod',
        },
    ],
    windowLabel: '后端窗口标签',
}
