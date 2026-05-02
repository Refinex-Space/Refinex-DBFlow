import {apiGet, apiPost} from '@/api/client'
import type {
    AdminUserRow,
    CreateUserRequest,
    ResetPasswordRequest,
    UserFilters,
    UserMutationResult,
} from '@/types/access'

export const usersQueryKey = (filters?: UserFilters) =>
    filters ? ['users', normalizeFilters(filters)] : ['users']

export function fetchUsers(filters: UserFilters = {}): Promise<AdminUserRow[]> {
    return apiGet<AdminUserRow[]>('/users', {
        params: normalizeFilters(filters),
    })
}

export function createUser(
    request: CreateUserRequest
): Promise<AdminUserRow> {
    return apiPost<AdminUserRow, CreateUserRequest>('/users', cleanCreateRequest(request))
}

export function disableUser(userId: number): Promise<UserMutationResult> {
    return apiPost<UserMutationResult>(`/users/${userId}/disable`)
}

export function enableUser(userId: number): Promise<UserMutationResult> {
    return apiPost<UserMutationResult>(`/users/${userId}/enable`)
}

export function resetUserPassword(
    userId: number,
    request: ResetPasswordRequest
): Promise<UserMutationResult> {
    return apiPost<UserMutationResult, ResetPasswordRequest>(
        `/users/${userId}/reset-password`,
        request
    )
}

function normalizeFilters(filters: UserFilters): UserFilters {
    return {
        username: cleanOptionalString(filters.username),
        status: cleanOptionalString(filters.status),
    }
}

function cleanCreateRequest(request: CreateUserRequest): CreateUserRequest {
    return {
        username: request.username.trim(),
        displayName: request.displayName.trim(),
        password: cleanOptionalString(request.password),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
