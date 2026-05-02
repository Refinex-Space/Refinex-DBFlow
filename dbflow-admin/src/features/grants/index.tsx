import {useQuery} from '@tanstack/react-query'
import {KeyRound, RefreshCw, TriangleAlert} from 'lucide-react'
import {fetchGrantGroups, fetchGrantOptions, grantOptionsQueryKey, grantsQueryKey,} from '@/api/grants'
import type {GrantFilters} from '@/types/access'
import {isApiClientError} from '@/lib/errors'
import {ConfigDrawer} from '@/components/config-drawer'
import {EmptyState} from '@/components/dbflow/empty-state'
import {PageHeader} from '@/components/dbflow/page-header'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Skeleton} from '@/components/ui/skeleton'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {CreateGrantSheet} from './components/create-grant-sheet'
import {GrantFilterBar} from './components/grant-filter-bar'
import {GrantsTable} from './components/grants-table'

export type GrantsPageSearch = GrantFilters

type GrantsPageProps = {
    search: GrantsPageSearch
    onSearchChange: (search: GrantsPageSearch) => void
}

export function GrantsPage({search, onSearchChange}: GrantsPageProps) {
    const filters = normalizeSearch(search)
    const grantsQuery = useQuery({
        queryKey: grantsQueryKey(filters),
        queryFn: () => fetchGrantGroups(filters),
    })
    const optionsQuery = useQuery({
        queryKey: grantOptionsQueryKey,
        queryFn: fetchGrantOptions,
    })
    const environmentOptions = optionsQuery.data?.environments ?? []
    const noConfiguredEnvironments =
        optionsQuery.isSuccess && environmentOptions.length === 0

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
                        eyebrow='身份与访问'
                        title='项目授权'
                        description='按用户和项目管理可访问的 DBFlow 环境边界。'
                        actions={<CreateGrantSheet options={optionsQuery.data}/>}
                    />

                    {noConfiguredEnvironments && (
                        <Alert>
                            <TriangleAlert/>
                            <AlertTitle>尚未配置可授权项目环境</AlertTitle>
                            <AlertDescription>
                                请检查外部配置中的 dbflow.projects，并确认项目环境已同步到元数据。
                            </AlertDescription>
                        </Alert>
                    )}

                    <GrantFilterBar search={filters} onSearchChange={onSearchChange}/>

                    {(grantsQuery.isPending || optionsQuery.isPending) && (
                        <GrantsLoading/>
                    )}

                    {(grantsQuery.isError || optionsQuery.isError) && (
                        <Alert variant='destructive'>
                            <RefreshCw/>
                            <AlertTitle>项目授权加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(grantsQuery.error ?? optionsQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {grantsQuery.isSuccess &&
                        optionsQuery.isSuccess &&
                        grantsQuery.data.length > 0 && (
                            <div className='rounded-md border bg-card'>
                                <GrantsTable
                                    rows={grantsQuery.data}
                                    environmentOptions={environmentOptions}
                                />
                            </div>
                        )}

                    {grantsQuery.isSuccess &&
                        optionsQuery.isSuccess &&
                        grantsQuery.data.length === 0 && (
                            <EmptyState
                                icon={<KeyRound className='size-5'/>}
                                title='当前没有项目授权'
                                description='调整筛选条件，或为用户新建项目环境授权。'
                            />
                        )}
                </section>
            </Main>
        </>
    )
}

function GrantsLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载项目授权'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 5}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function normalizeSearch(search: GrantFilters): GrantFilters {
    return {
        username: cleanOptionalString(search.username),
        projectKey: cleanOptionalString(search.projectKey),
        environmentKey: cleanOptionalString(search.environmentKey),
        status: cleanOptionalString(search.status),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '项目授权加载失败'
}
