import type {AuditEventFilters} from '@/types/audit'
import {auditResetSearch} from '@/api/audit'
import {formatText} from '@/lib/format'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'

type AuditFilterChipsProps = {
    filters: AuditEventFilters
    onSearchChange: (search: AuditEventFilters) => void
}

const filterLabels: Array<{
    key: keyof AuditEventFilters
    label: string
}> = [
    {key: 'from', label: '起始时间'},
    {key: 'to', label: '结束时间'},
    {key: 'userId', label: '用户 ID'},
    {key: 'project', label: '项目'},
    {key: 'env', label: '环境'},
    {key: 'risk', label: 'Risk'},
    {key: 'decision', label: 'Decision'},
    {key: 'sqlHash', label: 'SQL Hash'},
    {key: 'tool', label: 'Tool'},
]

export function AuditFilterChips({
                                     filters,
                                     onSearchChange,
                                 }: AuditFilterChipsProps) {
    const activeFilters = filterLabels
        .map((item) => ({
            ...item,
            value: filters[item.key],
        }))
        .filter((item) => item.value !== undefined && item.value !== '')

    if (activeFilters.length === 0) {
        return null
    }

    return (
        <div
            aria-label='已应用审计筛选'
            className='flex flex-wrap items-center gap-2 rounded-md border bg-card/50 p-3'
        >
            <span className='text-sm text-muted-foreground'>已应用筛选：</span>
            {activeFilters.map((item) => (
                <Badge
                    key={item.key}
                    variant='outline'
                    className='gap-1 rounded-full bg-background'
                >
                    <span className='text-muted-foreground'>{item.label}</span>
                    <span>{formatText(item.value)}</span>
                </Badge>
            ))}
            <Button
                type='button'
                variant='ghost'
                size='sm'
                className='ms-auto h-7'
                onClick={() => onSearchChange(auditResetSearch(filters))}
            >
                重置筛选
            </Button>
        </div>
    )
}
