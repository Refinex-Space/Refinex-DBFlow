import {type AxiosAdapter, AxiosError, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import type {AdminSession} from '@/types/session'
import {afterEach, describe, expect, it} from 'vitest'
import type {ApiClientError} from '@/lib/errors'
import {CSRF_COOKIE_NAME, CSRF_HEADER_NAME} from './csrf'
import {authClient, login, logout} from './session'

const originalAdapter = authClient.defaults.adapter

const authenticatedSession: AdminSession = {
    authenticated: true,
    username: 'admin',
    displayName: 'DBFlow Administrator',
    roles: ['ROLE_ADMIN'],
    shell: {
        adminName: 'admin',
        mcpStatus: 'HEALTHY',
        mcpTone: 'ok',
        configSourceLabel: 'Local application config',
    },
}

afterEach(() => {
    authClient.defaults.adapter = originalAdapter
    document.cookie = `${CSRF_COOKIE_NAME}=; path=/; max-age=0`
})

describe('authClient session endpoints', () => {
    it('posts JSON-aware form login with CSRF header', async () => {
        document.cookie = `${CSRF_COOKIE_NAME}=csrf-token; path=/`

        useAdapter((config) => {
            expect(config.url).toBe('/login')
            expect(config.method).toBe('post')
            expect(config.headers.get('Accept')).toBe('application/json')
            expect(config.headers.get('X-Requested-With')).toBe('XMLHttpRequest')
            expect(config.headers.get('Content-Type')).toBe(
                'application/x-www-form-urlencoded'
            )
            expect(config.headers.get(CSRF_HEADER_NAME)).toBe('csrf-token')
            expect(String(config.data)).toBe('username=admin&password=secret')
            return apiResponse(authenticatedSession)
        })

        await expect(login('admin', 'secret')).resolves.toEqual(authenticatedSession)
    })

    it('posts JSON-aware logout with CSRF header', async () => {
        document.cookie = `${CSRF_COOKIE_NAME}=csrf-token; path=/`

        useAdapter((config) => {
            expect(config.url).toBe('/logout')
            expect(config.method).toBe('post')
            expect(config.headers.get('Accept')).toBe('application/json')
            expect(config.headers.get(CSRF_HEADER_NAME)).toBe('csrf-token')
            return apiResponse(null)
        })

        await expect(logout()).resolves.toBeUndefined()
    })

    it('maps failed JSON login to ApiClientError', async () => {
        useAdapter((config) => {
            const response = apiResponse(null, {
                success: false,
                code: 'UNAUTHENTICATED',
                message: 'Invalid username or password',
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

        await expect(login('admin', 'wrong')).rejects.toMatchObject({
            errorCode: 'UNAUTHENTICATED',
            message: 'Invalid username or password',
            status: 401,
        } satisfies Partial<ApiClientError>)
    })
})

function useAdapter(
    handler: Parameters<AxiosAdapter>[0] extends infer Config
        ? (config: Config) => AxiosResponse | Promise<AxiosResponse>
        : never
) {
    authClient.defaults.adapter = async (config) => handler(config)
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
