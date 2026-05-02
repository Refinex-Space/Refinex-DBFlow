import React from 'react'
import {useNavigate} from '@tanstack/react-router'
import {Laptop, Moon, Sun} from 'lucide-react'
import {dbflowRouteGroups} from '@/lib/routes'
import {useSearch} from '@/context/search-provider'
import {useTheme} from '@/context/theme-provider'
import {
    CommandDialog,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    CommandSeparator,
} from '@/components/ui/command'
import {ScrollArea} from './ui/scroll-area'

export function CommandMenu() {
    const navigate = useNavigate()
    const {setTheme} = useTheme()
    const {open, setOpen} = useSearch()

    const runCommand = React.useCallback(
        (command: () => unknown) => {
            setOpen(false)
            command()
        },
        [setOpen]
    )

    return (
        <CommandDialog modal open={open} onOpenChange={setOpen}>
            <CommandInput placeholder='搜索 DBFlow 页面或命令...'/>
            <CommandList>
                <ScrollArea type='hover' className='h-72 pe-1'>
                    <CommandEmpty>没有匹配结果。</CommandEmpty>
                    {dbflowRouteGroups.map((group) => (
                        <CommandGroup key={group.title} heading={group.title}>
                            {group.items.map((navItem) => (
                                <CommandItem
                                    key={navItem.url}
                                    value={`${group.title} ${navItem.title}`}
                                    onSelect={() => {
                                        runCommand(() => navigate({to: navItem.url}))
                                    }}
                                >
                                    <navItem.icon className='text-muted-foreground/80'/>
                                    {navItem.title}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    ))}
                    <CommandSeparator/>
                    <CommandGroup heading='主题'>
                        <CommandItem onSelect={() => runCommand(() => setTheme('light'))}>
                            <Sun/> <span>亮色</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => setTheme('dark'))}>
                            <Moon className='scale-90'/>
                            <span>暗色</span>
                        </CommandItem>
                        <CommandItem onSelect={() => runCommand(() => setTheme('system'))}>
                            <Laptop/>
                            <span>跟随系统</span>
                        </CommandItem>
                    </CommandGroup>
                </ScrollArea>
            </CommandList>
        </CommandDialog>
    )
}
