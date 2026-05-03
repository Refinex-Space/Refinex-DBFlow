import type {IssuedTokenResponse} from '@/types/token'

export function createIssuedToken(
    overrides: Partial<IssuedTokenResponse> = {}
): IssuedTokenResponse {
    return {
        tokenId: 10,
        userId: 1,
        username: 'alice',
        plaintextToken: 'dbf_plaintext_once',
        tokenPrefix: 'dbf_plaintext_on',
        expiresAt: '2026-06-01T00:00:00Z',
        ...overrides,
    }
}
