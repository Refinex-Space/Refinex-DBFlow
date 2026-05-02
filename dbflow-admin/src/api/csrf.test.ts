import {afterEach, describe, expect, it} from 'vitest'
import {CSRF_COOKIE_NAME, getCookieValue, getCsrfToken} from './csrf'

function clearCookie(name: string) {
    document.cookie = `${name}=; path=/; max-age=0`
}

describe('csrf', () => {
    afterEach(() => {
        clearCookie(CSRF_COOKIE_NAME)
        clearCookie('encoded-cookie')
    })

    it('reads the Spring Security CSRF cookie', () => {
        document.cookie = `${CSRF_COOKIE_NAME}=csrf-token; path=/`

        expect(getCsrfToken()).toBe('csrf-token')
    })

    it('decodes encoded cookie values', () => {
        document.cookie = `encoded-cookie=${encodeURIComponent('a b+c')}; path=/`

        expect(getCookieValue('encoded-cookie')).toBe('a b+c')
    })
})
