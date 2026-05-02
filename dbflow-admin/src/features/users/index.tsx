import {type FormEvent, useEffect, useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {RefreshCw, Search as SearchIcon, UserRound} from 'lucide-react'
import {fetchUsers, usersQueryKey} from '@/api/users'
import type {UserFilters} from '@/types/access'
import {isApiClientError} from '@/lib/errors'
import {cn} from '@/lib/utils'
import {ConfigDrawer} from '@/components/config-drawer'
import {EmptyState} from '@/components/dbflow/empty-state'
import {PageHeader} from '@/components/dbflow/page-header'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {Skeleton} from '@/components/ui/skeleton'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {CreateUserSheet} from './components/create-user-sheet'
import {UsersTable} from './components/users-table'

export type UsersPageSearch = UserFilters

type UsersPageProps = {
    search: UsersPageSearch
    onSearchChange: (search: UsersPageSearch) => void
}

export function UsersPage({search, onSearchChange}: UsersPageProps) {
    const filters = normalizeSearch(search)
    const [username, setUsername] = useState(filters.username ?? '')
    const [status, setStatus] = useState(filters.status ?? '')

    useEffect(() => {
        setUsername(filters.username ?? '')
        setStatus(filters.status ?? '')
    }, [filters.username, filters.status])

    const usersQuery = useQuery({
        queryKey: usersQueryKey(filters),
        queryFn: () => fetchUsers(filters),
    })

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        onSearchChange(
            normalizeSearch({
                username,
                status,
            })
        )
    }

    function handleReset() {
        setUsername('')
        setStatus('')
        onSearchChange({})
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
                        eyebrow='身份与访问'
                        title='用户管理'
                        description='管理 DBFlow 操作员账户、访问状态、授权数量与活跃 MCP Token。'
                        actions={<CreateUserSheet/>}
                    />

                    <form
                        className='grid gap-3 rounded-md border bg-card/50 p-4 md:grid-cols-[minmax(220px,1fr)_180px_auto] md:items-end'
                        onSubmit={handleSubmit}
                    >
                        <div className='grid gap-2'>
                            <Label htmlFor='users-filter-username'>筛选用户名</Label>
                            <Input
                                id='users-filter-username'
                                value={username}
                                placeholder='输入用户名关键字'
                                onChange={(event) => setUsername(event.target.value)}
                            />
                        </div>
                        <div className='grid gap-2'>
                            <Label htmlFor='users-filter-status'>状态</Label>
                            <select
                                id='users-filter-status'
                                value={status}
                                className={cn(
                                    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors',
                                    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
                                )}
                                onChange={(event) => setStatus(event.target.value)}
                            >
                                <option value=''>全部状态</option>
                                <option value='ACTIVE'>ACTIVE</option>
                                <option value='DISABLED'>DISABLED</option>
                            </select>
                        </div>
                        <div className='flex gap-2'>
                            <Button type='submit' className='flex-1 md:flex-none'>
                                <SearchIcon className='size-4'/>
                                应用筛选
                            </Button>
                            <Button
                                type='button'
                                variant='outline'
                                className='flex-1 md:flex-none'
                                onClick={handleReset}
                            >
                                重置
                            </Button>
                        </div>
                    </form>

                    {usersQuery.isPending && <UsersLoading/>}

                    {usersQuery.isError && (
                        <Alert variant='destructive'>
                            <RefreshCw/>
                            <AlertTitle>用户列表加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(usersQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {usersQuery.isSuccess && usersQuery.data.length > 0 && (
                        <div className='rounded-md border bg-card'>
                            <UsersTable users={usersQuery.data}/>
                        </div>
                    )}

                    {usersQuery.isSuccess && usersQuery.data.length === 0 && (
                        <EmptyState
                            icon={<UserRound className='size-5'/>}
                            title='没有匹配的用户'
                            description='调整筛选条件，或创建新的 DBFlow 管理用户。'
                        />
                    )}
                </section>
            </Main>
        </>
    )
}

function UsersLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载用户列表'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 5}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function normalizeSearch(search: UserFilters): UserFilters {
    return {
        username: cleanOptionalString(search.username),
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

    return error instanceof Error ? error.message : '用户列表加载失败'
}
