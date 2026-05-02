export const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
export const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'

/**
 * 读取 Spring Security 暴露给 SPA 的 CSRF token cookie。
 */
export function getCsrfToken(): string | undefined {
    return getCookieValue(CSRF_COOKIE_NAME)
}

/**
 * 从 document.cookie 读取指定 cookie。
 */
export function getCookieValue(name: string): string | undefined {
    if (typeof document === 'undefined') {
        return undefined
    }

    const cookie = document.cookie
        .split('; ')
        .find((part) => part.startsWith(`${name}=`))

    if (!cookie) {
        return undefined
    }

    const value = cookie.slice(name.length + 1)
    try {
        return decodeURIComponent(value)
    } catch {
        return value
    }
}
