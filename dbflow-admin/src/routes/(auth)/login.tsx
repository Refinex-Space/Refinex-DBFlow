import {z} from 'zod'
import {ensureSession} from '@/stores/session-store'
import {createFileRoute, redirect, useSearch} from '@tanstack/react-router'
import {LoginPage} from '@/features/auth/login-page'

const searchSchema = z.object({
    redirect: z.string().optional(),
})

export const Route = createFileRoute('/(auth)/login')({
    validateSearch: searchSchema,
    beforeLoad: async () => {
        const session = await ensureSession()
        if (session?.authenticated) {
            throw redirect({to: '/'})
        }
    },
    component: Login,
})

function Login() {
    const {redirect} = useSearch({from: '/(auth)/login'})

    return <LoginPage redirectTo={redirect}/>
}
