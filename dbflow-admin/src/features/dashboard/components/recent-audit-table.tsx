import type {RecentAuditRow} from '@/types/overview'
import {ExternalLink} from 'lucide-react'
import {formatText} from '@/lib/format'
import {Button} from '@/components/ui/button'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {DecisionBadge} from '@/components/dbflow/decision-badge'
import {EmptyState} from '@/components/dbflow/empty-state'
import {EnvBadge} from '@/components/dbflow/env-badge'
import {RiskBadge} from '@/components/dbflow/risk-badge'

type RecentAuditTableProps = {
    rows: RecentAuditRow[]
}

export function RecentAuditTable({rows}: RecentAuditTableProps) {
    return (
        <section className='space-y-3'>
            <div className='flex items-center justify-between gap-3'>
                <div>
                    <h2 className='text-base font-semibold'>最近审计事件</h2>
                    <p className='text-sm text-muted-foreground'>
                        按创建时间倒序展示最近 5 条审计记录
                    </p>
                </div>
                <Button variant='ghost' size='sm' asChild>
                    <a href='/admin/audit'>查看全部</a>
                </Button>
            </div>

            {rows.length === 0 ? (
                <EmptyState
                    title='当前暂无审计事件。'
                    description='当 MCP SQL 请求进入网关后，最近的执行、拒绝和确认事件会显示在这里。'
                    className='py-8'
                />
            ) : (
                <div className='rounded-md border'>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>时间</TableHead>
                                <TableHead>用户</TableHead>
                                <TableHead>项目 / 环境</TableHead>
                                <TableHead>Risk</TableHead>
                                <TableHead>Decision</TableHead>
                                <TableHead>SQL Hash</TableHead>
                                <TableHead className='text-right'>操作</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {rows.map((row) => (
                                <TableRow key={row.id}>
                                    <TableCell className='font-mono text-xs'>
                                        {formatText(row.time)}
                                    </TableCell>
                                    <TableCell>{formatText(row.user)}</TableCell>
                                    <TableCell>
                                        <span className='mr-2'>{projectEnvLabel(row)}</span>
                                        <EnvBadge environment={row.env}/>
                                    </TableCell>
                                    <TableCell>
                                        <RiskBadge risk={row.risk}/>
                                    </TableCell>
                                    <TableCell>
                                        <DecisionBadge decision={row.decision}/>
                                    </TableCell>
                                    <TableCell className='max-w-[180px] truncate font-mono text-xs'>
                                        {formatText(row.sqlHash)}
                                    </TableCell>
                                    <TableCell className='text-right'>
                                        <Button variant='ghost' size='sm' asChild>
                                            <a href={`/admin/audit/${row.id}`}>
                                                <ExternalLink/>
                                                详情
                                            </a>
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>
            )}
        </section>
    )
}

function projectEnvLabel(row: RecentAuditRow) {
    return `${formatText(row.project)} / ${formatText(row.env)}`
}
