import {getStatusBadgeMeta} from '@/lib/badges'
import {cn} from '@/lib/utils'
import {Badge} from '@/components/ui/badge'

type StatusBadgeProps = {
    status: string | null | undefined
    className?: string
}

export function StatusBadge({status, className}: StatusBadgeProps) {
    const meta = getStatusBadgeMeta(status)

    return (
        <Badge variant='outline' className={cn(meta.className, className)}>
            <span className='size-1.5 rounded-full bg-current'/>
            {meta.label}
        </Badge>
    )
}
