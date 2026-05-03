import {useState} from 'react'
import {useQuery} from '@tanstack/react-query'
import type {PolicyDefaultRow, PolicyReason, PolicyWhitelistRow,} from '@/types/policy'
import {FileWarning, RefreshCw, ShieldAlert} from 'lucide-react'
import {dangerousPoliciesQueryKey, fetchDangerousPolicies,} from '@/api/policies'
import {isApiClientError} from '@/lib/errors'
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
import {DropWhitelistTable} from './components/drop-whitelist-table'
import {PolicyDefaultsTable} from './components/policy-defaults-table'
import {PolicyReasonSheet} from './components/policy-reason-sheet'
import {PolicyRules} from './components/policy-rules'

export function DangerousPoliciesPageView() {
    const [reason, setReason] = useState<PolicyReason | null>(null)
    const policiesQuery = useQuery({
        queryKey: dangerousPoliciesQueryKey,
        queryFn: fetchDangerousPolicies,
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
                <section className='space-y-6'>
                    <PageHeader
                        eyebrow='配置与策略'
                        title='危险策略'
                        description='只读查看 DBFlow 对 DROP 与 TRUNCATE 等高危操作的服务端策略。'
                        actions={
                            <div className='flex flex-wrap gap-2'>
                                <Button type='button' variant='outline' asChild>
                                    <a href='/audit?decision=POLICY_DENIED'>
                                        <FileWarning className='size-4'/>
                                        查看被拒绝审计
                                    </a>
                                </Button>
                                <Button
                                    type='button'
                                    variant='outline'
                                    disabled={policiesQuery.isFetching}
                                    onClick={() => void policiesQuery.refetch()}
                                >
                                    <RefreshCw
                                        className={
                                            policiesQuery.isFetching
                                                ? 'size-4 animate-spin'
                                                : 'size-4'
                                        }
                                    />
                                    刷新策略
                                </Button>
                            </div>
                        }
                    />

                    {policiesQuery.isPending && <PoliciesLoading/>}

                    {policiesQuery.isError && (
                        <Alert variant='destructive'>
                            <ShieldAlert/>
                            <AlertTitle>危险策略加载失败</AlertTitle>
                            <AlertDescription>
                                {errorMessage(policiesQuery.error)}
                            </AlertDescription>
                        </Alert>
                    )}

                    {policiesQuery.isSuccess && (
                        <>
                            <section className='space-y-3'>
                                <SectionTitle
                                    title='默认高危策略'
                                    description='未命中白名单或确认挑战时，后端按默认策略 fail-closed。'
                                />
                                <div className='rounded-md border bg-card'>
                                    <PolicyDefaultsTable
                                        rows={policiesQuery.data.defaults}
                                        onReasonSelect={(row) => setReason(defaultReason(row))}
                                    />
                                </div>
                            </section>

                            <section className='space-y-3'>
                                <SectionTitle
                                    title='DROP 白名单'
                                    description='仅展示 YAML/Nacos 当前生效的 DROP 白名单范围。'
                                />
                                {policiesQuery.data.whitelist.length > 0 ? (
                                    <div className='rounded-md border bg-card'>
                                        <DropWhitelistTable
                                            rows={policiesQuery.data.whitelist}
                                            onReasonSelect={(row) => setReason(whitelistReason(row))}
                                        />
                                    </div>
                                ) : (
                                    <EmptyState
                                        icon={<ShieldAlert className='size-5'/>}
                                        title='当前无 DROP 白名单条目'
                                        description={
                                            policiesQuery.data.emptyHint ||
                                            'DROP_DATABASE / DROP_TABLE 将按默认策略拒绝。'
                                        }
                                    />
                                )}
                            </section>

                            <section className='space-y-3'>
                                <SectionTitle
                                    title='固定强化规则'
                                    description='这些规则由服务端强制执行，React 页面只读展示。'
                                />
                                <PolicyRules rules={policiesQuery.data.rules}/>
                            </section>
                        </>
                    )}
                </section>
            </Main>

            <PolicyReasonSheet
                reason={reason}
                open={reason !== null}
                onOpenChange={(open) => {
                    if (!open) {
                        setReason(null)
                    }
                }}
            />
        </>
    )
}

function SectionTitle({
                          title,
                          description,
                      }: {
    title: string
    description: string
}) {
    return (
        <div className='space-y-1'>
            <h2 className='text-base font-semibold'>{title}</h2>
            <p className='text-sm text-muted-foreground'>{description}</p>
        </div>
    )
}

function PoliciesLoading() {
    return (
        <div
            role='status'
            aria-label='正在加载危险策略'
            className='space-y-2 rounded-md border bg-card p-4'
        >
            {Array.from({length: 6}).map((_, index) => (
                <Skeleton key={index} className='h-10 w-full'/>
            ))}
        </div>
    )
}

function defaultReason(row: PolicyDefaultRow): PolicyReason {
    return {
        kind: 'default',
        title: row.operation,
        risk: row.risk,
        decision: row.decision,
        requirement: row.requirement,
    }
}

function whitelistReason(row: PolicyWhitelistRow): PolicyReason {
    return {
        kind: 'whitelist',
        title: `${row.operation} ${row.project}/${row.env}`,
        risk: row.risk,
        scope: [row.project, row.env, row.schema, row.table].join(' / '),
        allowProd: row.allowProd,
        prodRule: row.prodRule,
    }
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '危险策略加载失败'
}
