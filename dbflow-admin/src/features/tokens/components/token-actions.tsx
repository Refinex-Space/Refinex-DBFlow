import {type FormEvent, useState} from 'react'
import {useMutation, useQueryClient} from '@tanstack/react-query'
import {RefreshCw, RotateCw, Trash2, TriangleAlert} from 'lucide-react'
import {toast} from 'sonner'
import {reissueToken, revokeToken, tokenOptionsQueryKey, tokensQueryKey} from '@/api/tokens'
import type {IssuedTokenResponse, TokenRow} from '@/types/token'
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
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'

type TokenActionsProps = {
    token: TokenRow
    onTokenIssued: (token: IssuedTokenResponse) => void
}

export function TokenActions({token, onTokenIssued}: TokenActionsProps) {
    const queryClient = useQueryClient()
    const [reissueOpen, setReissueOpen] = useState(false)
    const [expiresInDays, setExpiresInDays] = useState('30')
    const [reissueError, setReissueError] = useState<string | null>(null)
    const [reissuing, setReissuing] = useState(false)

    const revokeMutation = useMutation({
        mutationFn: () => revokeToken(token.id),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({queryKey: tokensQueryKey()}),
                queryClient.invalidateQueries({queryKey: tokenOptionsQueryKey}),
            ])
            toast.success('Token 已吊销')
        },
        onError: (mutationError) => {
            toast.error(errorMessage(mutationError))
        },
    })

    function resetReissueForm() {
        setExpiresInDays('30')
        setReissueError(null)
        setReissuing(false)
    }

    async function handleReissue(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setReissueError(null)
        setReissuing(true)
        try {
            const issuedToken = await reissueToken(token.userId, {
                expiresInDays: normalizeExpiresInDays(expiresInDays),
            })
            await Promise.all([
                queryClient.invalidateQueries({queryKey: tokensQueryKey()}),
                queryClient.invalidateQueries({queryKey: tokenOptionsQueryKey}),
            ])
            toast.success('Token 已重新颁发')
            setReissueOpen(false)
            resetReissueForm()
            onTokenIssued(issuedToken)
        } catch (reissueFailure) {
            const message = errorMessage(reissueFailure)
            setReissueError(message)
            toast.error(message)
        } finally {
            setReissuing(false)
        }
    }

    return (
        <div className='flex justify-end gap-1'>
            <Dialog
                open={reissueOpen}
                onOpenChange={(nextOpen) => {
                    setReissueOpen(nextOpen)
                    if (!nextOpen) {
                        resetReissueForm()
                    }
                }}
            >
                <DialogTrigger asChild>
                    <Button
                        type='button'
                        size='sm'
                        variant='outline'
                        aria-label={`重发 ${token.username} Token`}
                    >
                        <RotateCw className='size-4'/>
                        重发
                    </Button>
                </DialogTrigger>
                <DialogContent>
                    <form className='grid gap-4' onSubmit={handleReissue}>
                        <DialogHeader>
                            <DialogTitle>重新颁发 Token</DialogTitle>
                            <DialogDescription>
                                将为 {token.username} 生成新的 MCP Token，成功后只展示一次明文。
                            </DialogDescription>
                        </DialogHeader>

                        {reissueError && (
                            <Alert variant='destructive'>
                                <TriangleAlert/>
                                <AlertTitle>重发失败</AlertTitle>
                                <AlertDescription>{reissueError}</AlertDescription>
                            </Alert>
                        )}

                        <div className='grid gap-2'>
                            <Label htmlFor={`reissue-token-${token.id}-expires`}>
                                有效天数
                            </Label>
                            <Input
                                id={`reissue-token-${token.id}-expires`}
                                type='number'
                                min={1}
                                value={expiresInDays}
                                onChange={(event) => setExpiresInDays(event.target.value)}
                            />
                        </div>

                        <DialogFooter>
                            <Button type='submit' disabled={reissuing}>
                                {reissuing ? (
                                    <>
                                        <RefreshCw className='size-4 animate-spin'/>
                                        重发中...
                                    </>
                                ) : (
                                    '确认重发'
                                )}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>

            <AlertDialog>
                <AlertDialogTrigger asChild>
                    <Button
                        type='button'
                        size='sm'
                        variant='secondary'
                        aria-label={`吊销 ${token.username} Token`}
                    >
                        <Trash2 className='size-4'/>
                        吊销
                    </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>吊销 Token</AlertDialogTitle>
                        <AlertDialogDescription>
                            将吊销 {token.username} 的 Token {token.tokenPrefix}。该操作不会展示 Token 明文。
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel disabled={revokeMutation.isPending}>
                            取消
                        </AlertDialogCancel>
                        <AlertDialogAction
                            disabled={revokeMutation.isPending}
                            onClick={() => revokeMutation.mutate()}
                        >
                            {revokeMutation.isPending ? '吊销中...' : '确认吊销'}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
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

    return error instanceof Error ? error.message : 'Token 操作失败'
}
