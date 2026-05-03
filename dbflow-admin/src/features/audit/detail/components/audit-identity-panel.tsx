import type {ReactNode} from 'react'
import type {AuditEventDetail} from '@/types/audit'
import {formatDateTime, formatNumber, formatText} from '@/lib/format'
import {Card, CardContent, CardHeader, CardTitle,} from '@/components/ui/card'
import {EnvBadge} from '@/components/dbflow/env-badge'
import {RiskBadge} from '@/components/dbflow/risk-badge'

type AuditIdentityPanelProps = {
    detail: AuditEventDetail
}

export function AuditIdentityPanel({detail}: AuditIdentityPanelProps) {
    return (
        <Card className='rounded-md shadow-none'>
            <CardHeader>
                <CardTitle className='text-base'>请求身份</CardTitle>
            </CardHeader>
            <CardContent>
                <dl className='grid gap-4 md:grid-cols-2 xl:grid-cols-3'>
                    <DetailItem
                        label='请求时间'
                        value={formatDateTime(detail.createdAt)}
                    />
                    <DetailItem label='用户' value={formatText(detail.userId)}/>
                    <DetailItem
                        label='project / env'
                        value={
                            <span className='inline-flex items-center gap-2'>
                {formatText(detail.projectKey)} /{' '}
                                {formatText(detail.environmentKey)}
                                <EnvBadge environment={detail.environmentKey}/>
              </span>
                        }
                    />
                    <DetailItem label='tool' value={formatText(detail.tool)}/>
                    <DetailItem
                        label='operation'
                        value={formatText(detail.operationType)}
                    />
                    <DetailItem
                        label='risk'
                        value={<RiskBadge risk={detail.riskLevel}/>}
                    />
                    <DetailItem label='sqlHash' value={formatText(detail.sqlHash)} mono/>
                    <DetailItem
                        label='requestId'
                        value={formatText(detail.requestId)}
                        mono
                    />
                    <DetailItem
                        label='client'
                        value={`${formatText(detail.clientName)} ${formatText(detail.clientVersion)}`}
                    />
                    <DetailItem
                        label='sourceIp'
                        value={formatText(detail.sourceIp)}
                        mono
                    />
                    <DetailItem
                        label='affectedRows'
                        value={formatNumber(detail.affectedRows)}
                    />
                    <DetailItem
                        label='confirmationId'
                        value={formatText(detail.confirmationId)}
                        mono
                    />
                </dl>
            </CardContent>
        </Card>
    )
}

function DetailItem({
                        label,
                        value,
                        mono = false,
                    }: {
    label: string
    value: ReactNode
    mono?: boolean
}) {
    return (
        <div className='space-y-1'>
            <dt className='text-xs font-medium text-muted-foreground'>{label}</dt>
            <dd className={mono ? 'font-mono text-xs break-all' : 'text-sm'}>
                {value}
            </dd>
        </div>
    )
}
