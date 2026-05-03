import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {fetchConfigPage} from './config'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('fetchConfigPage', () => {
    it('loads sanitized DBFlow config data from the config API', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/config')
            return apiResponse({
                sourceLabel: 'Local application config',
                rows: [
                    {
                        project: 'ops',
                        projectName: 'Operations',
                        env: 'prod',
                        envName: 'Production',
                        datasource: 'ops/prod',
                        type: 'mysql',
                        host: 'db.internal',
                        port: '3306',
                        schema: 'ops',
                        username: 'dbflow_reader',
                        limits: 'maxPool=2',
                        syncStatus: '已同步',
                    },
                ],
                emptyHint: '',
            })
        })

        const page = await fetchConfigPage()

        expect(page.sourceLabel).toBe('Local application config')
        expect(page.rows[0]).not.toHaveProperty('jdbcUrl')
        expect(page.rows[0]).not.toHaveProperty('password')
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
