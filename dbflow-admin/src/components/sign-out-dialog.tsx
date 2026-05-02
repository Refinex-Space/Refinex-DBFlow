import {useState} from 'react'
import {useLocation, useNavigate} from '@tanstack/react-router'
import {toast} from 'sonner'
import {logout} from '@/api/session'
import {useSessionStore} from '@/stores/session-store'
import {ConfirmDialog} from '@/components/confirm-dialog'

interface SignOutDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function SignOutDialog({open, onOpenChange}: SignOutDialogProps) {
    const navigate = useNavigate()
    const location = useLocation()
    const clearSession = useSessionStore((state) => state.clearSession)
    const [isLoggingOut, setIsLoggingOut] = useState(false)

    const handleSignOut = async () => {
        setIsLoggingOut(true)
        try {
            await logout()
            clearSession()
            onOpenChange(false)

            const currentPath = location.href
            navigate({
                to: '/login',
                search: {redirect: currentPath},
                replace: true,
            })
        } catch {
            toast.error('退出登录失败，请稍后重试')
        } finally {
            setIsLoggingOut(false)
        }
    }

    return (
        <ConfirmDialog
            open={open}
            onOpenChange={onOpenChange}
            title='退出登录'
            desc='当前管理端会话将失效，再次访问 DBFlow Admin 需要重新登录。'
            cancelBtnText='取消'
            confirmText={isLoggingOut ? '正在退出...' : '退出登录'}
            destructive
            isLoading={isLoggingOut}
            handleConfirm={handleSignOut}
            className='sm:max-w-sm'
        />
    )
}
