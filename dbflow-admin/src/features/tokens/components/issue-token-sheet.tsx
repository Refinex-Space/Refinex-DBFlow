import {type FormEvent, useState} from 'react'
import {useQueryClient} from '@tanstack/react-query'
import {KeyRound, TriangleAlert} from 'lucide-react'
import {toast} from 'sonner'
import {issueToken, tokenOptionsQueryKey, tokensQueryKey} from '@/api/tokens'
import type {IssuedTokenResponse, TokenOptionsResponse} from '@/types/token'
import {isApiClientError} from '@/lib/errors'
import {cn} from '@/lib/utils'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from '@/components/ui/sheet'

type IssueTokenSheetProps = {
    options?: TokenOptionsResponse
    onTokenIssued: (token: IssuedTokenResponse) => void
}

export function IssueTokenSheet({options, onTokenIssued}: IssueTokenSheetProps) {
    const queryClient = useQueryClient()
    const [open, setOpen] = useState(false)
    const [userId, setUserId] = useState('')
    const [expiresInDays, setExpiresInDays] = useState('30')
    const [error, setError] = useState<string | null>(null)
    const [submitting, setSubmitting] = useState(false)
    const users = options?.users ?? []
    const hasUsers = users.length > 0

    function resetForm() {
        setUserId('')
        setExpiresInDays('30')
        setError(null)
        setSubmitting(false)
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)

        if (!userId) {
            setError('请选择授权用户。')
            return
        }

        setSubmitting(true)
        try {
            const token = await issueToken({
                userId: Number(userId),
                expiresInDays: normalizeExpiresInDays(expiresInDays),
            })
            await Promise.all([
                queryClient.invalidateQueries({queryKey: tokensQueryKey()}),
                queryClient.invalidateQueries({queryKey: tokenOptionsQueryKey}),
            ])
            toast.success('Token 已颁发')
            setOpen(false)
            resetForm()
            onTokenIssued(token)
        } catch (issueError) {
            const message = errorMessage(issueError)
            setError(message)
            toast.error(message)
        } finally {
            setSubmitting(false)
        }
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
                    <KeyRound className='size-4'/>
                    颁发 Token
                </Button>
            </SheetTrigger>
            <SheetContent className='sm:max-w-md'>
                <form className='flex h-full flex-col' onSubmit={handleSubmit}>
                    <SheetHeader>
                        <SheetTitle>颁发 Token</SheetTitle>
                        <SheetDescription>
                            为 active 用户生成新的 MCP 访问 Token。明文只会在保存后展示一次。
                        </SheetDescription>
                    </SheetHeader>

                    <div className='grid gap-4 px-4'>
                        {!hasUsers && (
                            <Alert>
                                <TriangleAlert/>
                                <AlertTitle>没有可颁发用户</AlertTitle>
                                <AlertDescription>
                                    请先创建或启用 DBFlow 用户，再颁发 MCP Token。
                                </AlertDescription>
                            </Alert>
                        )}

                        {error && (
                            <Alert variant='destructive'>
                                <TriangleAlert/>
                                <AlertTitle>颁发失败</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}

                        <div className='grid gap-2'>
                            <Label htmlFor='issue-token-user'>授权用户</Label>
                            <select
                                id='issue-token-user'
                                value={userId}
                                className={selectClassName}
                                disabled={!hasUsers}
                                onChange={(event) => setUserId(event.target.value)}
                            >
                                <option value=''>请选择用户</option>
                                {users.map((user) => (
                                    <option key={user.id} value={user.id}>
                                        {user.username} / {user.displayName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className='grid gap-2'>
                            <Label htmlFor='issue-token-expires'>有效天数</Label>
                            <Input
                                id='issue-token-expires'
                                type='number'
                                min={1}
                                value={expiresInDays}
                                disabled={!hasUsers}
                                onChange={(event) => setExpiresInDays(event.target.value)}
                            />
                        </div>
                    </div>

                    <SheetFooter>
                        <Button type='submit' disabled={submitting || !hasUsers}>
                            {submitting ? '颁发中...' : '确认颁发'}
                        </Button>
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    )
}

function normalizeExpiresInDays(value: string): number {
    const parsed = Number(value)
    return Number.isFinite(parsed) && parsed > 0 ? Math.trunc(parsed) : 30
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '颁发 Token 失败'
}

const selectClassName = cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors',
    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
)
