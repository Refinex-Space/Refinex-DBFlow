import type {IssuedTokenResponse} from '@/types/token'
import {formatDateTime, formatText} from '@/lib/format'
import {CopyButton} from '@/components/dbflow/copy-button'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog'

type TokenRevealDialogProps = {
    token: IssuedTokenResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function TokenRevealDialog({
                                      token,
                                      open,
                                      onOpenChange,
                                  }: TokenRevealDialogProps) {
    if (!token) {
        return null
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className='sm:max-w-xl'>
                <DialogHeader>
                    <DialogTitle>保存 MCP Token 明文</DialogTitle>
                    <DialogDescription>
                        该明文只在本次响应中展示。关闭后页面会立即清空本地明文状态。
                    </DialogDescription>
                </DialogHeader>

                <div className='grid gap-4'>
                    <Alert>
                        <AlertTitle>一次性展示</AlertTitle>
                        <AlertDescription>
                            请立即保存到受控凭据系统。列表、审计和后续页面不会再次显示该明文。
                        </AlertDescription>
                    </Alert>

                    <div className='grid gap-3 rounded-md border bg-muted/40 p-4 text-sm'>
                        <div className='flex items-center justify-between gap-3'>
                            <span className='text-muted-foreground'>用户</span>
                            <span className='font-medium'>{formatText(token.username)}</span>
                        </div>
                        <div className='flex items-center justify-between gap-3'>
                            <span className='text-muted-foreground'>Token ID</span>
                            <span className='font-mono text-xs'>{token.tokenId}</span>
                        </div>
                        <div className='flex items-center justify-between gap-3'>
                            <span className='text-muted-foreground'>Prefix</span>
                            <Badge variant='outline' className='font-mono'>
                                {formatText(token.tokenPrefix)}
                            </Badge>
                        </div>
                        <div className='flex items-center justify-between gap-3'>
                            <span className='text-muted-foreground'>过期时间</span>
                            <span>{formatDateTime(token.expiresAt)}</span>
                        </div>
                    </div>

                    <div className='grid gap-2'>
                        <div className='flex items-center justify-between gap-3'>
                            <span className='text-sm font-medium'>plaintextToken</span>
                            <CopyButton
                                value={token.plaintextToken}
                                label='复制明文'
                                successMessage='Token 明文已复制'
                            />
                        </div>
                        <code
                            className='block max-h-32 overflow-auto rounded-md border bg-background p-3 font-mono text-xs break-all'>
                            {token.plaintextToken}
                        </code>
                    </div>
                </div>

                <DialogFooter>
                    <Button type='button' onClick={() => onOpenChange(false)}>
                        我已保存，关闭
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
