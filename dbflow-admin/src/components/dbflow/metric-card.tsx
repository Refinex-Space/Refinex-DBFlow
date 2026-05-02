import type {ReactNode} from 'react'
import {cn} from '@/lib/utils'
import {Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'

type MetricCardProps = {
    title: ReactNode
    value: ReactNode
    description?: ReactNode
    action?: ReactNode
    footer?: ReactNode
    className?: string
}

export function MetricCard({
                               title,
                               value,
                               description,
                               action,
                               footer,
                               className,
                           }: MetricCardProps) {
    return (
        <Card className={cn('gap-3 rounded-md py-4 shadow-none', className)}>
            <CardHeader className='gap-1 px-4'>
                <CardTitle className='text-xs font-medium text-muted-foreground'>
                    {title}
                </CardTitle>
                {action && <CardAction>{action}</CardAction>}
            </CardHeader>
            <CardContent className='space-y-1 px-4'>
                <div className='text-2xl font-semibold tabular-nums'>{value}</div>
                {description && (
                    <CardDescription className='text-xs'>{description}</CardDescription>
                )}
                {footer && (
                    <div className='pt-1 text-xs text-muted-foreground'>{footer}</div>
                )}
            </CardContent>
        </Card>
    )
}
