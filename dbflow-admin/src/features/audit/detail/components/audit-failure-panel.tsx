import type {AuditEventDetail} from '@/types/audit'
import {formatText} from '@/lib/format'
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'
import {DecisionBadge} from '@/components/dbflow/decision-badge'
import {StatusBadge} from '@/components/dbflow/status-badge'

type AuditFailurePanelProps = {
    detail: AuditEventDetail
}

export function AuditFailurePanel({detail}: AuditFailurePanelProps) {
    return (
        <Card className='rounded-md shadow-none'>
            <CardHeader>
                <CardTitle className='text-base'>拒绝 / 失败原因</CardTitle>
                <CardDescription>
                    状态、错误码、失败原因和后端脱敏后的结果摘要。
                </CardDescription>
            </CardHeader>
            <CardContent className='grid gap-4 md:grid-cols-2'>
                <div className='space-y-1'>
                    <p className='text-xs font-medium text-muted-foreground'>status</p>
                    <StatusBadge status={detail.status}/>
                </div>
                <div className='space-y-1'>
                    <p className='text-xs font-medium text-muted-foreground'>decision</p>
                    <DecisionBadge decision={detail.decision}/>
                </div>
                <TextBlock label='errorCode' value={detail.errorCode}/>
                <TextBlock label='failureReason' value={detail.errorMessage}/>
                <TextBlock
                    label='resultSummary'
                    value={detail.resultSummary}
                    className='md:col-span-2'
                />
            </CardContent>
        </Card>
    )
}

function TextBlock({
                       label,
                       value,
                       className,
                   }: {
    label: string
    value: string | null | undefined
    className?: string
}) {
    return (
        <div className={className}>
            <p className='mb-1 text-xs font-medium text-muted-foreground'>{label}</p>
            <p className='rounded-md border bg-muted/30 p-3 text-sm break-words'>
                {formatText(value)}
            </p>
        </div>
    )
}
