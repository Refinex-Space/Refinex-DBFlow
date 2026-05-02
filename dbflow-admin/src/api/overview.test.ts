import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {fetchOverview} from './overview'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('fetchOverview', () => {
    it('loads overview data from the DBFlow overview API', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/overview')
            return apiResponse({
                metrics: [],
                recentAuditRows: [],
                attentionItems: [],
                environmentOptions: [],
                windowLabel: '最近 24 小时网关安全、执行和健康摘要。',
            })
        })

        await expect(fetchOverview()).resolves.toMatchObject({
            windowLabel: '最近 24 小时网关安全、执行和健康摘要。',
        })
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
