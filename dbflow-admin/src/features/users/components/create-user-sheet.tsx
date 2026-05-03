import {type FormEvent, useState} from 'react'
import {useMutation, useQueryClient} from '@tanstack/react-query'
import {Plus, TriangleAlert} from 'lucide-react'
import {toast} from 'sonner'
import {createUser, usersQueryKey} from '@/api/users'
import {isApiClientError} from '@/lib/errors'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {
    Sheet,
    SheetContent,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from '@/components/ui/sheet'
import {PasswordInput} from '@/components/password-input'

export function CreateUserSheet() {
    const queryClient = useQueryClient()
    const [open, setOpen] = useState(false)
    const [username, setUsername] = useState('')
    const [displayName, setDisplayName] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState<string | null>(null)

    const mutation = useMutation({
        mutationFn: createUser,
        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: usersQueryKey()})
            toast.success('用户已创建')
            setOpen(false)
            resetForm()
        },
        onError: (mutationError) => {
            const message = errorMessage(mutationError)
            setError(message)
            toast.error(message)
        },
    })

    function resetForm() {
        setUsername('')
        setDisplayName('')
        setPassword('')
        setError(null)
    }

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)
        mutation.mutate({
            username,
            displayName,
            password: password.trim() || undefined,
        })
    }

    return (
        <Sheet
            open={open}
            onOpenChange={(nextOpen) => {
                setOpen(nextOpen)
                if (!nextOpen) {
                    resetForm()
                }
            }}
        >
            <SheetTrigger asChild>
                <Button>
                    <Plus className='size-4'/>
                    新建用户
                </Button>
            </SheetTrigger>
            <SheetContent aria-describedby={undefined} className='sm:max-w-md'>
                <form className='flex h-full flex-col' onSubmit={handleSubmit}>
                    <SheetHeader>
                        <SheetTitle>新建用户</SheetTitle>
                    </SheetHeader>

                    <div className='grid gap-4 px-4'>
                        {error && (
                            <Alert variant='destructive'>
                                <TriangleAlert/>
                                <AlertTitle>创建失败</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}

                        <div className='grid gap-2'>
                            <Label htmlFor='create-user-username'>用户名</Label>
                            <Input
                                id='create-user-username'
                                value={username}
                                autoComplete='username'
                                onChange={(event) => setUsername(event.target.value)}
                            />
                        </div>
                        <div className='grid gap-2'>
                            <Label htmlFor='create-user-display-name'>显示名</Label>
                            <Input
                                id='create-user-display-name'
                                value={displayName}
                                onChange={(event) => setDisplayName(event.target.value)}
                            />
                        </div>
                        <div className='grid gap-2'>
                            <Label htmlFor='create-user-password'>初始密码</Label>
                            <PasswordInput
                                id='create-user-password'
                                value={password}
                                autoComplete='new-password'
                                onChange={(event) => setPassword(event.target.value)}
                            />
                            <p className='text-xs text-muted-foreground'>
                                可留空；后续可通过重置密码补齐。
                            </p>
                        </div>
                    </div>

                    <SheetFooter>
                        <Button type='submit' disabled={mutation.isPending}>
                            {mutation.isPending ? '创建中...' : '创建用户'}
                        </Button>
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '创建用户失败'
}
