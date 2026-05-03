import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import type {DangerousPolicyPage} from '@/types/policy'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {DangerousPoliciesPageView} from './index'

const mocks = vi.hoisted(() => ({
    fetchDangerousPolicies: vi.fn(),
}))

vi.mock('@/api/policies', () => ({
    fetchDangerousPolicies: mocks.fetchDangerousPolicies,
    dangerousPoliciesQueryKey: ['policies', 'dangerous'],
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

describe('DangerousPoliciesPageView', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchDangerousPolicies.mockResolvedValue(policyFixture)
    })

    it('renders defaults, whitelist, strengthened rules and denied-audit link', async () => {
        const screen = await renderPolicyPage()

        const breadcrumb = screen.getByRole('navigation', {name: '页面路径'})
        await expect.element(breadcrumb.getByText('配置与策略')).toBeInTheDocument()
        await expect.element(breadcrumb.getByText('危险策略')).toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: 'DROP_TABLE'}).first())
            .toBeInTheDocument()
        await expect.element(screen.getByText('POLICY_DENIED')).toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: '必须命中 DROP 白名单'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: 'ops', exact: true}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: 'prod', exact: true}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: 'ops_schema'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: 'legacy_jobs'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('cell', {name: 'prod 命中后仍拒绝'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('TRUNCATE confirmation'))
            .toBeInTheDocument()
        await expect.element(screen.getByText('prod 强化')).toBeInTheDocument()

        const auditLink = screen.getByRole('link', {name: '查看被拒绝审计'})
        await expect
            .element(auditLink)
            .toHaveAttribute('href', '/audit?decision=POLICY_DENIED')
        await expect
            .element(screen.getByText('secret-password'))
            .not.toBeInTheDocument()
        await expect
            .element(screen.getByText('jdbc:mysql://db.internal:3306/ops'))
            .not.toBeInTheDocument()
    })

    it('shows a clear empty whitelist message', async () => {
        mocks.fetchDangerousPolicies.mockResolvedValue({
            ...policyFixture,
            whitelist: [],
            emptyHint: '后端空白名单提示',
        })

        const screen = await renderPolicyPage()

        await expect
            .element(screen.getByRole('heading', {name: '无白名单条目'}))
            .toBeInTheDocument()
    })

    it('opens policy reason sheet from a table row action', async () => {
        const screen = await renderPolicyPage()

        await userEvent.click(
            screen.getByRole('button', {name: '查看 DROP_TABLE 默认策略说明'})
        )

        await expect.element(screen.getByRole('dialog')).toBeInTheDocument()
        await expect
            .element(screen.getByRole('heading', {name: 'DROP_TABLE'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByRole('dialog').getByText('必须命中 DROP 白名单'))
            .toBeInTheDocument()
    })
})

async function renderPolicyPage(): Promise<RenderResult> {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })

    return render(
        <QueryClientProvider client={queryClient}>
            <DangerousPoliciesPageView/>
        </QueryClientProvider>
    )
}

const policyFixture: DangerousPolicyPage = {
    defaults: [
        {
            operation: 'DROP_TABLE',
            risk: 'CRITICAL',
            decision: 'POLICY_DENIED',
            requirement: '必须命中 DROP 白名单',
            tone: 'bad',
        },
    ],
    whitelist: [
        {
            operation: 'DROP_TABLE',
            risk: 'CRITICAL',
            project: 'ops',
            env: 'prod',
            schema: 'ops_schema',
            table: 'legacy_jobs',
            allowProd: 'NO',
            prodRule: 'prod 命中后仍拒绝',
            tone: 'warn',
        },
    ],
    rules: [
        {
            name: 'DROP 白名单',
            status: 'ENFORCED',
            description: 'DROP_DATABASE 与 DROP_TABLE 默认拒绝',
            detail: '必须命中 YAML/Nacos 白名单。',
            tone: 'bad',
        },
        {
            name: 'TRUNCATE confirmation',
            status: 'ENFORCED',
            description: 'TRUNCATE 默认创建服务端确认挑战',
            detail: '确认挑战默认有效期 5 分钟。',
            tone: 'warn',
        },
        {
            name: 'prod 强化',
            status: 'ENFORCED',
            description: '生产环境需要显式放行',
            detail:
                'prod/production 环境命中 DROP 白名单后仍要求 allow-prod-dangerous-ddl=true。',
            tone: 'bad',
        },
    ],
    emptyHint: '',
}
