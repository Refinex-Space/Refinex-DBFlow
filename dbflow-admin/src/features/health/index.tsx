import {useQuery, useQueryClient} from '@tanstack/react-query'
import {Activity, RefreshCw} from 'lucide-react'
import {fetchHealthPage, healthQueryKey} from '@/api/health'
import {isApiClientError} from '@/lib/errors'
import {formatNumber} from '@/lib/format'
import {dbflowBreadcrumbs} from '@/lib/routes'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Skeleton} from '@/components/ui/skeleton'
import {ConfigDrawer} from '@/components/config-drawer'
import {MetricCard} from '@/components/dbflow/metric-card'
import {PageBreadcrumb} from '@/components/dbflow/page-breadcrumb'
import {StatusBadge} from '@/components/dbflow/status-badge'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {HealthCard} from './components/health-card'

export function HealthPageView() {
    const queryClient = useQueryClient()
    const healthQuery = useQuery({
        queryKey: healthQueryKey,
        queryFn: fetchHealthPage,
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
                <section className='space-y-4'>
                    <PageBreadcrumb
                        items={dbflowBreadcrumbs.health}
                        actions={
                            <Button
                                type='button'
                                variant='outline'
                                disabled={healthQuery.isFetching}
                                onClick={() =>
                                    void queryClient.invalidateQueries({
                                        queryKey: healthQueryKey,
                                    })
                                }
                            >
                                <RefreshCw
                                    className={
                                        healthQuery.isFetching ? 'size-4 animate-spin' : 'size-4'
                                    }
                                />
                                刷新健康状态
                            </Button>
                        }
                    />

                    {healthQuery.isPending && <HealthLoading/>}

                    {healthQuery.isError && (
                        <Alert variant='destructive'>
                            <Activity/>
                            <AlertTitle>系统健康加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(healthQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {healthQuery.isSuccess && (
                        <>
                            <div className='grid gap-3 md:grid-cols-3'>
                                <MetricCard
                                    title='overall'
                                    value={<StatusBadge status={healthQuery.data.overall}/>}
                                />
                                <MetricCard
                                    title='unhealthy / total'
                                    value={`${formatNumber(healthQuery.data.unhealthyCount)} / ${formatNumber(healthQuery.data.totalCount)}`}
                                />
                                <MetricCard
                                    title='components'
                                    value={formatNumber(healthQuery.data.items.length)}
                                />
                            </div>

                            <div className='grid gap-3 xl:grid-cols-2'>
                                {healthQuery.data.items.map((item) => (
                                    <HealthCard
                                        key={`${item.name}:${item.component}`}
                                        item={item}
                                    />
                                ))}
                            </div>
                        </>
                    )}
                </section>
            </Main>
        </>
    )
}

function HealthLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载系统健康'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 6}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '系统健康加载失败'
}
