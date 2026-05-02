import {getEnvBadgeMeta} from '@/lib/badges'
import {cn} from '@/lib/utils'
import {Badge} from '@/components/ui/badge'

type EnvBadgeProps = {
    environment: string | null | undefined
    className?: string
}

export function EnvBadge({environment, className}: EnvBadgeProps) {
    const meta = getEnvBadgeMeta(environment)

    return (
        <Badge variant='outline' className={cn(meta.className, className)}>
            {meta.label}
        </Badge>
    )
}
