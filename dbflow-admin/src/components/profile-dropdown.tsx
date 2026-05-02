import {Link} from '@tanstack/react-router'
import {Palette, ShieldCheck, UserRound} from 'lucide-react'
import {useSessionStore} from '@/stores/session-store'
import useDialogState from '@/hooks/use-dialog-state'
import {Avatar, AvatarFallback, AvatarImage} from '@/components/ui/avatar'
import {Button} from '@/components/ui/button'
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {SignOutDialog} from '@/components/sign-out-dialog'

export function ProfileDropdown() {
    const [open, setOpen] = useDialogState()
    const session = useSessionStore((state) => state.session)
    const username = session?.username ?? 'admin'
    const displayName = session?.displayName || username

    return (
        <>
            <DropdownMenu modal={false}>
                <DropdownMenuTrigger asChild>
                    <Button variant='ghost' className='relative h-8 w-8 rounded-full'>
                        <Avatar className='h-8 w-8'>
                            <AvatarImage src='/images/favicon.png' alt={displayName}/>
                            <AvatarFallback>DB</AvatarFallback>
                        </Avatar>
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className='w-56' align='end' forceMount>
                    <DropdownMenuLabel className='font-normal'>
                        <div className='flex flex-col gap-1.5'>
                            <p className='text-sm leading-none font-medium'>{displayName}</p>
                            <p className='text-xs leading-none text-muted-foreground'>
                                {username} · DBFlow Administrator
                            </p>
                        </div>
                    </DropdownMenuLabel>
                    <DropdownMenuSeparator/>
                    <DropdownMenuGroup>
                        <DropdownMenuItem asChild>
                            <Link to='/settings'>
                                <UserRound/>
                                管理员信息
                            </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                            <Link to='/settings/appearance'>
                                <Palette/>
                                外观设置
                            </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem>
                            <ShieldCheck/>
                            MCP SQL Gateway
                        </DropdownMenuItem>
                    </DropdownMenuGroup>
                    <DropdownMenuSeparator/>
                    <DropdownMenuItem variant='destructive' onClick={() => setOpen(true)}>
                        退出登录
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <SignOutDialog open={!!open} onOpenChange={setOpen}/>
        </>
    )
}
