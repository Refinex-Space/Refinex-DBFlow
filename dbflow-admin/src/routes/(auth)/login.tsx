import {z} from 'zod'
import {createFileRoute, Link, redirect, useSearch,} from '@tanstack/react-router'
import {ensureSession} from '@/stores/session-store'
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'
import {AuthLayout} from '@/features/auth/auth-layout'
import {UserAuthForm} from '@/features/auth/sign-in/components/user-auth-form'

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

    return (
        <AuthLayout>
            <Card className='max-w-sm gap-4'>
                <CardHeader>
                    <CardTitle className='text-lg tracking-tight'>
                        Sign in to DBFlow Admin
                    </CardTitle>
                    <CardDescription>
                        Use your administrator account to access{' '}
                        <br className='max-sm:hidden'/> Refinex-DBFlow operations.
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <UserAuthForm redirectTo={redirect}/>
                    <p className='mt-4 text-center text-sm text-muted-foreground'>
                        Prefer the template path?{' '}
                        <Link
                            to='/sign-in'
                            search={{redirect}}
                            className='underline underline-offset-4 hover:text-primary'
                        >
                            Sign in
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </AuthLayout>
    )
}
