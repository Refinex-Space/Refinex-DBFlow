import {useState} from 'react'
import {useMutation, useQueryClient} from '@tanstack/react-query'
import {KeyRound, Lock, Unlock} from 'lucide-react'
import {toast} from 'sonner'
import {disableUser, enableUser, usersQueryKey} from '@/api/users'
import type {AdminUserRow} from '@/types/access'
import {isApiClientError} from '@/lib/errors'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import {Button} from '@/components/ui/button'
import {ResetPasswordDialog} from './reset-password-dialog'

type UserActionsProps = {
    user: AdminUserRow
}

export function UserActions({user}: UserActionsProps) {
    const queryClient = useQueryClient()
    const [resetOpen, setResetOpen] = useState(false)
    const isDisabled = user.status === 'DISABLED'
    const actionLabel = isDisabled ? '启用' : '禁用'
    const confirmLabel = isDisabled ? '确认启用' : '确认禁用'

    const toggleMutation = useMutation({
        mutationFn: () => (isDisabled ? enableUser(user.id) : disableUser(user.id)),
        onSuccess: async () => {
            await queryClient.invalidateQueries({queryKey: usersQueryKey()})
            toast.success(isDisabled ? '用户已启用' : '用户已禁用')
        },
        onError: (mutationError) => {
            toast.error(errorMessage(mutationError))
        },
    })

    return (
        <div className='flex justify-end gap-1'>
            <Button
                type='button'
                size='sm'
                variant='outline'
                aria-label={`重置 ${user.username} 密码`}
                onClick={() => setResetOpen(true)}
            >
                <KeyRound className='size-4'/>
                重置
            </Button>

            <AlertDialog>
                <AlertDialogTrigger asChild>
                    <Button
                        type='button'
                        size='sm'
                        variant={isDisabled ? 'outline' : 'secondary'}
                        aria-label={`${actionLabel} ${user.username}`}
                    >
                        {isDisabled ? (
                            <Unlock className='size-4'/>
                        ) : (
                            <Lock className='size-4'/>
                        )}
                        {actionLabel}
                    </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{actionLabel}用户</AlertDialogTitle>
                        <AlertDialogDescription>
                            将对用户 {user.username} 执行{actionLabel}操作。该操作会影响其
                            MCP SQL Gateway 访问能力。
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel disabled={toggleMutation.isPending}>
                            取消
                        </AlertDialogCancel>
                        <AlertDialogAction
                            disabled={toggleMutation.isPending}
                            onClick={() => {
                                toggleMutation.mutate()
                            }}
                        >
                            {toggleMutation.isPending ? '处理中...' : confirmLabel}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <ResetPasswordDialog
                user={user}
                open={resetOpen}
                onOpenChange={setResetOpen}
            />
        </div>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '用户操作失败'
}
