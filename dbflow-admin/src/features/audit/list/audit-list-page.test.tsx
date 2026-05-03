import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import type {AuditEventPage, AuditEventSummary} from '@/types/audit'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {AuditListPage} from './index'

const mocks = vi.hoisted(() => ({
    fetchAuditEvents: vi.fn(),
    clipboardWriteText: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock('@/api/audit', () => ({
    fetchAuditEvents: mocks.fetchAuditEvents,
    auditEventsQueryKey: (filters?: unknown) => ['audit-events', filters],
    AUDIT_DEFAULT_DIRECTION: 'desc',
    AUDIT_DEFAULT_SORT: 'createdAt',
    normalizeAuditFilters: (filters: AuditListPageSearch) => ({
        from: cleanOptionalString(filters.from),
        to: cleanOptionalString(filters.to),
        userId: cleanOptionalString(filters.userId),
        project: cleanOptionalString(filters.project),
        env: cleanOptionalString(filters.env),
        risk: cleanOptionalString(filters.risk),
        decision: cleanOptionalString(filters.decision),
        sqlHash: cleanOptionalString(filters.sqlHash),
        tool: cleanOptionalString(filters.tool),
        page: filters.page ?? 0,
        size: filters.size ?? 20,
        sort: cleanOptionalString(filters.sort) ?? 'createdAt',
        direction: cleanOptionalString(filters.direction) ?? 'desc',
    }),
    auditResetSearch: (filters: AuditListPageSearch) => ({
        page: 0,
        size: filters.size ?? 20,
        sort: cleanOptionalString(filters.sort) ?? 'createdAt',
        direction: cleanOptionalString(filters.direction) ?? 'desc',
    }),
}))

vi.mock('sonner', () => ({
    toast: {
        success: mocks.toastSuccess,
        error: mocks.toastError,
    },
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

describe('AuditListPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchAuditEvents.mockResolvedValue(auditPage)
        mocks.clipboardWriteText.mockResolvedValue(undefined)
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true,
            value: {
                writeText: mocks.clipboardWriteText,
            },
        })
    })

    it('renders server-paged audit rows without sensitive text', async () => {
        const screen = await renderAuditListPage()

        await expect
            .element(screen.getByRole('heading', {name: '审计列表'}))
            .toBeInTheDocument()
        const table = screen.getByRole('table')
        await expect.element(table.getByText('1001')).toBeInTheDocument()
        await expect
            .element(table.getByText('billing-core / prod'))
            .toBeInTheDocument()
        await expect
            .element(table.getByText('dbflow_execute_sql'))
            .toBeInTheDocument()
        await expect.element(table.getByText('DROP_TABLE')).toBeInTheDocument()
        await expect.element(table.getByText('HIGH')).toBeInTheDocument()
        await expect.element(table.getByText('POLICY_DENIED')).toBeInTheDocument()
        await expect.element(table.getByText('sha256:abc')).toBeInTheDocument()
        await expect
            .element(table.getByText('DROP 高危 DDL 未命中 YAML 白名单'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('plain-db-password'))
            .not.toBeInTheDocument()
        await expect
            .element(screen.getByText('jdbc:mysql://'))
            .not.toBeInTheDocument()
        await expect
            .element(screen.getByText('plaintextToken'))
            .not.toBeInTheDocument()
        await expect.element(screen.getByText('tokenHash')).not.toBeInTheDocument()
    })

    it('submits Thymeleaf-equivalent filters through the route search callback', async () => {
        const onSearchChange = vi.fn()
        const screen = await renderAuditListPage({onSearchChange})

        await userEvent.click(screen.getByRole('button', {name: '高级筛选'}))
        await userEvent.fill(screen.getByLabelText('用户 ID'), '1002')
        await userEvent.fill(screen.getByLabelText('项目'), 'analytics')
        await userEvent.fill(screen.getByLabelText('环境'), 'staging')
        await userEvent.selectOptions(screen.getByLabelText('Risk'), 'CRITICAL')
        await userEvent.selectOptions(screen.getByLabelText('Decision'), 'FAILED')
        await userEvent.fill(
            screen.getByRole('textbox', {name: 'SQL Hash'}),
            'sha256:def'
        )
        await userEvent.selectOptions(
            screen.getByLabelText('Tool'),
            'dbflow_explain_sql'
        )
        await userEvent.selectOptions(screen.getByLabelText('每页'), '50')
        await userEvent.click(screen.getByRole('button', {name: '应用筛选'}))

        expect(onSearchChange).toHaveBeenCalledWith({
            userId: '1002',
            project: 'analytics',
            env: 'staging',
            risk: 'CRITICAL',
            decision: 'FAILED',
            sqlHash: 'sha256:def',
            tool: 'dbflow_explain_sql',
            page: 0,
            size: 50,
            sort: 'createdAt',
            direction: 'desc',
        })
    })

    it('shows active filter chips and resets filters', async () => {
        const onSearchChange = vi.fn()
        const screen = await renderAuditListPage({
            search: {
                project: 'billing-core',
                decision: 'POLICY_DENIED',
                page: 1,
                size: 20,
                sort: 'createdAt',
                direction: 'desc',
            },
            onSearchChange,
        })

        const chipBar = screen.getByLabelText('已应用审计筛选')
        await expect.element(chipBar.getByText('项目')).toBeInTheDocument()
        await expect.element(chipBar.getByText('billing-core')).toBeInTheDocument()
        await expect.element(chipBar.getByText('Decision')).toBeInTheDocument()
        await expect.element(screen.getByText('POLICY_DENIED')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', {name: '重置筛选'}))

        expect(onSearchChange).toHaveBeenCalledWith({
            page: 0,
            size: 20,
            sort: 'createdAt',
            direction: 'desc',
        })
    })

    it('updates page search for server-side pagination and copies SQL hash', async () => {
        const onSearchChange = vi.fn()
        const screen = await renderAuditListPage({onSearchChange})

        await userEvent.click(screen.getByRole('button', {name: '下一页'}))
        expect(onSearchChange).toHaveBeenCalledWith({
            page: 1,
            size: 20,
            sort: 'createdAt',
            direction: 'desc',
        })

        await userEvent.click(
            screen.getByRole('button', {name: '复制 SQL Hash sha256:abc'})
        )
        expect(mocks.clipboardWriteText).toHaveBeenCalledWith('sha256:abc')
        expect(mocks.toastSuccess).toHaveBeenCalledWith('SQL Hash 已复制')
    })

    it('renders a detail navigation target for each row', async () => {
        const screen = await renderAuditListPage()

        const detailLink = screen.getByRole('link', {name: '查看拒绝原因'})
        await expect.element(detailLink).toHaveAttribute('href', '/audit/42')
    })
})

async function renderAuditListPage({
                                       search = {},
                                       onSearchChange = vi.fn(),
                                   }: {
    search?: Partial<AuditListPageSearch>
    onSearchChange?: (search: AuditListPageSearch) => void
} = {}): Promise<RenderResult> {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })

    return render(
        <QueryClientProvider client={queryClient}>
            <AuditListPage search={search} onSearchChange={onSearchChange}/>
        </QueryClientProvider>
    )
}

type AuditListPageSearch = {
    from?: string
    to?: string
    userId?: string
    project?: string
    env?: string
    risk?: string
    decision?: string
    sqlHash?: string
    tool?: string
    page?: number
    size?: number
    sort?: string
    direction?: string
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}

const auditRow: AuditEventSummary = {
    id: 42,
    requestId: 'req-1',
    userId: 1001,
    projectKey: 'billing-core',
    environmentKey: 'prod',
    clientName: 'Codex',
    clientVersion: '1.0.0',
    tool: 'dbflow_execute_sql',
    operationType: 'DROP_TABLE',
    riskLevel: 'HIGH',
    status: 'DENIED',
    decision: 'POLICY_DENIED',
    sqlHash: 'sha256:abc',
    resultSummary: 'DROP 高危 DDL 未命中 YAML 白名单',
    affectedRows: 0,
    createdAt: '2026-05-01T12:00:00Z',
}

const auditPage: AuditEventPage = {
    content: [auditRow],
    page: 0,
    size: 20,
    totalElements: 21,
    totalPages: 2,
    sort: 'createdAt',
    direction: 'desc',
}
