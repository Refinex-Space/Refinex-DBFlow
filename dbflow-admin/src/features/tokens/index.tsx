import {type FormEvent, useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import {KeyRound, RefreshCw, Search as SearchIcon} from 'lucide-react'
import {fetchTokenOptions, fetchTokens, tokenOptionsQueryKey, tokensQueryKey,} from '@/api/tokens'
import type {IssuedTokenResponse, TokenFilters} from '@/types/token'
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
import {IssueTokenSheet} from './components/issue-token-sheet'
import {TokenRevealDialog} from './components/token-reveal-dialog'
import {TokensTable} from './components/tokens-table'

export type TokensPageSearch = TokenFilters

type TokensPageProps = {
    search: TokensPageSearch
    onSearchChange: (search: TokensPageSearch) => void
}

export function TokensPage({search, onSearchChange}: TokensPageProps) {
    const filters = normalizeSearch(search)
    const [revealedToken, setRevealedToken] = useState<IssuedTokenResponse | null>(null)
    const formDefaults = {
        username: filters.username ?? '',
        status: filters.status ?? '',
    }

    const tokensQuery = useQuery({
        queryKey: tokensQueryKey(filters),
        queryFn: () => fetchTokens(filters),
    })
    const optionsQuery = useQuery({
        queryKey: tokenOptionsQueryKey,
        queryFn: fetchTokenOptions,
    })

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        const data = new FormData(event.currentTarget)
        onSearchChange(
            normalizeSearch({
                username: formValue(data, 'username'),
                status: formValue(data, 'status'),
            })
        )
    }

    function handleReset() {
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
                        title='Token 管理'
                        description='管理 DBFlow MCP SQL Gateway 的访问 Token，并确保明文只在颁发后一次性展示。'
                        actions={
                            <IssueTokenSheet
                                options={optionsQuery.data}
                                onTokenIssued={setRevealedToken}
                            />
                        }
                    />

                    <form
                        key={JSON.stringify(formDefaults)}
                        className='grid gap-3 rounded-md border bg-card/50 p-4 md:grid-cols-[minmax(220px,1fr)_180px_auto] md:items-end'
                        onSubmit={handleSubmit}
                    >
                        <div className='grid gap-2'>
                            <Label htmlFor='tokens-filter-username'>用户名</Label>
                            <Input
                                id='tokens-filter-username'
                                name='username'
                                defaultValue={formDefaults.username}
                                placeholder='输入用户名关键字'
                            />
                        </div>
                        <div className='grid gap-2'>
                            <Label htmlFor='tokens-filter-status'>状态</Label>
                            <select
                                id='tokens-filter-status'
                                name='status'
                                defaultValue={formDefaults.status}
                                className={selectClassName}
                            >
                                <option value=''>全部状态</option>
                                <option value='ACTIVE'>ACTIVE</option>
                                <option value='REVOKED'>REVOKED</option>
                                <option value='EXPIRED'>EXPIRED</option>
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

                    {(tokensQuery.isPending || optionsQuery.isPending) && <TokensLoading/>}

                    {(tokensQuery.isError || optionsQuery.isError) && (
                        <Alert variant='destructive'>
                            <RefreshCw/>
                            <AlertTitle>Token 列表加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(tokensQuery.error ?? optionsQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {tokensQuery.isSuccess &&
                        optionsQuery.isSuccess &&
                        tokensQuery.data.length > 0 && (
                            <div className='rounded-md border bg-card'>
                                <TokensTable
                                    tokens={tokensQuery.data}
                                    onTokenIssued={setRevealedToken}
                                />
                            </div>
                        )}

                    {tokensQuery.isSuccess &&
                        optionsQuery.isSuccess &&
                        tokensQuery.data.length === 0 && (
                            <EmptyState
                                icon={<KeyRound className='size-5'/>}
                                title='没有匹配的 Token'
                                description='调整筛选条件，或为 active 用户颁发新的 MCP Token。'
                            />
                        )}
                </section>
            </Main>

            <TokenRevealDialog
                token={revealedToken}
                open={revealedToken !== null}
                onOpenChange={(nextOpen) => {
                    if (!nextOpen) {
                        setRevealedToken(null)
                    }
                }}
            />
        </>
    )
}

function TokensLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载 Token 列表'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 5}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function normalizeSearch(search: TokenFilters): TokenFilters {
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

    return error instanceof Error ? error.message : 'Token 列表加载失败'
}

function formValue(data: FormData, name: keyof TokenFilters): string {
    return String(data.get(name) ?? '')
}

const selectClassName = cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors',
    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
)
