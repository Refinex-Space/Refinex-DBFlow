import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {fetchGrantGroups, fetchGrantOptions, revokeGrant, updateProjectGrants,} from './grants'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('grants API client', () => {
    it('loads grant groups with all supported filters', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/grants')
            expect(config.params).toEqual({
                username: 'alice',
                projectKey: 'billing-core',
                environmentKey: 'staging',
                status: 'ACTIVE',
            })
            return apiResponse([
                {
                    userId: 1,
                    username: 'alice',
                    projectKey: 'billing-core',
                    environments: [
                        {
                            grantId: 11,
                            environmentKey: 'staging',
                            grantType: 'WRITE',
                            status: 'ACTIVE',
                        },
                    ],
                },
            ])
        })

        await expect(
            fetchGrantGroups({
                username: 'alice',
                projectKey: 'billing-core',
                environmentKey: 'staging',
                status: 'ACTIVE',
            })
        ).resolves.toHaveLength(1)
    })

    it('loads grant options without connection fields', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/grants/options')
            return apiResponse({
                users: [{id: 1, username: 'alice', displayName: 'Alice'}],
                environments: [
                    {
                        projectKey: 'billing-core',
                        projectName: 'Billing Core',
                        environmentKey: 'staging',
                        environmentName: 'Staging',
                    },
                ],
            })
        })

        const options = await fetchGrantOptions()

        expect(options.environments[0]).not.toHaveProperty('jdbcUrl')
        expect(JSON.stringify(options)).not.toContain('password')
        expect(JSON.stringify(options)).not.toContain('Token')
    })

    it('updates project grants and revokes a single grant', async () => {
        const calls: Array<{ method?: string; url?: string; data?: unknown }> = []
        useAdapter((config) => {
            calls.push({
                method: config.method,
                url: config.url,
                data: config.data ? JSON.parse(String(config.data)) : undefined,
            })
            return apiResponse({ok: true})
        })

        await updateProjectGrants({
            userId: 1,
            projectKey: 'billing-core',
            environmentKeys: ['staging', 'prod'],
            grantType: 'WRITE',
        })
        await revokeGrant(11)

        expect(calls).toEqual([
            {
                method: 'post',
                url: '/grants/update-project',
                data: {
                    userId: 1,
                    projectKey: 'billing-core',
                    environmentKeys: ['staging', 'prod'],
                    grantType: 'WRITE',
                },
            },
            {method: 'post', url: '/grants/11/revoke', data: undefined},
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
