import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {fetchDangerousPolicies} from './policies'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('fetchDangerousPolicies', () => {
    it('loads sanitized dangerous policy data from the policy API', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/policies/dangerous')
            return apiResponse({
                defaults: [
                    {
                        operation: 'DROP_TABLE',
                        risk: 'CRITICAL',
                        decision: 'POLICY_DENIED',
                        requirement: '必须命中 DROP 白名单',
                        tone: 'bad',
                    },
                ],
                whitelist: [
                    {
                        operation: 'DROP_TABLE',
                        risk: 'CRITICAL',
                        project: 'ops',
                        env: 'prod',
                        schema: 'ops_schema',
                        table: 'legacy_jobs',
                        allowProd: 'NO',
                        prodRule: 'prod 命中后仍拒绝',
                        tone: 'warn',
                    },
                ],
                rules: [
                    {
                        name: 'DROP 白名单',
                        status: 'ENFORCED',
                        description: 'DROP_DATABASE 与 DROP_TABLE 默认拒绝',
                        detail: '必须命中 YAML/Nacos 白名单。',
                        tone: 'bad',
                    },
                ],
                emptyHint: '',
            })
        })

        const page = await fetchDangerousPolicies()

        expect(page.defaults[0].operation).toBe('DROP_TABLE')
        expect(page.whitelist[0].table).toBe('legacy_jobs')
        expect(JSON.stringify(page)).not.toContain('password')
        expect(JSON.stringify(page)).not.toContain('tokenHash')
        expect(JSON.stringify(page)).not.toContain('jdbc:mysql://')
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
