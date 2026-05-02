import {apiGet, apiPost} from '@/api/client'
import type {
    IssuedTokenResponse,
    IssueTokenRequest,
    ReissueTokenRequest,
    TokenFilters,
    TokenMutationResult,
    TokenOptionsResponse,
    TokenRow,
} from '@/types/token'

export const tokenOptionsQueryKey = ['tokens', 'options']

export const tokensQueryKey = (filters?: TokenFilters) =>
    filters ? ['tokens', normalizeFilters(filters)] : ['tokens']

export function fetchTokens(filters: TokenFilters = {}): Promise<TokenRow[]> {
    return apiGet<TokenRow[]>('/tokens', {
        params: normalizeFilters(filters),
    })
}

export function fetchTokenOptions(): Promise<TokenOptionsResponse> {
    return apiGet<TokenOptionsResponse>('/tokens/options')
}

export function issueToken(
    request: IssueTokenRequest
): Promise<IssuedTokenResponse> {
    return apiPost<IssuedTokenResponse, IssueTokenRequest>(
        '/tokens',
        cleanIssueRequest(request)
    )
}

export function reissueToken(
    userId: number,
    request: ReissueTokenRequest
): Promise<IssuedTokenResponse> {
    return apiPost<IssuedTokenResponse, ReissueTokenRequest>(
        `/users/${userId}/tokens/reissue`,
        cleanReissueRequest(request)
    )
}

export function revokeToken(tokenId: number): Promise<TokenMutationResult> {
    return apiPost<TokenMutationResult>(`/tokens/${tokenId}/revoke`)
}

function normalizeFilters(filters: TokenFilters): TokenFilters {
    return {
        username: cleanOptionalString(filters.username),
        status: cleanOptionalString(filters.status),
    }
}

function cleanIssueRequest(request: IssueTokenRequest): IssueTokenRequest {
    return {
        userId: request.userId,
        expiresInDays: normalizeExpiresInDays(request.expiresInDays),
    }
}

function cleanReissueRequest(request: ReissueTokenRequest): ReissueTokenRequest {
    return {
        expiresInDays: normalizeExpiresInDays(request.expiresInDays),
    }
}

function normalizeExpiresInDays(value: number): number {
    return Number.isFinite(value) && value > 0 ? Math.trunc(value) : 30
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
