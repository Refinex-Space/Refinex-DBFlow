import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import type {AuditEventDetail} from '@/types/audit'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {AuditDetailPage} from './index'

const mocks = vi.hoisted(() => ({
    fetchAuditEventDetail: vi.fn(),
    clipboardWriteText: vi.fn(),
    toastSuccess: vi.fn(),
    toastError: vi.fn(),
}))

vi.mock('@/api/audit', () => ({
    fetchAuditEventDetail: mocks.fetchAuditEventDetail,
    auditEventDetailQueryKey: (eventId: number) => ['audit-events', eventId],
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

describe('AuditDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchAuditEventDetail.mockResolvedValue(auditDetail)
        mocks.clipboardWriteText.mockResolvedValue(undefined)
        Object.defineProperty(navigator, 'clipboard', {
            configurable: true,
            value: {
                writeText: mocks.clipboardWriteText,
            },
        })
    })

    it('loads detail by id and renders the investigation panels', async () => {
        const screen = await renderAuditDetailPage()

        const breadcrumb = screen.getByRole('navigation', {name: '页面路径'})
        await expect
            .element(breadcrumb.getByText('审计', {exact: true}))
            .toBeInTheDocument()
        await expect.element(breadcrumb.getByText('审计详情')).toBeInTheDocument()
        await expect.element(screen.getByText(/^Audit #42$/)).toBeInTheDocument()
        await expect.element(screen.getByText('请求身份')).toBeInTheDocument()
        await expect.element(screen.getByText('project / env')).toBeInTheDocument()
        await expect.element(screen.getByText('tool')).toBeInTheDocument()
        await expect.element(screen.getByText('operation')).toBeInTheDocument()
        await expect.element(screen.getByText('sqlHash')).toBeInTheDocument()
        await expect.element(screen.getByText('requestId')).toBeInTheDocument()
        await expect.element(screen.getByText('client')).toBeInTheDocument()
        await expect.element(screen.getByText('sourceIp')).toBeInTheDocument()
        await expect.element(screen.getByText('affectedRows')).toBeInTheDocument()
        await expect.element(screen.getByText('confirmationId')).toBeInTheDocument()
        await expect.element(screen.getByText('req-1')).toBeInTheDocument()
        await expect.element(screen.getByText('Codex 1.0.0')).toBeInTheDocument()
        await expect.element(screen.getByText('127.0.0.1')).toBeInTheDocument()
        await expect.element(screen.getByText('confirm-1')).toBeInTheDocument()
        await expect.element(screen.getByText('SQL 文本')).toBeInTheDocument()
        await expect
            .element(screen.getByText('DROP TABLE payment_order'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('拒绝 / 失败原因'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('POLICY_DENIED_ERROR'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('DROP 高危 DDL 未命中 YAML 白名单'))
            .toBeInTheDocument()
    })

    it('copies SQL hash and provides return-list navigation', async () => {
        const screen = await renderAuditDetailPage()

        await userEvent.click(
            screen.getByRole('button', {name: '复制 SQL Hash sha256:abc'})
        )
        expect(mocks.clipboardWriteText).toHaveBeenCalledWith('sha256:abc')
        expect(mocks.toastSuccess).toHaveBeenCalledWith('SQL Hash 已复制')

        await expect
            .element(screen.getByRole('link', {name: '返回列表'}))
            .toHaveAttribute('href', '/audit')
    })

    it('renders the reconstructed timeline and excludes sensitive text', async () => {
        const screen = await renderAuditDetailPage()

        await expect
            .element(screen.getByText('request received'))
            .toBeInTheDocument()
        await expect.element(screen.getByText('authorization')).toBeInTheDocument()
        await expect.element(screen.getByText('classification')).toBeInTheDocument()
        await expect
            .element(screen.getByText('policy decision'))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('audit persisted'))
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
        await expect
            .element(screen.getByText('passwordHash'))
            .not.toBeInTheDocument()
    })
})

async function renderAuditDetailPage(): Promise<RenderResult> {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })

    return render(
        <QueryClientProvider client={queryClient}>
            <AuditDetailPage eventId={42}/>
        </QueryClientProvider>
    )
}

const auditDetail: AuditEventDetail = {
    id: 42,
    requestId: 'req-1',
    userId: 1001,
    projectKey: 'billing-core',
    environmentKey: 'prod',
    clientName: 'Codex',
    clientVersion: '1.0.0',
    userAgent: 'Codex/Test',
    sourceIp: '127.0.0.1',
    tool: 'dbflow_execute_sql',
    operationType: 'DROP_TABLE',
    riskLevel: 'HIGH',
    status: 'DENIED',
    decision: 'POLICY_DENIED',
    sqlHash: 'sha256:abc',
    sqlText: 'DROP TABLE payment_order',
    resultSummary: 'DROP rejected before target execution.',
    affectedRows: 0,
    errorCode: 'POLICY_DENIED_ERROR',
    errorMessage: 'DROP 高危 DDL 未命中 YAML 白名单',
    confirmationId: 'confirm-1',
    createdAt: '2026-05-01T12:00:00Z',
}
