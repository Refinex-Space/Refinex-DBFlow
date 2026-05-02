/**
 * API 客户端错误参数。
 */
interface ApiClientErrorOptions {
    message: string
    errorCode?: string
    status?: number
    cause?: unknown
}

/**
 * 带后端错误码和 HTTP 状态的 API 客户端错误。
 */
export class ApiClientError extends Error {
    readonly errorCode?: string
    readonly status?: number
    readonly cause?: unknown

    constructor({message, errorCode, status, cause}: ApiClientErrorOptions) {
        super(message)
        this.name = 'ApiClientError'
        this.errorCode = errorCode
        this.status = status
        this.cause = cause
    }
}

/**
 * 判断未知异常是否为 API 客户端错误。
 */
export function isApiClientError(error: unknown): error is ApiClientError {
    return error instanceof ApiClientError
}
