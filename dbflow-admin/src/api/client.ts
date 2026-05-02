import axios, {AxiosError, type AxiosRequestConfig} from 'axios'
import {type ApiResult, isApiResult} from '@/types/api'
import {CSRF_HEADER_NAME, getCsrfToken} from '@/api/csrf'
import {ApiClientError} from '@/lib/errors'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

/**
 * DBFlow React 管理端统一 API 客户端。
 */
export const apiClient = axios.create({
    baseURL: '/admin/api',
    headers: {
        Accept: 'application/json',
    },
    withCredentials: true,
})

apiClient.interceptors.request.use((config) => {
    const method = config.method?.toUpperCase() ?? 'GET'
    if (!SAFE_METHODS.has(method)) {
        const csrfToken = getCsrfToken()
        if (csrfToken) {
            config.headers.set(CSRF_HEADER_NAME, csrfToken)
        }
    }

    return config
})

apiClient.interceptors.response.use(
    (response) => unwrapApiResult(response.data),
    (error: unknown) => Promise.reject(toApiClientError(error))
)

/**
 * 发送 GET 请求并返回解包后的业务数据。
 */
export async function apiGet<T>(
    url: string,
    config?: AxiosRequestConfig
): Promise<T> {
    return apiClient.get<unknown, T>(url, config)
}

/**
 * 发送 POST 请求并返回解包后的业务数据。
 */
export async function apiPost<T, B = unknown>(
    url: string,
    body?: B,
    config?: AxiosRequestConfig
): Promise<T> {
    return apiClient.post<unknown, T>(url, body, config)
}

/**
 * 发送 DELETE 请求并返回解包后的业务数据。
 */
export async function apiDelete<T>(
    url: string,
    config?: AxiosRequestConfig
): Promise<T> {
    return apiClient.delete<unknown, T>(url, config)
}

function unwrapApiResult<T>(payload: ApiResult<T> | T): T {
    if (!isApiResult(payload)) {
        return payload
    }

    if (payload.success) {
        return payload.data as T
    }

    throw new ApiClientError({
        errorCode: payload.code,
        message: payload.message || 'API request failed',
    })
}

function toApiClientError(error: unknown): unknown {
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
        message: error.message || 'API request failed',
        status,
        cause: error,
    })
}
