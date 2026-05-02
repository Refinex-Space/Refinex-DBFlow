import type {AdminSession} from '@/types/session'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {ApiClientError} from '@/lib/errors'

const getCurrentSessionMock = vi.hoisted(() =>
    vi.fn<() => Promise<AdminSession>>()
)

vi.mock('@/api/session', () => ({
    getCurrentSession: getCurrentSessionMock,
}))

async function importSessionStore() {
    const {ensureSession, useSessionStore} = await import('./session-store')
    return {ensureSession, useSessionStore}
}

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

describe('useSessionStore', () => {
    beforeEach(async () => {
        getCurrentSessionMock.mockReset()
        const {useSessionStore} = await importSessionStore()
        useSessionStore.setState({
            session: null,
            status: 'idle',
            error: null,
        })
    })

    it('loads and stores authenticated server session data', async () => {
        getCurrentSessionMock.mockResolvedValue(authenticatedSession)
        const {ensureSession, useSessionStore} = await importSessionStore()

        await expect(ensureSession()).resolves.toEqual(authenticatedSession)

        expect(useSessionStore.getState()).toMatchObject({
            session: authenticatedSession,
            status: 'authenticated',
            error: null,
        })
    })

    it('clears session state when the backend returns 401', async () => {
        getCurrentSessionMock.mockRejectedValue(
            new ApiClientError({
                message: 'Authentication required',
                status: 401,
            })
        )
        const {ensureSession, useSessionStore} = await importSessionStore()

        await expect(ensureSession()).resolves.toBeNull()

        expect(useSessionStore.getState()).toMatchObject({
            session: null,
            status: 'anonymous',
            error: null,
        })
    })

    it('deduplicates concurrent session loads', async () => {
        getCurrentSessionMock.mockResolvedValue(authenticatedSession)
        const {ensureSession} = await importSessionStore()

        const [firstSession, secondSession] = await Promise.all([
            ensureSession(),
            ensureSession(),
        ])

        expect(firstSession).toEqual(authenticatedSession)
        expect(secondSession).toEqual(authenticatedSession)
        expect(getCurrentSessionMock).toHaveBeenCalledTimes(1)
    })
})
