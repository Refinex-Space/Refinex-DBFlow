import {Fragment, type ReactNode} from 'react'
import {ChevronRight} from 'lucide-react'
import type {DbflowBreadcrumbItem} from '@/lib/routes'
import {cn} from '@/lib/utils'

type PageBreadcrumbProps = {
    items: DbflowBreadcrumbItem[]
    actions?: ReactNode
    className?: string
}

export function PageBreadcrumb({items, actions, className}: PageBreadcrumbProps) {
    return (
        <div
            className={cn(
                'flex flex-col gap-3 border-b pb-3 md:flex-row md:items-center md:justify-between',
                className
            )}
        >
            <nav aria-label='页面路径' className='min-w-0'>
                <ol className='flex min-w-0 items-center gap-1 text-sm font-medium text-muted-foreground'>
                    {items.map((item, index) => {
                        const isLast = index === items.length - 1

                        return (
                            <Fragment key={`${item.title}-${index}`}>
                                {index > 0 && (
                                    <li aria-hidden='true' className='flex shrink-0 items-center'>
                                        <ChevronRight className='size-3.5'/>
                                    </li>
                                )}
                                <li className='min-w-0'>
                                    {item.href && !isLast ? (
                                        <a
                                            href={item.href}
                                            className='truncate transition-colors hover:text-foreground'
                                        >
                                            {item.title}
                                        </a>
                                    ) : (
                                        <span
                                            aria-current={isLast ? 'page' : undefined}
                                            className={cn(
                                                'truncate',
                                                isLast && 'text-foreground'
                                            )}
                                        >
                                            {item.title}
                                        </span>
                                    )}
                                </li>
                            </Fragment>
                        )
                    })}
                </ol>
            </nav>
            {actions && (
                <div className='flex shrink-0 flex-wrap items-center gap-2'>{actions}</div>
            )}
        </div>
    )
}
