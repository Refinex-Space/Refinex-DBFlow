import {useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {Database, RefreshCw} from 'lucide-react'
import {configQueryKey, fetchConfigPage} from '@/api/config'
import type {ConfigRow} from '@/types/config'
import {isApiClientError} from '@/lib/errors'
import {formatText} from '@/lib/format'
import {dbflowBreadcrumbs} from '@/lib/routes'
import {ConfigDrawer} from '@/components/config-drawer'
import {EmptyState} from '@/components/dbflow/empty-state'
import {PageBreadcrumb} from '@/components/dbflow/page-breadcrumb'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Skeleton} from '@/components/ui/skeleton'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {ConfigDetailSheet} from './components/config-detail-sheet'
import {ConfigTable} from './components/config-table'

export function ConfigPageView() {
    const [selectedRow, setSelectedRow] = useState<ConfigRow | null>(null)
    const configQuery = useQuery({
        queryKey: configQueryKey,
        queryFn: fetchConfigPage,
    })
    const sourceLabel = formatText(configQuery.data?.sourceLabel)

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
                        items={dbflowBreadcrumbs.config}
                        actions={
                            <Button
                                type='button'
                                variant='outline'
                                disabled={configQuery.isFetching}
                                onClick={() => void configQuery.refetch()}
                            >
                                <RefreshCw
                                    className={configQuery.isFetching ? 'size-4 animate-spin' : 'size-4'}
                                />
                                刷新配置
                            </Button>
                        }
                    />

                    {configQuery.isPending && <ConfigLoading/>}

                    {configQuery.isError && (
                        <Alert variant='destructive'>
                            <RefreshCw/>
                            <AlertTitle>配置加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(configQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {configQuery.isSuccess && (
                        <>
                            <div className='rounded-md border bg-card/50 p-4'>
                                <div className='text-sm text-muted-foreground'>配置来源</div>
                                <div className='mt-1 font-medium'>{sourceLabel}</div>
                            </div>

                            {configQuery.data.rows.length > 0 ? (
                                <div className='rounded-md border bg-card'>
                                    <ConfigTable
                                        rows={configQuery.data.rows}
                                        onRowSelect={setSelectedRow}
                                    />
                                </div>
                            ) : (
                                <EmptyState
                                    icon={<Database className='size-5'/>}
                                    title='当前未配置 dbflow.projects。'
                                />
                            )}
                        </>
                    )}
                </section>
            </Main>

            <ConfigDetailSheet
                row={selectedRow}
                open={selectedRow !== null}
                onOpenChange={(open) => {
                    if (!open) {
                        setSelectedRow(null)
                    }
                }}
            />
        </>
    )
}

function ConfigLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载配置'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 5}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '配置加载失败'
}
