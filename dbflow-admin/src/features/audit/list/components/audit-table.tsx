import type {AuditEventSummary} from '@/types/audit'
import {ExternalLink} from 'lucide-react'
import {formatDateTime, formatText} from '@/lib/format'
import {Button} from '@/components/ui/button'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {CopyButton} from '@/components/dbflow/copy-button'
import {DecisionBadge} from '@/components/dbflow/decision-badge'
import {EnvBadge} from '@/components/dbflow/env-badge'
import {RiskBadge} from '@/components/dbflow/risk-badge'

type AuditTableProps = {
    rows: AuditEventSummary[]
}

export function AuditTable({rows}: AuditTableProps) {
    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>时间</TableHead>
                    <TableHead>用户</TableHead>
                    <TableHead>项目 / 环境</TableHead>
                    <TableHead>Tool</TableHead>
                    <TableHead>Operation</TableHead>
                    <TableHead>Risk</TableHead>
                    <TableHead>Decision</TableHead>
                    <TableHead>SQL Hash</TableHead>
                    <TableHead>结果摘要</TableHead>
                    <TableHead className='text-end'>详情</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map((row) => (
                    <TableRow key={row.id}>
                        <TableCell className='font-mono text-xs whitespace-nowrap text-muted-foreground'>
                            {formatDateTime(row.createdAt)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.userId)}
                        </TableCell>
                        <TableCell>
                            <div className='flex min-w-36 flex-wrap items-center gap-2'>
                                <span>{projectEnvLabel(row)}</span>
                                <EnvBadge environment={row.environmentKey}/>
                            </div>
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.tool)}
                        </TableCell>
                        <TableCell>{formatText(row.operationType)}</TableCell>
                        <TableCell>
                            <RiskBadge risk={row.riskLevel}/>
                        </TableCell>
                        <TableCell>
                            <DecisionBadge decision={row.decision}/>
                        </TableCell>
                        <TableCell>
                            <div className='flex min-w-48 items-center gap-2'>
                <span className='max-w-40 truncate font-mono text-xs'>
                  {formatText(row.sqlHash)}
                </span>
                                {row.sqlHash && (
                                    <CopyButton
                                        value={row.sqlHash}
                                        label='复制'
                                        ariaLabel={`复制 SQL Hash ${row.sqlHash}`}
                                        successMessage='SQL Hash 已复制'
                                        className='h-7 px-2'
                                    />
                                )}
                            </div>
                        </TableCell>
                        <TableCell className='max-w-72 truncate'>
                            {formatText(row.resultSummary)}
                        </TableCell>
                        <TableCell className='text-end'>
                            <Button variant='ghost' size='sm' asChild>
                                <a href={auditDetailHref(row.id)}>
                                    <ExternalLink className='size-4'/>
                                    {row.decision === 'POLICY_DENIED'
                                        ? '查看拒绝原因'
                                        : '查看详情'}
                                </a>
                            </Button>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}

function projectEnvLabel(row: AuditEventSummary) {
    return `${formatText(row.projectKey)} / ${formatText(row.environmentKey)}`
}

function auditDetailHref(eventId: number) {
    const baseUrl = import.meta.env.BASE_URL.replace(/\/+$/, '')
    return `${baseUrl}/audit/${eventId}`
}
