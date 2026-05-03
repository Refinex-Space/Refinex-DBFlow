import {createFileRoute} from '@tanstack/react-router'
import {Button} from '@/components/ui/button'
import {ConfigDrawer} from '@/components/config-drawer'
import {PageHeader} from '@/components/dbflow/page-header'
import {Header} from '@/components/layout/header'
import {Main} from '@/components/layout/main'
import {ProfileDropdown} from '@/components/profile-dropdown'
import {Search} from '@/components/search'
import {ThemeSwitch} from '@/components/theme-switch'

export const Route = createFileRoute('/_authenticated/audit/$eventId')({
    component: AuditDetailRoute,
})

function AuditDetailRoute() {
    const {eventId} = Route.useParams()

    return (
        <>
            <Header>
                <Search/>
                <ThemeSwitch/>
                <ConfigDrawer/>
                <ProfileDropdown/>
            </Header>
            <Main>
                <section className='space-y-6'>
                    <PageHeader
                        eyebrow='审计'
                        title={`审计事件 #${eventId}`}
                        description='审计详情页将在后续阶段接入完整 JSON detail API；当前路由用于承接列表详情导航。'
                        actions={
                            <Button variant='outline' asChild>
                                <a
                                    href={`${import.meta.env.BASE_URL.replace(/\/+$/, '')}/audit`}
                                >
                                    返回审计列表
                                </a>
                            </Button>
                        }
                    />
                </section>
            </Main>
        </>
    )
}
