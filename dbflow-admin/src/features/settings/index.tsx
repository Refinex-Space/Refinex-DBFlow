import {Outlet, useLocation} from '@tanstack/react-router'
import {Bell, Monitor, Palette, UserCog, Wrench} from 'lucide-react'
import {dbflowBreadcrumbForPath} from '@/lib/routes'
import {ConfigDrawer} from '@/components/config-drawer'
import {PageBreadcrumb} from '@/components/dbflow/page-breadcrumb'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'
import {SidebarNav} from './components/sidebar-nav'

const sidebarNavItems = [
    {
        title: '个人资料',
        href: '/settings',
        icon: <UserCog size={18}/>,
    },
    {
        title: '账户',
        href: '/settings/account',
        icon: <Wrench size={18}/>,
    },
    {
        title: '外观',
        href: '/settings/appearance',
        icon: <Palette size={18}/>,
    },
    {
        title: '通知',
        href: '/settings/notifications',
        icon: <Bell size={18}/>,
    },
    {
        title: '显示',
        href: '/settings/display',
        icon: <Monitor size={18}/>,
    },
]

export function Settings() {
    const {pathname} = useLocation()

    return (
        <>
            <Header>
                <Search className='me-auto'/>
                <ThemeSwitch/>
                <ConfigDrawer/>
                <ProfileDropdown/>
            </Header>

            <Main fixed>
                <PageBreadcrumb items={dbflowBreadcrumbForPath(pathname)}/>
                <div
                    className='mt-4 flex flex-1 flex-col space-y-2 overflow-hidden md:space-y-2 lg:flex-row lg:space-y-0 lg:space-x-10'>
                    <aside className='top-0 lg:sticky lg:w-1/5'>
                        <SidebarNav items={sidebarNavItems}/>
                    </aside>
                    <div className='flex w-full overflow-y-hidden p-1'>
                        <Outlet/>
                    </div>
                </div>
            </Main>
        </>
    )
}
