import {useState} from 'react'
import {z} from 'zod'
import {zodResolver} from '@hookform/resolvers/zod'
import {useForm} from 'react-hook-form'
import {useNavigate} from '@tanstack/react-router'
import {Database, Eye, EyeOff, KeyRound, Loader2} from 'lucide-react'
import {toast} from 'sonner'
import {login} from '@/api/session'
import {useSessionStore} from '@/stores/session-store'
import {isApiClientError} from '@/lib/errors'
import {Button} from '@/components/ui/button'
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage,} from '@/components/ui/form'
import {Input} from '@/components/ui/input'

const loginSchema = z.object({
    username: z.string().min(1, '请输入用户名。'),
    password: z.string().min(1, '请输入密码。'),
})

type LoginFormValues = z.infer<typeof loginSchema>

interface LoginPageProps {
    redirectTo?: string
}

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
            toast.success('已登录 DBFlow Admin')
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
            className='relative flex min-h-svh items-center justify-center bg-[radial-gradient(circle_at_top_left,var(--muted),transparent_30%),linear-gradient(135deg,var(--background),color-mix(in_oklab,var(--muted)_55%,var(--background)))] px-4 py-12'>
            <header className='absolute left-4 top-4 flex items-center gap-3 md:left-8 md:top-8'>
                <div className='flex size-10 items-center justify-center rounded-md border bg-background/90'>
                    <Database className='text-primary'/>
                </div>
                <h1 className='text-2xl font-semibold tracking-tight'>DBFlow Admin</h1>
            </header>

            <Card className='w-full max-w-md gap-5 rounded-lg bg-background/95 shadow-xs'>
                <CardHeader>
                    <CardTitle className='text-xl tracking-tight'>
                        管理员登录
                    </CardTitle>
                    <CardDescription>
                        使用 DBFlow 管理员账号进入控制台。
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
                                        <FormLabel>用户名</FormLabel>
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
                                        <FormLabel>密码</FormLabel>
                                        <div className='relative'>
                                            <FormControl>
                                                <Input
                                                    type={showPassword ? 'text' : 'password'}
                                                    autoComplete='current-password'
                                                    placeholder='输入密码'
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
                              {showPassword ? '隐藏密码' : '显示密码'}
                            </span>
                                            </Button>
                                        </div>
                                        <FormMessage/>
                                    </FormItem>
                                )}
                            />

                            <Button type='submit' className='mt-1' disabled={isSubmitting}>
                                {isSubmitting ? <Loader2 className='animate-spin'/> : <KeyRound/>}
                                登录
                            </Button>
                        </form>
                    </Form>
                </CardContent>
            </Card>
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

    return '登录失败，请检查用户名和密码。'
}
