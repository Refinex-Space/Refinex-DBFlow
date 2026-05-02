import type {ReactNode} from 'react'
import {Inbox} from 'lucide-react'
import {cn} from '@/lib/utils'
import {Card} from '@/components/ui/card'

type EmptyStateProps = {
    title: ReactNode
    description?: ReactNode
    icon?: ReactNode
    action?: ReactNode
    className?: string
}

export function EmptyState({
                               title,
                               description,
                               icon,
                               action,
                               className,
                           }: EmptyStateProps) {
    return (
        <Card
            className={cn(
                'items-center gap-3 rounded-md px-6 py-10 text-center shadow-none',
                className
            )}
        >
            <div
                className='flex size-10 items-center justify-center rounded-md border bg-muted/40 text-muted-foreground'>
                {icon ?? <Inbox className='size-5'/>}
            </div>
            <div className='space-y-1'>
                <h2 className='text-sm font-semibold'>{title}</h2>
                {description && (
                    <p className='max-w-md text-sm text-muted-foreground'>
                        {description}
                    </p>
                )}
            </div>
            {action && <div className='pt-1'>{action}</div>}
        </Card>
    )
}
