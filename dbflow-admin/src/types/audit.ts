export type AuditDirection = 'asc' | 'desc' | string

export type AuditEventFilters = {
    from?: string
    to?: string
    userId?: string
    project?: string
    env?: string
    risk?: string
    decision?: string
    sqlHash?: string
    tool?: string
    page?: number
    size?: number
    sort?: string
    direction?: AuditDirection
}

export type AuditEventSummary = {
    id: number
    requestId: string
    userId: number | null
    projectKey: string
    environmentKey: string
    clientName: string
    clientVersion: string
    tool: string
    operationType: string
    riskLevel: string
    status: string
    decision: string
    sqlHash: string
    resultSummary: string
    affectedRows: number | null
    createdAt: string
}

export type AuditEventPage = {
    content: AuditEventSummary[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    sort: string
    direction: AuditDirection
}
