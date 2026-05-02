import {type FormEvent, useEffect, useState} from 'react'
import {Search} from 'lucide-react'
import type {GrantFilters} from '@/types/access'
import {cn} from '@/lib/utils'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'

type GrantFilterBarProps = {
    search: GrantFilters
    onSearchChange: (search: GrantFilters) => void
}

export function GrantFilterBar({search, onSearchChange}: GrantFilterBarProps) {
    const [username, setUsername] = useState(search.username ?? '')
    const [projectKey, setProjectKey] = useState(search.projectKey ?? '')
    const [environmentKey, setEnvironmentKey] = useState(search.environmentKey ?? '')
    const [status, setStatus] = useState(search.status ?? '')

    useEffect(() => {
        setUsername(search.username ?? '')
        setProjectKey(search.projectKey ?? '')
        setEnvironmentKey(search.environmentKey ?? '')
        setStatus(search.status ?? '')
    }, [search.username, search.projectKey, search.environmentKey, search.status])

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        onSearchChange(cleanFilters({username, projectKey, environmentKey, status}))
    }

    function handleReset() {
        setUsername('')
        setProjectKey('')
        setEnvironmentKey('')
        setStatus('')
        onSearchChange({})
    }

    return (
        <form
            className='grid gap-3 rounded-md border bg-card/50 p-4 lg:grid-cols-[repeat(4,minmax(140px,1fr))_auto] lg:items-end'
            onSubmit={handleSubmit}
        >
            <div className='grid gap-2'>
                <Label htmlFor='grants-filter-username'>用户</Label>
                <Input
                    id='grants-filter-username'
                    value={username}
                    placeholder='alice'
                    onChange={(event) => setUsername(event.target.value)}
                />
            </div>
            <div className='grid gap-2'>
                <Label htmlFor='grants-filter-project'>项目</Label>
                <Input
                    id='grants-filter-project'
                    value={projectKey}
                    placeholder='billing-core'
                    onChange={(event) => setProjectKey(event.target.value)}
                />
            </div>
            <div className='grid gap-2'>
                <Label htmlFor='grants-filter-environment'>环境</Label>
                <Input
                    id='grants-filter-environment'
                    value={environmentKey}
                    placeholder='prod'
                    onChange={(event) => setEnvironmentKey(event.target.value)}
                />
            </div>
            <div className='grid gap-2'>
                <Label htmlFor='grants-filter-status'>状态</Label>
                <select
                    id='grants-filter-status'
                    value={status}
                    className={selectClassName}
                    onChange={(event) => setStatus(event.target.value)}
                >
                    <option value=''>全部状态</option>
                    <option value='ACTIVE'>ACTIVE</option>
                    <option value='REVOKED'>REVOKED</option>
                </select>
            </div>
            <div className='flex gap-2'>
                <Button type='submit' className='flex-1 lg:flex-none'>
                    <Search className='size-4'/>
                    应用筛选
                </Button>
                <Button
                    type='button'
                    variant='outline'
                    className='flex-1 lg:flex-none'
                    onClick={handleReset}
                >
                    重置
                </Button>
            </div>
        </form>
    )
}

function cleanFilters(filters: GrantFilters): GrantFilters {
    return {
        username: cleanOptionalString(filters.username),
        projectKey: cleanOptionalString(filters.projectKey),
        environmentKey: cleanOptionalString(filters.environmentKey),
        status: cleanOptionalString(filters.status),
    }
}

function cleanOptionalString(value: string | undefined): string | undefined {
    const text = value?.trim()
    return text ? text : undefined
}

const selectClassName = cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors',
    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
)
