import {useQuery} from '@tanstack/react-query'
import {fetchOverview} from '@/api/overview'
import {isApiClientError} from '@/lib/errors'
import {ConfigDrawer} from '@/components/config-drawer'
import {PageHeader} from '@/components/dbflow/page-header'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {AttentionItems} from './components/attention-items'
import {DashboardError} from './components/dashboard-error'
import {DashboardLoading} from './components/dashboard-loading'
import {EnvironmentSelector} from './components/environment-selector'
import {OverviewMetrics} from './components/overview-metrics'
import {RecentAuditTable} from './components/recent-audit-table'

export function Dashboard() {
    const overviewQuery = useQuery({
        queryKey: ['overview'],
        queryFn: fetchOverview,
    })

    return (
        <>
            <Header>
                <Search/>
                <ThemeSwitch/>
                <ConfigDrawer/>
                <ProfileDropdown/>
            </Header>

            <Main>
                {overviewQuery.isPending && <DashboardLoading/>}

                {overviewQuery.isError && (
                    <DashboardError message={errorMessage(overviewQuery.error)}/>
                )}

                {overviewQuery.isSuccess && (
                    <section className='space-y-6'>
                        <PageHeader
                            eyebrow='MCP SQL Gateway'
                            title='总览'
                            description={overviewQuery.data.windowLabel}
                            actions={
                                <EnvironmentSelector
                                    options={overviewQuery.data.environmentOptions}
                                />
                            }
                        />

                        <OverviewMetrics metrics={overviewQuery.data.metrics}/>

                        <div className='grid gap-6 xl:grid-cols-[minmax(0,2fr)_minmax(320px,1fr)]'>
                            <RecentAuditTable rows={overviewQuery.data.recentAuditRows}/>
                            <AttentionItems items={overviewQuery.data.attentionItems}/>
                        </div>
                    </section>
                )}
            </Main>
        </>
    )
}

function errorMessage(error: unknown) {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error
        ? error.message
        : '请稍后重试或检查管理端会话状态。'
}
