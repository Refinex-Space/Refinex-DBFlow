/**
 * 后端统一 API 响应结构。
 */
export interface ApiResult<T = unknown> {
    success: boolean
    code: string
    message: string
    data: T
}

/**
 * 判断响应体是否符合 DBFlow ApiResult 结构。
 */
export function isApiResult(value: unknown): value is ApiResult {
    if (!value || typeof value !== 'object') {
        return false
    }

    const candidate = value as Partial<ApiResult>
    return (
        typeof candidate.success === 'boolean' &&
        typeof candidate.code === 'string' &&
        typeof candidate.message === 'string' &&
        'data' in candidate
    )
}
