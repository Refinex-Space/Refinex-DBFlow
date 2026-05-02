import {z} from 'zod'
import {createFileRoute, useNavigate, useSearch} from '@tanstack/react-router'
import {TokensPage, type TokensPageSearch} from '@/features/tokens'

const searchSchema = z.object({
    username: z.string().optional(),
    status: z.string().optional(),
})

export const Route = createFileRoute('/_authenticated/tokens')({
    validateSearch: searchSchema,
    component: TokensRoute,
})

function TokensRoute() {
    const search = useSearch({from: '/_authenticated/tokens'})
    const navigate = useNavigate()

    return (
        <TokensPage
            search={search}
            onSearchChange={(nextSearch) =>
                navigate({
                    to: '/tokens',
                    search: cleanRouteSearch(nextSearch),
                    replace: true,
                })
            }
        />
    )
}

function cleanRouteSearch(search: TokensPageSearch): TokensPageSearch {
    return {
        username: cleanOptionalString(search.username),
        status: cleanOptionalString(search.status),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
