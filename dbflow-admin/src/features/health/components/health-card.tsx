import type {HealthItem} from '@/types/health'
import {Activity, Database, Network, Server, Settings2} from 'lucide-react'
import {formatText} from '@/lib/format'
import {cn} from '@/lib/utils'
import {Badge} from '@/components/ui/badge'
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card'
import {StatusBadge} from '@/components/dbflow/status-badge'

type HealthCardProps = {
    item: HealthItem
}

export function HealthCard({item}: HealthCardProps) {
    const Icon = iconForComponent(item.component)

    return (
        <Card
            role='article'
            aria-label={`健康项 ${item.name}`}
            className='gap-4 rounded-md py-4 shadow-none'
        >
            <CardHeader className='gap-3 px-4'>
                <div className='flex items-start justify-between gap-3'>
                    <div className='flex min-w-0 items-start gap-3'>
                        <div
                            className='flex size-9 shrink-0 items-center justify-center rounded-md border bg-muted/40 text-muted-foreground'>
                            <Icon className='size-4'/>
                        </div>
                        <div className='min-w-0 space-y-1'>
                            <CardTitle className='truncate text-sm'>
                                {formatText(item.name)}
                            </CardTitle>
                            <div className='font-mono text-xs break-all text-muted-foreground'>
                                {formatText(item.component)}
                            </div>
                        </div>
                    </div>
                    <StatusBadge status={item.status} className='shrink-0'/>
                </div>
            </CardHeader>
            <CardContent className='space-y-3 px-4'>
                <p className='text-sm text-muted-foreground'>
                    {formatText(item.description)}
                </p>
                <div className='grid gap-2 text-xs'>
                    <DetailRow label='detail' value={item.detail}/>
                    <div className='flex items-center justify-between gap-3 rounded-md border bg-muted/30 px-3 py-2'>
                        <span className='text-muted-foreground'>tone</span>
                        <ToneBadge tone={item.tone}/>
                    </div>
                </div>
            </CardContent>
        </Card>
    )
}

function DetailRow({label, value}: { label: string; value: string }) {
    return (
        <div className='grid gap-1 rounded-md border bg-muted/30 px-3 py-2'>
            <span className='text-muted-foreground'>{label}</span>
            <span className='break-words'>{formatText(value)}</span>
        </div>
    )
}

function ToneBadge({tone}: { tone: string }) {
    const normalized = tone.trim().toLowerCase()
    const className =
        normalized === 'ok'
            ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
            : normalized === 'warn'
                ? 'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300'
                : normalized === 'bad'
                    ? 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300'
                    : 'border-muted-foreground/25 bg-muted/40 text-muted-foreground'

    return (
        <Badge variant='outline' className={cn('shrink-0', className)}>
            {formatText(tone)}
        </Badge>
    )
}

function iconForComponent(component: string) {
    const lowerComponent = component.toLowerCase()

    if (lowerComponent.includes('database')) {
        return Database
    }

    if (lowerComponent.includes('datasource')) {
        return Server
    }

    if (lowerComponent.includes('nacos')) {
        return Settings2
    }

    if (lowerComponent.includes('mcp')) {
        return Network
    }

    return Activity
}
