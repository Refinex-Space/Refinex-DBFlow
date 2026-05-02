export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type Decision =
    | 'EXECUTED'
    | 'POLICY_DENIED'
    | 'REQUIRES_CONFIRMATION'
    | 'FAILED'

export type Status =
    | 'ACTIVE'
    | 'DISABLED'
    | 'REVOKED'
    | 'EXPIRED'
    | 'HEALTHY'
    | 'UNHEALTHY'

export type BadgeMeta = {
    label: string
    className: string
}

const neutralBadge =
    'border-muted-foreground/25 bg-muted/40 text-muted-foreground'

const riskBadgeMeta: Record<RiskLevel, BadgeMeta> = {
    LOW: {
        label: 'LOW',
        className: 'border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-300',
    },
    MEDIUM: {
        label: 'MEDIUM',
        className:
            'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300',
    },
    HIGH: {
        label: 'HIGH',
        className:
            'border-orange-500/30 bg-orange-500/10 text-orange-700 dark:text-orange-300',
    },
    CRITICAL: {
        label: 'CRITICAL',
        className: 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300',
    },
}

const decisionBadgeMeta: Record<Decision, BadgeMeta> = {
    EXECUTED: {
        label: 'EXECUTED',
        className:
            'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
    },
    POLICY_DENIED: {
        label: 'POLICY_DENIED',
        className: 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300',
    },
    REQUIRES_CONFIRMATION: {
        label: 'REQUIRES_CONFIRMATION',
        className:
            'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300',
    },
    FAILED: {
        label: 'FAILED',
        className:
            'border-rose-500/30 bg-rose-500/10 text-rose-700 dark:text-rose-300',
    },
}

const statusBadgeMeta: Record<Status, BadgeMeta> = {
    ACTIVE: {
        label: 'ACTIVE',
        className:
            'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
    },
    DISABLED: {
        label: 'DISABLED',
        className:
            'border-slate-500/30 bg-slate-500/10 text-slate-700 dark:text-slate-300',
    },
    REVOKED: {
        label: 'REVOKED',
        className: 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300',
    },
    EXPIRED: {
        label: 'EXPIRED',
        className:
            'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300',
    },
    HEALTHY: {
        label: 'HEALTHY',
        className:
            'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
    },
    UNHEALTHY: {
        label: 'UNHEALTHY',
        className: 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300',
    },
}

export function getRiskBadgeMeta(value: string | null | undefined): BadgeMeta {
    const key = normalizeKey(value)
    return key in riskBadgeMeta
        ? riskBadgeMeta[key as RiskLevel]
        : unknownBadgeMeta(key)
}

export function getDecisionBadgeMeta(
    value: string | null | undefined
): BadgeMeta {
    const key = normalizeKey(value)
    return key in decisionBadgeMeta
        ? decisionBadgeMeta[key as Decision]
        : unknownBadgeMeta(key)
}

export function getStatusBadgeMeta(
    value: string | null | undefined
): BadgeMeta {
    const key = normalizeKey(value)
    return key in statusBadgeMeta
        ? statusBadgeMeta[key as Status]
        : unknownBadgeMeta(key)
}

export function getEnvBadgeMeta(value: string | null | undefined): BadgeMeta {
    const key = normalizeKey(value)
    const lowerKey = key.toLowerCase()

    if (['prod', 'production'].includes(lowerKey)) {
        return {
            label: key,
            className:
                'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300',
        }
    }

    if (['stage', 'staging', 'pre', 'preprod'].includes(lowerKey)) {
        return {
            label: key,
            className:
                'border-violet-500/30 bg-violet-500/10 text-violet-700 dark:text-violet-300',
        }
    }

    if (['test', 'qa', 'uat'].includes(lowerKey)) {
        return {
            label: key,
            className:
                'border-cyan-500/30 bg-cyan-500/10 text-cyan-700 dark:text-cyan-300',
        }
    }

    if (['dev', 'local'].includes(lowerKey)) {
        return {
            label: key,
            className:
                'border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-300',
        }
    }

    return unknownBadgeMeta(key)
}

function normalizeKey(value: string | null | undefined): string {
    const key = value?.trim()
    return key ? key.toUpperCase() : 'UNKNOWN'
}

function unknownBadgeMeta(label: string): BadgeMeta {
    return {
        label,
        className: neutralBadge,
    }
}
