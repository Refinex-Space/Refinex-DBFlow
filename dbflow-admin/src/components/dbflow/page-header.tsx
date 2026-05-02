import type {ReactNode} from 'react'
import {cn} from '@/lib/utils'

type PageHeaderProps = {
    title: ReactNode
    description?: ReactNode
    eyebrow?: ReactNode
    actions?: ReactNode
    className?: string
}

export function PageHeader({
                               title,
                               description,
                               eyebrow,
                               actions,
                               className,
                           }: PageHeaderProps) {
    return (
        <div
            className={cn(
                'flex flex-col gap-3 border-b pb-4 md:flex-row md:items-end md:justify-between',
                className
            )}
        >
            <div className='min-w-0 space-y-1'>
                {eyebrow && (
                    <p className='text-xs font-medium text-muted-foreground'>{eyebrow}</p>
                )}
                <h1 className='truncate text-2xl font-semibold tracking-tight'>
                    {title}
                </h1>
                {description && (
                    <p className='max-w-3xl text-sm text-muted-foreground'>
                        {description}
                    </p>
                )}
            </div>
            {actions && (
                <div className='flex shrink-0 items-center gap-2'>{actions}</div>
            )}
        </div>
    )
}
