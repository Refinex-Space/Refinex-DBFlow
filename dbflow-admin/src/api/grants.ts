import {apiGet, apiPost} from '@/api/client'
import type {
    GrantFilters,
    GrantGroupRow,
    GrantMutationResult,
    GrantOptionsResponse,
    UpdateProjectGrantsRequest,
} from '@/types/access'

export const grantOptionsQueryKey = ['grants', 'options']

export const grantsQueryKey = (filters?: GrantFilters) =>
    filters ? ['grants', normalizeFilters(filters)] : ['grants']

export function fetchGrantGroups(
    filters: GrantFilters = {}
): Promise<GrantGroupRow[]> {
    return apiGet<GrantGroupRow[]>('/grants', {
        params: normalizeFilters(filters),
    })
}

export function fetchGrantOptions(): Promise<GrantOptionsResponse> {
    return apiGet<GrantOptionsResponse>('/grants/options')
}

export function updateProjectGrants(
    request: UpdateProjectGrantsRequest
): Promise<GrantMutationResult> {
    return apiPost<GrantMutationResult, UpdateProjectGrantsRequest>(
        '/grants/update-project',
        {
            userId: request.userId,
            projectKey: request.projectKey.trim(),
            environmentKeys: request.environmentKeys.map((key) => key.trim()).filter(Boolean),
            grantType: request.grantType.trim(),
        }
    )
}

export function revokeGrant(grantId: number): Promise<GrantMutationResult> {
    return apiPost<GrantMutationResult>(`/grants/${grantId}/revoke`)
}

function normalizeFilters(filters: GrantFilters): GrantFilters {
    return {
        username: cleanOptionalString(filters.username),
        projectKey: cleanOptionalString(filters.projectKey),
        environmentKey: cleanOptionalString(filters.environmentKey),
        status: cleanOptionalString(filters.status),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
