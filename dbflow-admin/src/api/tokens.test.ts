import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {fetchTokenOptions, fetchTokens, issueToken, reissueToken, revokeToken,} from './tokens'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('tokens API client', () => {
    it('loads token rows with supported filters and no plaintext fields', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/tokens')
            expect(config.params).toEqual({
                username: 'alice',
                status: 'ACTIVE',
            })
            return apiResponse([
                {
                    id: 10,
                    userId: 1,
                    username: 'alice',
                    tokenPrefix: 'dbf_live_123456',
                    status: 'ACTIVE',
                    expiresAt: '2026-06-01T00:00:00Z',
                    lastUsedAt: null,
                },
            ])
        })

        const rows = await fetchTokens({
            username: ' alice ',
            status: ' ACTIVE ',
        })

        expect(rows[0].tokenPrefix).toBe('dbf_live_123456')
        expect(rows[0]).not.toHaveProperty('plaintextToken')
        expect(rows[0]).not.toHaveProperty('tokenHash')
    })

    it('loads active user options for token issuing', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/tokens/options')
            return apiResponse({
                users: [{id: 1, username: 'alice', displayName: 'Alice'}],
            })
        })

        await expect(fetchTokenOptions()).resolves.toEqual({
            users: [{id: 1, username: 'alice', displayName: 'Alice'}],
        })
    })

    it('issues, reissues, and revokes tokens through the JSON API', async () => {
        const calls: Array<{ method?: string; url?: string; data?: unknown }> = []
        useAdapter((config) => {
            calls.push({
                method: config.method,
                url: config.url,
                data: config.data ? JSON.parse(String(config.data)) : undefined,
            })

            if (config.url === '/tokens/10/revoke') {
                return apiResponse({revoked: true})
            }

            return apiResponse({
                tokenId: 10,
                userId: 1,
                username: 'alice',
                plaintextToken: 'dbf_plaintext_once',
                tokenPrefix: 'dbf_plaintext_on',
                expiresAt: '2026-06-01T00:00:00Z',
            })
        })

        await issueToken({userId: 1, expiresInDays: 7})
        await reissueToken(1, {expiresInDays: 30})
        await revokeToken(10)

        expect(calls).toEqual([
            {
                method: 'post',
                url: '/tokens',
                data: {userId: 1, expiresInDays: 7},
            },
            {
                method: 'post',
                url: '/users/1/tokens/reissue',
                data: {expiresInDays: 30},
            },
            {method: 'post', url: '/tokens/10/revoke', data: undefined},
        ])
    })
})

function useAdapter(
    handler: Parameters<AxiosAdapter>[0] extends infer Config
        ? (config: Config) => AxiosResponse | Promise<AxiosResponse>
        : never
) {
    apiClient.defaults.adapter = async (config) => handler(config)
}

function apiResponse<T>(data: T): AxiosResponse<ApiResult<T>> {
    return {
        data: {
            success: true,
            code: 'OK',
            message: 'OK',
            data,
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as AxiosResponse<ApiResult<T>>['config'],
    }
}
