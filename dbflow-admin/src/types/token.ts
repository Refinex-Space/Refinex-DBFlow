import type {UserOption} from '@/types/access'

export type TokenRow = {
    id: number
    userId: number
    username: string
    tokenPrefix: string
    status: string
    expiresAt: string | null
    lastUsedAt: string | null
}

export type TokenFilters = {
    username?: string
    status?: string
}

export type TokenOptionsResponse = {
    users: UserOption[]
}

export type IssueTokenRequest = {
    userId: number
    expiresInDays: number
}

export type ReissueTokenRequest = {
    expiresInDays: number
}

export type IssuedTokenResponse = {
    tokenId: number
    userId: number
    username: string
    plaintextToken: string
    tokenPrefix: string
    expiresAt: string | null
}

export type TokenMutationResult = Record<string, boolean>
