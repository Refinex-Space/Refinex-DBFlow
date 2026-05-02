import {useState} from 'react'
import {z} from 'zod'
import {zodResolver} from '@hookform/resolvers/zod'
import {useForm} from 'react-hook-form'
import {useNavigate} from '@tanstack/react-router'
import {Database, Eye, EyeOff, KeyRound, Loader2, Shield} from 'lucide-react'
import {toast} from 'sonner'
import {login} from '@/api/session'
import {useSessionStore} from '@/stores/session-store'
import {isApiClientError} from '@/lib/errors'
import {Button} from '@/components/ui/button'
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage,} from '@/components/ui/form'
import {Input} from '@/components/ui/input'

const loginSchema = z.object({
    username: z.string().min(1, 'Please enter your username.'),
    password: z.string().min(1, 'Please enter your password.'),
})

type LoginFormValues = z.infer<typeof loginSchema>

interface LoginPageProps {
    redirectTo?: string
}

const governanceSignals = [
    {
        title: 'MCP SQL Gateway',
        description: 'One managed entrypoint for AI-assisted MySQL operations.',
        icon: Database,
    },
    {
        title: 'Database Operation Governance',
        description: 'Authorization, policy checks, and high-risk SQL controls stay server-side.',
        icon: Shield,
    },
    {
        title: 'Audit Ready',
        description: 'Every sensitive operation is traceable without exposing secrets.',
        icon: KeyRound,
    },
]

export function LoginPage({redirectTo}: LoginPageProps) {
    const navigate = useNavigate()
    const setSession = useSessionStore((state) => state.setSession)
    const [showPassword, setShowPassword] = useState(false)

    const form = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            username: '',
            password: '',
        },
    })

    const isSubmitting = form.formState.isSubmitting

    async function onSubmit(values: LoginFormValues) {
        try {
            const session = await login(values.username, values.password)
            setSession(session)
            toast.success('Welcome to DBFlow Admin')
            await navigate({
                to: normalizeRedirect(redirectTo),
                replace: true,
            })
        } catch (error) {
            toast.error(getLoginErrorMessage(error))
        }
    }

    return (
        <main
            className='min-h-svh bg-[radial-gradient(circle_at_top_left,var(--muted),transparent_34%),linear-gradient(135deg,var(--background),var(--muted))]'>
            <div
                className='mx-auto grid min-h-svh w-full max-w-6xl gap-8 px-4 py-8 md:grid-cols-[1.05fr_0.95fr] md:items-center md:px-8'>
                <section className='flex flex-col gap-8'>
                    <div className='flex items-center gap-3'>
                        <div
                            className='flex size-10 items-center justify-center rounded-md border bg-background shadow-xs'>
                            <Database className='text-primary'/>
                        </div>
                        <div>
                            <p className='text-sm font-medium text-muted-foreground'>
                                Refinex-DBFlow
                            </p>
                            <h1 className='text-2xl font-semibold tracking-tight'>
                                DBFlow Admin
                            </h1>
                        </div>
                    </div>

                    <div className='flex max-w-2xl flex-col gap-4'>
                        <p className='text-sm font-medium text-primary'>MCP SQL Gateway</p>
                        <h2 className='text-4xl font-semibold tracking-tight md:text-5xl'>
                            Govern database operations before they reach production.
                        </h2>
                        <p className='max-w-xl text-base text-muted-foreground'>
                            Sign in to manage users, project grants, tokens, dangerous SQL
                            policy, and audit visibility for AI-assisted MySQL access.
                        </p>
                    </div>

                    <div className='grid gap-3'>
                        {governanceSignals.map((item) => {
                            const Icon = item.icon
                            return (
                                <div
                                    key={item.title}
                                    className='flex gap-3 rounded-md border bg-background/70 p-4 shadow-xs backdrop-blur'
                                >
                                    <div className='flex size-9 items-center justify-center rounded-md bg-muted'>
                                        <Icon className='text-primary'/>
                                    </div>
                                    <div className='flex flex-col gap-1'>
                                        <h3 className='text-sm font-semibold'>{item.title}</h3>
                                        <p className='text-sm text-muted-foreground'>
                                            {item.description}
                                        </p>
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                </section>

                <Card className='mx-auto w-full max-w-md gap-5 rounded-lg shadow-sm'>
                    <CardHeader>
                        <CardTitle className='text-xl tracking-tight'>
                            Sign in with administrator credentials
                        </CardTitle>
                        <CardDescription>
                            Your session is verified by the DBFlow server. No login state is
                            stored in the browser.
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <Form {...form}>
                            <form
                                className='flex flex-col gap-4'
                                onSubmit={form.handleSubmit(onSubmit)}
                            >
                                <FormField
                                    control={form.control}
                                    name='username'
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Username</FormLabel>
                                            <FormControl>
                                                <Input
                                                    autoComplete='username'
                                                    placeholder='admin'
                                                    disabled={isSubmitting}
                                                    {...field}
                                                />
                                            </FormControl>
                                            <FormMessage/>
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={form.control}
                                    name='password'
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Password</FormLabel>
                                            <div className='relative'>
                                                <FormControl>
                                                    <Input
                                                        type={showPassword ? 'text' : 'password'}
                                                        autoComplete='current-password'
                                                        placeholder='Enter password'
                                                        disabled={isSubmitting}
                                                        className='pe-10'
                                                        {...field}
                                                    />
                                                </FormControl>
                                                <Button
                                                    type='button'
                                                    variant='ghost'
                                                    size='icon'
                                                    disabled={isSubmitting}
                                                    className='absolute inset-y-0 end-0 h-full rounded-s-none text-muted-foreground'
                                                    onClick={() => setShowPassword((current) => !current)}
                                                >
                                                    {showPassword ? <EyeOff/> : <Eye/>}
                                                    <span className='sr-only'>
                              {showPassword ? 'Hide password' : 'Show password'}
                            </span>
                                                </Button>
                                            </div>
                                            <FormMessage/>
                                        </FormItem>
                                    )}
                                />

                                <Button type='submit' className='mt-1' disabled={isSubmitting}>
                                    {isSubmitting ? <Loader2 className='animate-spin'/> : <KeyRound/>}
                                    Sign in
                                </Button>
                            </form>
                        </Form>
                    </CardContent>
                </Card>
            </div>
        </main>
    )
}

function normalizeRedirect(redirectTo?: string): string {
    if (!redirectTo || !redirectTo.startsWith('/') || redirectTo.startsWith('//')) {
        return '/'
    }

    return redirectTo
}

function getLoginErrorMessage(error: unknown): string {
    if (isApiClientError(error) && error.message) {
        return error.message
    }

    return 'Sign in failed. Please check your username and password.'
}
