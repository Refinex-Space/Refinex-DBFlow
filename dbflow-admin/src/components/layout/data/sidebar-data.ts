import {Database} from 'lucide-react'
import {dbflowRouteGroups} from '@/lib/routes'
import {type SidebarData} from '../types'

export const sidebarData: Omit<SidebarData, 'user'> = {
    teams: [
        {
            name: 'DBFlow Admin',
            logo: Database,
            plan: 'MCP SQL Gateway',
        },
    ],
    navGroups: dbflowRouteGroups,
}
