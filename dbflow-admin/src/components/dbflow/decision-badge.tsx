import {getDecisionBadgeMeta} from '@/lib/badges'
import {cn} from '@/lib/utils'
import {Badge} from '@/components/ui/badge'

type DecisionBadgeProps = {
    decision: string | null | undefined
    className?: string
}

export function DecisionBadge({decision, className}: DecisionBadgeProps) {
    const meta = getDecisionBadgeMeta(decision)

    return (
        <Badge variant='outline' className={cn(meta.className, className)}>
            <span className='size-1.5 rounded-full bg-current'/>
            {meta.label}
        </Badge>
    )
}
