import {createFileRoute} from '@tanstack/react-router'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'

export const Route = createFileRoute('/_authenticated/users')({
    component: UsersPlaceholder,
})

function UsersPlaceholder() {
    return (
        <>
            <Header>
                <Search/>
                <ThemeSwitch/>
                <ProfileDropdown/>
            </Header>
            <Main>
                <section className='max-w-3xl space-y-3'>
                    <p className='text-sm font-medium text-muted-foreground'>
                        Identity and access
                    </p>
                    <h1 className='text-2xl font-semibold tracking-tight'>
                        User Management
                    </h1>
                    <p className='text-sm text-muted-foreground'>
                        DBFlow user management will be rebuilt on this protected route in
                        the next stage.
                    </p>
                </section>
            </Main>
        </>
    )
}
