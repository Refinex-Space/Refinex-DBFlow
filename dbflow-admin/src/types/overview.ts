export type OverviewMetric = {
    label: string
    value: string
    hint: string
    tone: 'neutral' | 'ok' | 'warn' | 'bad' | string
}

export type RecentAuditRow = {
    id: number
    time: string
    user: string
    project: string
    env: string
    operation: string
    risk: string
    riskTone: string
    decision: string
    decisionTone: string
    sqlHash: string
}

export type AttentionItem = {
    label: string
    status: string
    tone: string
    href: string
}

export type OverviewEnvironmentOption = {
    value: string
    label: string
}

export type Overview = {
    metrics: OverviewMetric[]
    recentAuditRows: RecentAuditRow[]
    attentionItems: AttentionItem[]
    environmentOptions: OverviewEnvironmentOption[]
    windowLabel: string
}
