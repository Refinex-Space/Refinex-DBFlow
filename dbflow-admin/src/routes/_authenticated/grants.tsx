import {z} from 'zod'
import {createFileRoute, useNavigate, useSearch} from '@tanstack/react-router'
import {GrantsPage, type GrantsPageSearch} from '@/features/grants'

const searchSchema = z.object({
    username: z.string().optional(),
    projectKey: z.string().optional(),
    environmentKey: z.string().optional(),
    status: z.string().optional(),
})

export const Route = createFileRoute('/_authenticated/grants')({
    validateSearch: searchSchema,
    component: GrantsRoute,
})

function GrantsRoute() {
    const search = useSearch({from: '/_authenticated/grants'})
    const navigate = useNavigate()

    return (
        <GrantsPage
            search={search}
            onSearchChange={(nextSearch) =>
                navigate({
                    to: '/grants',
                    search: cleanRouteSearch(nextSearch),
                    replace: true,
                })
            }
        />
    )
}

function cleanRouteSearch(search: GrantsPageSearch): GrantsPageSearch {
    return {
        username: cleanOptionalString(search.username),
        projectKey: cleanOptionalString(search.projectKey),
        environmentKey: cleanOptionalString(search.environmentKey),
        status: cleanOptionalString(search.status),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
