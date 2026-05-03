import type {AuditEventDetail} from '@/types/audit'
import {CheckCircle2, CircleDot, ShieldCheck} from 'lucide-react'
import {formatDateTime, formatText} from '@/lib/format'
import {Card, CardContent, CardHeader, CardTitle,} from '@/components/ui/card'

type AuditTimelineProps = {
    detail: AuditEventDetail
}

export function AuditTimeline({detail}: AuditTimelineProps) {
    const items = [
        {
            title: 'request received',
            description: `${formatText(detail.tool)} accepted into DBFlow audit pipeline.`,
        },
        {
            title: 'authorization',
            description: `User ${formatText(detail.userId)} scoped to ${formatText(detail.projectKey)} / ${formatText(detail.environmentKey)}.`,
        },
        {
            title: 'classification',
            description: `${formatText(detail.operationType)} classified as ${formatText(detail.riskLevel)} risk.`,
        },
        {
            title: 'policy decision',
            description: `${formatText(detail.decision)} with status ${formatText(detail.status)}.`,
        },
        {
            title: 'audit persisted',
            description: `Audit #${detail.id} persisted at ${formatDateTime(detail.createdAt)}.`,
        },
    ]

    return (
        <Card className='rounded-md shadow-none'>
            <CardHeader>
                <CardTitle className='text-base'>审计时间线</CardTitle>
            </CardHeader>
            <CardContent>
                <ol className='space-y-4'>
                    {items.map((item, index) => {
                        const Icon =
                            index === items.length - 1
                                ? ShieldCheck
                                : index === 0
                                    ? CircleDot
                                    : CheckCircle2
                        return (
                            <li key={item.title} className='flex gap-3'>
                                <div
                                    className='mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full border bg-background text-muted-foreground'>
                                    <Icon className='size-4'/>
                                </div>
                                <div className='min-w-0 space-y-1'>
                                    <p className='text-sm font-medium'>{item.title}</p>
                                    <p className='text-sm text-muted-foreground'>
                                        {item.description}
                                    </p>
                                </div>
                            </li>
                        )
                    })}
                </ol>
            </CardContent>
        </Card>
    )
}
