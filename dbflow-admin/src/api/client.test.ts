import {type AxiosAdapter, AxiosError, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import type {ApiClientError} from '@/lib/errors'
import {apiClient, apiGet, apiPost} from './client'
import {CSRF_COOKIE_NAME, CSRF_HEADER_NAME} from './csrf'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
    document.cookie = `${CSRF_COOKIE_NAME}=; path=/; max-age=0`
})

describe('apiClient', () => {
    it('unwraps successful ApiResult responses', async () => {
        useAdapter(() => apiResponse({name: 'admin'}))

        await expect(apiGet<{ name: string }>('/session')).resolves.toEqual({
            name: 'admin',
        })
    })

    it('throws ApiClientError for failed ApiResult responses', async () => {
        useAdapter(() =>
            apiResponse(null, {
                success: false,
                code: 'INVALID_REQUEST',
                message: 'Invalid request',
            })
        )

        await expect(apiGet('/broken')).rejects.toMatchObject({
            errorCode: 'INVALID_REQUEST',
            message: 'Invalid request',
        })
    })

    it('does not add CSRF header to GET requests', async () => {
        document.cookie = `${CSRF_COOKIE_NAME}=csrf-token; path=/`

        useAdapter((config) => {
            expect(config.headers.get(CSRF_HEADER_NAME)).toBeUndefined()
            return apiResponse({ok: true})
        })

        await apiGet('/session')
    })

    it('adds CSRF header to non-GET requests', async () => {
        document.cookie = `${CSRF_COOKIE_NAME}=csrf-token; path=/`

        useAdapter((config) => {
            expect(config.headers.get(CSRF_HEADER_NAME)).toBe('csrf-token')
            return apiResponse({created: true})
        })

        await apiPost('/users', {username: 'admin'})
    })

    it('propagates 401 responses without redirect side effects', async () => {
        useAdapter((config) => {
            const response = apiResponse(null, {
                success: false,
                code: 'UNAUTHORIZED',
                message: 'Authentication required',
            })
            response.status = 401
            response.statusText = 'Unauthorized'

            throw new AxiosError(
                'Request failed with status code 401',
                AxiosError.ERR_BAD_REQUEST,
                config,
                undefined,
                response
            )
        })

        await expect(apiGet('/session')).rejects.toMatchObject({
            errorCode: 'UNAUTHORIZED',
            message: 'Authentication required',
            status: 401,
        } satisfies Partial<ApiClientError>)
    })
})

function useAdapter(
    handler: Parameters<AxiosAdapter>[0] extends infer Config
        ? (config: Config) => AxiosResponse | Promise<AxiosResponse>
        : never
) {
    apiClient.defaults.adapter = async (config) => handler(config)
}

function apiResponse<T>(
    data: T,
    result: Partial<ApiResult<T>> = {}
): AxiosResponse<ApiResult<T>> {
    return {
        data: {
            success: true,
            code: 'OK',
            message: 'OK',
            data,
            ...result,
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as AxiosResponse<ApiResult<T>>['config'],
    }
}
