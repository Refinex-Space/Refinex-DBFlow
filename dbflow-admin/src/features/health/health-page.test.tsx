import type {ReactNode} from 'react'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import type {HealthPage} from '@/types/health'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render, type RenderResult} from 'vitest-browser-react'
import {userEvent} from 'vitest/browser'
import {HealthPageView} from './index'

const mocks = vi.hoisted(() => ({
    fetchHealthPage: vi.fn(),
}))

vi.mock('@/api/health', () => ({
    fetchHealthPage: mocks.fetchHealthPage,
    healthQueryKey: ['health'],
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

describe('HealthPageView', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mocks.fetchHealthPage.mockResolvedValue(healthFixture)
    })

    it('renders overall health summary and all health item fields', async () => {
        const screen = await renderHealthPage()

        await expect
            .element(screen.getByRole('heading', {name: '系统健康'}))
            .toBeInTheDocument()
        await expect
            .element(screen.getByText('HEALTHY').first())
            .toBeInTheDocument()
        await expect.element(screen.getByText('0 / 4')).toBeInTheDocument()

        for (const item of healthFixture.items) {
            const card = screen.getByRole('article', {name: `健康项 ${item.name}`})
            await expect
                .element(card.getByText(item.name, {exact: true}))
                .toBeInTheDocument()
            await expect
                .element(card.getByText(item.component, {exact: true}))
                .toBeInTheDocument()
            await expect
                .element(card.getByText(item.status, {exact: true}))
                .toBeInTheDocument()
            await expect
                .element(card.getByText(item.description, {exact: true}))
                .toBeInTheDocument()
            await expect
                .element(card.getByText(item.detail, {exact: true}))
                .toBeInTheDocument()
            await expect
                .element(card.getByText(item.tone, {exact: true}))
                .toBeInTheDocument()
        }

        await expect
            .element(screen.getByText('plain-db-password'))
            .not.toBeInTheDocument()
        await expect
            .element(screen.getByText('jdbc:mysql://db.internal:3306/metadata'))
            .not.toBeInTheDocument()
        await expect.element(screen.getByText('tokenHash')).not.toBeInTheDocument()
    })

    it('invalidates health query when clicking refresh', async () => {
        const queryClient = new QueryClient({
            defaultOptions: {
                queries: {
                    retry: false,
                },
            },
        })
        const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
        const screen = await renderHealthPage(queryClient)

        await expect
            .element(screen.getByText('HEALTHY').first())
            .toBeInTheDocument()
        await userEvent.click(screen.getByRole('button', {name: '刷新健康状态'}))

        expect(invalidateSpy).toHaveBeenCalledWith({queryKey: ['health']})
    })
})

async function renderHealthPage(
    queryClient = createQueryClient()
): Promise<RenderResult> {
    return render(
        <QueryClientProvider client={queryClient}>
            <HealthPageView/>
        </QueryClientProvider>
    )
}

function createQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })
}

const healthFixture: HealthPage = {
    overall: 'HEALTHY',
    tone: 'ok',
    totalCount: 4,
    unhealthyCount: 0,
    items: [
        {
            name: 'metadata database',
            component: 'database',
            status: 'HEALTHY',
            description: 'Metadata database is reachable.',
            detail: 'metadata ok',
            tone: 'ok',
        },
        {
            name: 'target datasource registry',
            component: 'datasource-registry',
            status: 'HEALTHY',
            description: 'Target datasource registry contains 2 pools.',
            detail: 'ops-api / prod',
            tone: 'ok',
        },
        {
            name: 'Nacos',
            component: 'nacos',
            status: 'HEALTHY',
            description: 'Nacos config and discovery are enabled.',
            detail: 'namespace=dev',
            tone: 'ok',
        },
        {
            name: 'MCP endpoint',
            component: 'mcp-endpoint',
            status: 'HEALTHY',
            description: 'MCP endpoint is ready.',
            detail: '/mcp',
            tone: 'ok',
        },
    ],
}
