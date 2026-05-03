import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {fetchHealthPage} from './health'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('fetchHealthPage', () => {
    it('loads sanitized system health data from the health API', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/health')
            return apiResponse({
                overall: 'HEALTHY',
                tone: 'ok',
                totalCount: 4,
                unhealthyCount: 0,
                items: [
                    {
                        name: 'metadata database',
                        component: 'database',
                        status: 'HEALTHY',
                        description: 'Metadata database is reachable.',
                        detail: 'metadata ok',
                        tone: 'ok',
                    },
                ],
            })
        })

        const page = await fetchHealthPage()

        expect(page.overall).toBe('HEALTHY')
        expect(page.items[0].name).toBe('metadata database')
        expect(JSON.stringify(page)).not.toContain('plain-db-password')
        expect(JSON.stringify(page)).not.toContain('jdbc:mysql://')
        expect(JSON.stringify(page)).not.toContain('tokenHash')
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
