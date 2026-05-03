import {useQuery} from '@tanstack/react-query'
import type {AuditEventFilters} from '@/types/audit'
import {ChevronLeft, ChevronRight, RefreshCw, ScrollText} from 'lucide-react'
import {auditEventsQueryKey, fetchAuditEvents, normalizeAuditFilters,} from '@/api/audit'
import {isApiClientError} from '@/lib/errors'
import {formatNumber} from '@/lib/format'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Skeleton} from '@/components/ui/skeleton'
import {ConfigDrawer} from '@/components/config-drawer'
import {EmptyState} from '@/components/dbflow/empty-state'
import {PageHeader} from '@/components/dbflow/page-header'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {AuditFilterChips} from './components/audit-filter-chips'
import {AuditFilterSheet} from './components/audit-filter-sheet'
import {AuditTable} from './components/audit-table'

export type AuditListPageSearch = AuditEventFilters

type AuditListPageProps = {
    search: AuditListPageSearch
    onSearchChange: (search: AuditListPageSearch) => void
}

export function AuditListPage({search, onSearchChange}: AuditListPageProps) {
    const filters = normalizeAuditFilters(search)
    const auditQuery = useQuery({
        queryKey: auditEventsQueryKey(filters),
        queryFn: () => fetchAuditEvents(filters),
    })

    function updatePage(page: number) {
        onSearchChange(
            normalizeAuditFilters({
                ...filters,
                page,
            })
        )
    }

    return (
        <>
            <Header>
                <Search/>
                <ThemeSwitch/>
                <ConfigDrawer/>
                <ProfileDropdown/>
            </Header>

            <Main>
                <section className='space-y-6'>
                    <PageHeader
                        eyebrow='审计'
                        title='审计列表'
                        description='按时间、用户、项目、环境、risk、decision、SQL hash 和 tool 查询 MCP SQL Gateway 审计事件。'
                        actions={
                            <AuditFilterSheet
                                filters={filters}
                                onSearchChange={onSearchChange}
                            />
                        }
                    />

                    <AuditFilterChips filters={filters} onSearchChange={onSearchChange}/>

                    {auditQuery.isPending && <AuditLoading/>}

                    {auditQuery.isError && (
                        <Alert variant='destructive'>
                            <RefreshCw/>
                            <AlertTitle>审计列表加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(auditQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {auditQuery.isSuccess && auditQuery.data.content.length > 0 && (
                        <div className='overflow-hidden rounded-md border bg-card'>
                            <div className='overflow-x-auto'>
                                <AuditTable rows={auditQuery.data.content}/>
                            </div>
                            <AuditPagination
                                page={auditQuery.data.page}
                                size={auditQuery.data.size}
                                totalElements={auditQuery.data.totalElements}
                                totalPages={auditQuery.data.totalPages}
                                onPageChange={updatePage}
                            />
                        </div>
                    )}

                    {auditQuery.isSuccess && auditQuery.data.content.length === 0 && (
                        <EmptyState
                            icon={<ScrollText className='size-5'/>}
                            title='当前筛选条件下没有审计事件'
                            description='调整筛选条件，或等待新的 MCP SQL 请求进入 DBFlow 网关。'
                        />
                    )}
                </section>
            </Main>
        </>
    )
}

function AuditLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载审计列表'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 8}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function AuditPagination({
                             page,
                             size,
                             totalElements,
                             totalPages,
                             onPageChange,
                         }: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    onPageChange: (page: number) => void
}) {
    const firstItem = totalElements === 0 ? 0 : page * size + 1
    const lastItem = Math.min((page + 1) * size, totalElements)
    const displayTotalPages = totalPages === 0 ? 1 : totalPages
    const hasPrevious = page > 0
    const hasNext = page + 1 < totalPages

    return (
        <div className='flex flex-col gap-3 border-t px-4 py-3 text-sm md:flex-row md:items-center md:justify-between'>
      <span className='text-muted-foreground'>
        共 {formatNumber(totalElements)} 条，当前显示 {formatNumber(firstItem)}-
          {formatNumber(lastItem)}，第 {formatNumber(page + 1)}/
          {formatNumber(displayTotalPages)} 页
      </span>
            <div className='flex gap-2'>
                <Button
                    type='button'
                    variant='outline'
                    size='sm'
                    disabled={!hasPrevious}
                    onClick={() => onPageChange(page - 1)}
                >
                    <ChevronLeft className='size-4'/>
                    上一页
                </Button>
                <Button
                    type='button'
                    variant='outline'
                    size='sm'
                    disabled={!hasNext}
                    onClick={() => onPageChange(page + 1)}
                >
                    下一页
                    <ChevronRight className='size-4'/>
                </Button>
            </div>
        </div>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '审计列表加载失败'
}
