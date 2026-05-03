import {useQuery} from '@tanstack/react-query'
import {ArrowLeft, RefreshCw} from 'lucide-react'
import {auditEventDetailQueryKey, fetchAuditEventDetail} from '@/api/audit'
import {isApiClientError} from '@/lib/errors'
import {dbflowBreadcrumbs} from '@/lib/routes'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Skeleton} from '@/components/ui/skeleton'
import {ConfigDrawer} from '@/components/config-drawer'
import {CopyButton} from '@/components/dbflow/copy-button'
import {DecisionBadge} from '@/components/dbflow/decision-badge'
import {PageBreadcrumb} from '@/components/dbflow/page-breadcrumb'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {AuditFailurePanel} from './components/audit-failure-panel'
import {AuditIdentityPanel} from './components/audit-identity-panel'
import {AuditSqlPanel} from './components/audit-sql-panel'
import {AuditTimeline} from './components/audit-timeline'

type AuditDetailPageProps = {
    eventId: number
}

export function AuditDetailPage({eventId}: AuditDetailPageProps) {
    const detailQuery = useQuery({
        queryKey: auditEventDetailQueryKey(eventId),
        queryFn: () => fetchAuditEventDetail(eventId),
    })

    const baseUrl = import.meta.env.BASE_URL.replace(/\/+$/, '')
    const auditListHref = `${baseUrl}/audit`

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
                        items={dbflowBreadcrumbs.auditDetail}
                        actions={
                            <>
                                <Button variant='outline' asChild>
                                    <a href={auditListHref}>
                                        <ArrowLeft className='size-4'/>
                                        返回列表
                                    </a>
                                </Button>
                                {detailQuery.data?.sqlHash && (
                                    <CopyButton
                                        value={detailQuery.data.sqlHash}
                                        label='复制 SQL Hash'
                                        ariaLabel={`复制 SQL Hash ${detailQuery.data.sqlHash}`}
                                        successMessage='SQL Hash 已复制'
                                    />
                                )}
                            </>
                        }
                    />

                    {detailQuery.isPending && <AuditDetailLoading/>}

                    {detailQuery.isError && (
                        <Alert variant='destructive'>
                            <RefreshCw/>
                            <AlertTitle>审计详情加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(detailQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {detailQuery.isSuccess && (
                        <>
                            <div className='flex flex-wrap items-center gap-2'>
                                <span className='font-mono text-xs text-muted-foreground'>
                                    Audit #{eventId}
                                </span>
                                <DecisionBadge decision={detailQuery.data.decision}/>
                                <span className='font-mono text-xs text-muted-foreground'>
                  {detailQuery.data.sqlHash}
                </span>
                            </div>
                            <AuditIdentityPanel detail={detailQuery.data}/>
                            <div className='grid gap-4 xl:grid-cols-[minmax(0,1.3fr)_minmax(360px,0.7fr)]'>
                                <AuditSqlPanel detail={detailQuery.data}/>
                                <div className='space-y-4'>
                                    <AuditFailurePanel detail={detailQuery.data}/>
                                    <AuditTimeline detail={detailQuery.data}/>
                                </div>
                            </div>
                        </>
                    )}
                </section>
            </Main>
        </>
    )
}

function AuditDetailLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载审计详情'
            className='space-y-3 rounded-md border bg-card p-4'
        >
            {Array.from({length: 7}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '审计详情加载失败'
}
