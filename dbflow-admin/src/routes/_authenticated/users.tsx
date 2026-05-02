import {z} from 'zod'
import {createFileRoute, useNavigate, useSearch} from '@tanstack/react-router'
import {UsersPage, type UsersPageSearch} from '@/features/users'

const searchSchema = z.object({
    username: z.string().optional(),
    status: z.string().optional(),
})

export const Route = createFileRoute('/_authenticated/users')({
    validateSearch: searchSchema,
    component: UsersRoute,
})

function UsersRoute() {
    const search = useSearch({from: '/_authenticated/users'})
    const navigate = useNavigate()

    return (
        <UsersPage
            search={search}
            onSearchChange={(nextSearch) =>
                navigate({
                    to: '/users',
                    search: cleanRouteSearch(nextSearch),
                    replace: true,
                })
            }
        />
    )
}

function cleanRouteSearch(search: UsersPageSearch): UsersPageSearch {
    return {
        username: cleanOptionalString(search.username),
        status: cleanOptionalString(search.status),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}
