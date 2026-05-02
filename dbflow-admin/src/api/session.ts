import axios, {AxiosError} from 'axios'
import {apiGet} from '@/api/client'
import {CSRF_HEADER_NAME, getCsrfToken} from '@/api/csrf'
import {type ApiResult, isApiResult} from '@/types/api'
import type {AdminSession} from '@/types/session'
import {ApiClientError} from '@/lib/errors'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

/**
 * Spring Security 登录登出客户端，使用根路径而不是 `/admin/api`。
 */
export const authClient = axios.create({
    headers: {
        Accept: 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
    },
    withCredentials: true,
})

authClient.interceptors.request.use((config) => {
    const method = config.method?.toUpperCase() ?? 'GET'
    if (!SAFE_METHODS.has(method)) {
        const csrfToken = getCsrfToken()
        if (csrfToken) {
            config.headers.set(CSRF_HEADER_NAME, csrfToken)
        }
    }

    return config
})

authClient.interceptors.response.use(
    (response) => unwrapAuthResult(response.data),
    (error: unknown) => Promise.reject(toAuthClientError(error))
)

/**
 * 读取当前管理端服务端 Session。
 */
export function getCurrentSession(): Promise<AdminSession> {
    return apiGet<AdminSession>('/session')
}

/**
 * 使用 Spring Security JSON 登录协议创建管理端 Session。
 */
export function login(
    username: string,
    password: string
): Promise<AdminSession> {
    const body = new URLSearchParams()
    body.set('username', username)
    body.set('password', password)

    return authClient.post<unknown, AdminSession>('/login', body, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
    })
}

/**
 * 使用 Spring Security JSON 登出协议失效当前 Session。
 */
export async function logout(): Promise<void> {
    await authClient.post('/logout')
}

function unwrapAuthResult<T>(payload: ApiResult<T> | T): T {
    if (!isApiResult(payload)) {
        return payload
    }

    if (payload.success) {
        return payload.data as T
    }

    throw new ApiClientError({
        errorCode: payload.code,
        message: payload.message || 'Authentication request failed',
    })
}

function toAuthClientError(error: unknown): unknown {
    if (!(error instanceof AxiosError)) {
        return error
    }

    const status = error.response?.status
    const payload = error.response?.data

    if (isApiResult(payload)) {
        return new ApiClientError({
            errorCode: payload.code,
            message: payload.message || error.message,
            status,
            cause: error,
        })
    }

    return new ApiClientError({
        message: error.message || 'Authentication request failed',
        status,
        cause: error,
    })
}
