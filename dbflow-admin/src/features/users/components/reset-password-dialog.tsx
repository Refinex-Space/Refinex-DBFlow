import {type FormEvent, useState} from 'react'
import {useMutation, useQueryClient} from '@tanstack/react-query'
import {TriangleAlert} from 'lucide-react'
import {toast} from 'sonner'
import {resetUserPassword, usersQueryKey} from '@/api/users'
import type {AdminUserRow} from '@/types/access'
import {isApiClientError} from '@/lib/errors'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog'
import {Label} from '@/components/ui/label'
import {PasswordInput} from '@/components/password-input'

type ResetPasswordDialogProps = {
    user: AdminUserRow
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function ResetPasswordDialog({
                                        user,
                                        open,
                                        onOpenChange,
                                    }: ResetPasswordDialogProps) {
    const queryClient = useQueryClient()
    const [newPassword, setNewPassword] = useState('')
    const [error, setError] = useState<string | null>(null)

    const mutation = useMutation({
        mutationFn: () => resetUserPassword(user.id, {newPassword}),
        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: usersQueryKey()})
            toast.success('密码已重置')
            setNewPassword('')
            setError(null)
            onOpenChange(false)
        },
        onError: (mutationError) => {
            const message = errorMessage(mutationError)
            setError(message)
            toast.error(message)
        },
    })

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)
        mutation.mutate()
    }

    return (
        <Dialog
            open={open}
            onOpenChange={(nextOpen) => {
                onOpenChange(nextOpen)
                if (!nextOpen) {
                    setNewPassword('')
                    setError(null)
                }
            }}
        >
            <DialogContent>
                <form className='space-y-4' onSubmit={handleSubmit}>
                    <DialogHeader>
                        <DialogTitle>重置密码</DialogTitle>
                        <DialogDescription>
                            为用户 {user.username} 设置新的管理端登录密码。
                        </DialogDescription>
                    </DialogHeader>

                    {error && (
                        <Alert variant='destructive'>
                            <TriangleAlert/>
                            <AlertTitle>重置失败</AlertTitle>
                            <AlertDescription>{error}</AlertDescription>
                        </Alert>
                    )}

                    <div className='grid gap-2'>
                        <Label htmlFor={`reset-password-${user.id}`}>新密码</Label>
                        <PasswordInput
                            id={`reset-password-${user.id}`}
                            value={newPassword}
                            autoComplete='new-password'
                            onChange={(event) => setNewPassword(event.target.value)}
                        />
                    </div>

                    <DialogFooter>
                        <Button type='submit' disabled={mutation.isPending}>
                            {mutation.isPending ? '重置中...' : '重置密码'}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '重置密码失败'
}
