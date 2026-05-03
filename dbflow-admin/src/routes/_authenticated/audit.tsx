import {z} from 'zod'
import {createFileRoute, useNavigate, useSearch} from '@tanstack/react-router'
import {AuditListPage, type AuditListPageSearch} from '@/features/audit/list'

const searchSchema = z.object({
    from: z.string().optional(),
    to: z.string().optional(),
    userId: z.string().optional(),
    project: z.string().optional(),
    env: z.string().optional(),
    risk: z.string().optional(),
    decision: z.string().optional(),
    sqlHash: z.string().optional(),
    tool: z.string().optional(),
    page: z.number().optional(),
    size: z.number().optional(),
    sort: z.string().optional(),
    direction: z.string().optional(),
})

export const Route = createFileRoute('/_authenticated/audit')({
    validateSearch: searchSchema,
    component: AuditRoute,
})

function AuditRoute() {
    const search = useSearch({from: '/_authenticated/audit'})
    const navigate = useNavigate()

    return (
        <AuditListPage
            search={search}
            onSearchChange={(nextSearch) =>
                navigate({
                    to: '/audit',
                    search: cleanRouteSearch(nextSearch),
                    replace: true,
                })
            }
        />
    )
}

function cleanRouteSearch(search: AuditListPageSearch): AuditListPageSearch {
    return {
        from: cleanOptionalString(search.from),
        to: cleanOptionalString(search.to),
        userId: cleanOptionalString(search.userId),
        project: cleanOptionalString(search.project),
        env: cleanOptionalString(search.env),
        risk: cleanOptionalString(search.risk),
        decision: cleanOptionalString(search.decision),
        sqlHash: cleanOptionalString(search.sqlHash),
        tool: cleanOptionalString(search.tool),
        page: search.page,
        size: search.size,
        sort: cleanOptionalString(search.sort),
        direction: cleanOptionalString(search.direction),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
