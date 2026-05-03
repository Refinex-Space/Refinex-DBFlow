import {
    Activity,
    KeyRound,
    LayoutDashboard,
    type LucideIcon,
    Palette,
    ScrollText,
    Settings,
    Shield,
    TriangleAlert,
    Users,
} from 'lucide-react'

export type DbflowRoute = {
    title: string
    url: string
    icon: LucideIcon
}

export type DbflowBreadcrumbItem = {
    title: string
    href?: string
}

export type DbflowRouteGroup = {
    title: string
    items: DbflowRoute[]
}

export const dbflowRouteGroups: DbflowRouteGroup[] = [
    {
        title: '工作台',
        items: [
            {
                title: '总览',
                url: '/',
                icon: LayoutDashboard,
            },
        ],
    },
    {
        title: '身份与访问',
        items: [
            {
                title: '用户管理',
                url: '/users',
                icon: Users,
            },
            {
                title: '项目授权',
                url: '/grants',
                icon: Shield,
            },
            {
                title: 'Token 管理',
                url: '/tokens',
                icon: KeyRound,
            },
        ],
    },
    {
        title: '配置与策略',
        items: [
            {
                title: '配置查看',
                url: '/config',
                icon: Settings,
            },
            {
                title: '危险策略',
                url: '/policies/dangerous',
                icon: TriangleAlert,
            },
        ],
    },
    {
        title: '审计',
        items: [
            {
                title: '审计列表',
                url: '/audit',
                icon: ScrollText,
            },
        ],
    },
    {
        title: '运维',
        items: [
            {
                title: '系统健康',
                url: '/health',
                icon: Activity,
            },
        ],
    },
    {
        title: '设置',
        items: [
            {
                title: '外观设置',
                url: '/settings/appearance',
                icon: Palette,
            },
        ],
    },
]

export const dbflowRoutes = dbflowRouteGroups.flatMap((group) => group.items)

const routeGroupsByUrl = new Map(
    dbflowRouteGroups.flatMap((group) =>
        group.items.map((item) => [item.url, group.title] as const)
    )
)

const routesByUrl = new Map(dbflowRoutes.map((route) => [route.url, route]))

function routeBreadcrumb(url: string): DbflowBreadcrumbItem[] {
    const route = routesByUrl.get(url)
    const groupTitle = routeGroupsByUrl.get(url)

    if (!route || !groupTitle) {
        throw new Error(`Unknown DBFlow admin route: ${url}`)
    }

    return [
        {title: groupTitle},
        {title: route.title, href: route.url},
    ]
}

export const dbflowBreadcrumbs = {
    dashboard: routeBreadcrumb('/'),
    users: routeBreadcrumb('/users'),
    grants: routeBreadcrumb('/grants'),
    tokens: routeBreadcrumb('/tokens'),
    config: routeBreadcrumb('/config'),
    dangerousPolicies: routeBreadcrumb('/policies/dangerous'),
    auditList: routeBreadcrumb('/audit'),
    auditDetail: [
        {title: '审计'},
        {title: '审计详情', href: '/audit'},
    ],
    health: routeBreadcrumb('/health'),
    settings: [
        {title: '设置'},
        {title: '设置首页', href: '/settings'},
    ],
    settingsProfile: [
        {title: '设置'},
        {title: '个人资料', href: '/settings'},
    ],
    settingsAccount: [
        {title: '设置'},
        {title: '账户', href: '/settings/account'},
    ],
    settingsAppearance: routeBreadcrumb('/settings/appearance'),
    settingsNotifications: [
        {title: '设置'},
        {title: '通知', href: '/settings/notifications'},
    ],
    settingsDisplay: [
        {title: '设置'},
        {title: '显示', href: '/settings/display'},
    ],
} satisfies Record<string, DbflowBreadcrumbItem[]>

const breadcrumbsByPath = new Map<string, DbflowBreadcrumbItem[]>([
    ['/', dbflowBreadcrumbs.dashboard],
    ['/users', dbflowBreadcrumbs.users],
    ['/grants', dbflowBreadcrumbs.grants],
    ['/tokens', dbflowBreadcrumbs.tokens],
    ['/config', dbflowBreadcrumbs.config],
    ['/policies/dangerous', dbflowBreadcrumbs.dangerousPolicies],
    ['/audit', dbflowBreadcrumbs.auditList],
    ['/health', dbflowBreadcrumbs.health],
    ['/settings', dbflowBreadcrumbs.settingsProfile],
    ['/settings/account', dbflowBreadcrumbs.settingsAccount],
    ['/settings/appearance', dbflowBreadcrumbs.settingsAppearance],
    ['/settings/notifications', dbflowBreadcrumbs.settingsNotifications],
    ['/settings/display', dbflowBreadcrumbs.settingsDisplay],
])

export function dbflowBreadcrumbForPath(pathname: string): DbflowBreadcrumbItem[] {
    const normalizedPathname = normalizePathname(pathname)

    if (/^\/audit\/[^/]+$/.test(normalizedPathname)) {
        return dbflowBreadcrumbs.auditDetail
    }

    return breadcrumbsByPath.get(normalizedPathname) ?? dbflowBreadcrumbs.dashboard
}

function normalizePathname(pathname: string): string {
    const withoutAdminPrefix = pathname.startsWith('/admin/')
        ? pathname.slice('/admin'.length)
        : pathname
    const withoutTrailingSlash =
        withoutAdminPrefix.length > 1
            ? withoutAdminPrefix.replace(/\/+$/, '')
            : withoutAdminPrefix

    return withoutTrailingSlash || '/'
}
