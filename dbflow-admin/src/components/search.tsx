import {SearchIcon} from 'lucide-react'
import {useSessionStore} from '@/stores/session-store'
import {cn} from '@/lib/utils'
import {useSearch} from '@/context/search-provider'
import {Badge} from './ui/badge'
import {Button} from './ui/button'

export function Search({
                           className = '',
                           placeholder = '搜索 DBFlow 页面',
                           ...props
                       }: React.ComponentProps<'button'> & { placeholder?: string }) {
    const {setOpen} = useSearch()
    const shell = useSessionStore((state) => state.session?.shell)
    const status = shell?.mcpStatus ?? 'UNKNOWN'
    const configSourceLabel = shell?.configSourceLabel ?? '等待 Session'

    return (
        <div className={cn('flex min-w-0 flex-1 items-center gap-2', className)}>
            <div className='hidden min-w-0 items-center gap-2 lg:flex'>
                <Badge
                    variant='outline'
                    className={cn(
                        'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300',
                        status !== 'HEALTHY' &&
                        'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300'
                    )}
                >
                    <span className='size-1.5 rounded-full bg-current'/>
                    MCP {status}
                </Badge>
                <span className='max-w-56 truncate text-xs text-muted-foreground'>
          {configSourceLabel}
        </span>
            </div>
            <Button
                {...props}
                variant='outline'
                className='group relative h-8 w-full justify-start rounded-md bg-muted/25 text-sm font-normal text-muted-foreground shadow-none hover:bg-accent sm:w-40 sm:pe-12 md:flex-none lg:w-52 xl:w-64'
                aria-keyshortcuts='Meta+K Control+K'
                onClick={() => setOpen(true)}
            >
                <SearchIcon
                    aria-hidden='true'
                    className='absolute inset-s-1.5 top-1/2 -translate-y-1/2'
                    size={16}
                />
                <span className='ms-4'>{placeholder}</span>
                <kbd
                    className='pointer-events-none absolute inset-e-[0.3rem] top-[0.3rem] hidden h-5 items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium opacity-100 select-none group-hover:bg-accent sm:flex'>
                    <span className='text-xs'>⌘</span>K
                </kbd>
            </Button>
        </div>
    )
}
