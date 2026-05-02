import {ConfigDrawer} from '@/components/config-drawer'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'

const shellStatus = [
    {
        label: 'Frontend shell',
        value: 'React shell is ready',
    },
    {
        label: 'Product',
        value: 'Refinex-DBFlow',
    },
    {
        label: 'Gateway',
        value: 'MCP SQL Gateway',
    },
]

export function Dashboard() {
    return (
        <>
            <Header>
                <Search/>
                <ThemeSwitch/>
                <ConfigDrawer/>
                <ProfileDropdown/>
            </Header>

            <Main>
                <section className='max-w-5xl space-y-8'>
                    <div className='space-y-2'>
                        <p className='text-sm font-medium text-muted-foreground'>
                            MCP SQL Gateway
                        </p>
                        <h1 className='text-3xl font-semibold tracking-tight'>
                            DBFlow Admin
                        </h1>
                        <p className='max-w-2xl text-sm text-muted-foreground'>
                            React shell is ready. Demo pages have been removed so the next
                            stages can rebuild DBFlow-specific operations on a clean admin
                            surface.
                        </p>
                    </div>

                    <div className='grid gap-4 md:grid-cols-3'>
                        {shellStatus.map((item) => (
                            <div
                                key={item.label}
                                className='rounded-md border bg-card p-4 text-card-foreground'
                            >
                                <p className='text-xs font-medium text-muted-foreground'>
                                    {item.label}
                                </p>
                                <p className='mt-2 text-lg font-semibold'>{item.value}</p>
                            </div>
                        ))}
                    </div>

                    <div className='rounded-md border bg-muted/30 p-4'>
                        <div className='flex items-center gap-2'>
                            <span className='size-2 rounded-full bg-emerald-500'/>
                            <p className='text-sm font-medium'>React shell is ready</p>
                        </div>
                        <p className='mt-2 text-sm text-muted-foreground'>
                            The reusable layout, UI primitives, theme controls, search shell,
                            and error routes remain available for the DBFlow admin rebuild.
                        </p>
                    </div>
                </section>
            </Main>
        </>
    )
}
