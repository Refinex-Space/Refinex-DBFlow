import type {AuditEventFilters, AuditEventPage} from '@/types/audit'
import {apiGet} from '@/api/client'

export const AUDIT_DEFAULT_PAGE = 0
export const AUDIT_DEFAULT_SIZE = 20
export const AUDIT_DEFAULT_SORT = 'createdAt'
export const AUDIT_DEFAULT_DIRECTION = 'desc'

export const auditEventsQueryKey = (filters?: AuditEventFilters) =>
    filters ? ['audit-events', normalizeAuditFilters(filters)] : ['audit-events']

export function fetchAuditEvents(
    filters: AuditEventFilters = {}
): Promise<AuditEventPage> {
    return apiGet<AuditEventPage>('/audit-events', {
        params: normalizeAuditFilters(filters),
    })
}

export function normalizeAuditFilters(
    filters: AuditEventFilters
): AuditEventFilters {
    return {
        from: cleanOptionalString(filters.from),
        to: cleanOptionalString(filters.to),
        userId: cleanOptionalString(filters.userId),
        project: cleanOptionalString(filters.project),
        env: cleanOptionalString(filters.env),
        risk: cleanOptionalString(filters.risk),
        decision: cleanOptionalString(filters.decision),
        sqlHash: cleanOptionalString(filters.sqlHash),
        tool: cleanOptionalString(filters.tool),
        page: normalizeNonNegativeInteger(filters.page, AUDIT_DEFAULT_PAGE),
        size: normalizePositiveInteger(filters.size, AUDIT_DEFAULT_SIZE),
        sort: cleanOptionalString(filters.sort) ?? AUDIT_DEFAULT_SORT,
        direction: normalizeDirection(filters.direction),
    }
}

export function auditResetSearch(
    current?: AuditEventFilters
): AuditEventFilters {
    return {
        page: AUDIT_DEFAULT_PAGE,
        size: normalizePositiveInteger(current?.size, AUDIT_DEFAULT_SIZE),
        sort: cleanOptionalString(current?.sort) ?? AUDIT_DEFAULT_SORT,
        direction: normalizeDirection(current?.direction),
    }
}

function normalizeDirection(value: string | undefined): string {
    return value?.toLowerCase() === 'asc' ? 'asc' : AUDIT_DEFAULT_DIRECTION
}

function normalizeNonNegativeInteger(
    value: number | undefined,
    fallback: number
): number {
    if (value === undefined || !Number.isFinite(value)) {
        return fallback
    }

    return Math.max(0, Math.trunc(value))
}

function normalizePositiveInteger(
    value: number | undefined,
    fallback: number
): number {
    if (value === undefined || !Number.isFinite(value) || value <= 0) {
        return fallback
    }

    return Math.trunc(value)
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
