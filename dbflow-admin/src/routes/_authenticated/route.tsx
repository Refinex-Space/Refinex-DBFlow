import {createFileRoute, redirect} from '@tanstack/react-router'
import {ensureSession} from '@/stores/session-store'
import {AuthRoutePending} from '@/components/auth/require-auth'
import {AuthenticatedLayout} from '@/components/layout/authenticated-layout'

export const Route = createFileRoute('/_authenticated')({
    beforeLoad: async ({location}) => {
        const session = await ensureSession()
        if (!session?.authenticated) {
            throw redirect({
                to: '/login',
                search: {
                    redirect: location.href,
                },
            })
        }
    },
    pendingComponent: AuthRoutePending,
    component: AuthenticatedLayout,
})
