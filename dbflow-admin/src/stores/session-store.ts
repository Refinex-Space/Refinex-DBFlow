import type {AdminSession} from '@/types/session'
import {create} from 'zustand'
import {getCurrentSession} from '@/api/session'
import {isApiClientError} from '@/lib/errors'

export type SessionStatus =
    | 'idle'
    | 'loading'
    | 'authenticated'
    | 'anonymous'
    | 'error'

interface SessionState {
    session: AdminSession | null
    status: SessionStatus
    error: unknown | null
    setSession: (session: AdminSession) => void
    clearSession: () => void
    loadSession: () => Promise<AdminSession | null>
}

let inFlightSession: Promise<AdminSession | null> | null = null

/**
 * 服务端 Session 状态，不做 localStorage 持久化。
 */
export const useSessionStore = create<SessionState>()((set, get) => ({
    session: null,
    status: 'idle',
    error: null,
    setSession: (session) =>
        set({session, status: 'authenticated', error: null}),
    clearSession: () => set({session: null, status: 'anonymous', error: null}),
    loadSession: async () => {
        const current = get()
        if (current.session && current.status === 'authenticated') {
            return current.session
        }

        if (inFlightSession) {
            return inFlightSession
        }

        set({status: 'loading', error: null})
        inFlightSession = getCurrentSession()
            .then((session) => {
                if (!session.authenticated) {
                    set({session: null, status: 'anonymous', error: null})
                    return null
                }

                set({session, status: 'authenticated', error: null})
                return session
            })
            .catch((error: unknown) => {
                if (isApiClientError(error) && error.status === 401) {
                    set({session: null, status: 'anonymous', error: null})
                    return null
                }

                set({session: null, status: 'error', error})
                throw error
            })
            .finally(() => {
                inFlightSession = null
            })

        return inFlightSession
    },
}))

/**
 * 给 TanStack Router loader/beforeLoad 使用的 Session 入口。
 */
export function ensureSession(): Promise<AdminSession | null> {
    return useSessionStore.getState().loadSession()
}
