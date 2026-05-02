import {getRiskBadgeMeta} from '@/lib/badges'
import {cn} from '@/lib/utils'
import {Badge} from '@/components/ui/badge'

type RiskBadgeProps = {
    risk: string | null | undefined
    className?: string
}

export function RiskBadge({risk, className}: RiskBadgeProps) {
    const meta = getRiskBadgeMeta(risk)

    return (
        <Badge variant='outline' className={cn(meta.className, className)}>
            <span className='size-1.5 rounded-full bg-current'/>
            {meta.label}
        </Badge>
    )
}
