import {describe, expect, it} from 'vitest'
import {ApiClientError, isApiClientError} from './errors'

describe('ApiClientError', () => {
    it('keeps backend error metadata', () => {
        const error = new ApiClientError({
            errorCode: 'INVALID_REQUEST',
            message: 'Invalid request',
            status: 400,
        })

        expect(error).toBeInstanceOf(Error)
        expect(error.name).toBe('ApiClientError')
        expect(error.errorCode).toBe('INVALID_REQUEST')
        expect(error.message).toBe('Invalid request')
        expect(error.status).toBe(400)
        expect(isApiClientError(error)).toBe(true)
    })
})
