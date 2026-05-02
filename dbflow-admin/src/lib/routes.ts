import type {LucideIcon} from 'lucide-react'
import {
    Activity,
    KeyRound,
    LayoutDashboard,
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
