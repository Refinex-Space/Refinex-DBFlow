import {type FormEvent, useEffect, useState} from 'react'
import type {AuditEventFilters} from '@/types/audit'
import {Filter} from 'lucide-react'
import {AUDIT_DEFAULT_DIRECTION, AUDIT_DEFAULT_SORT, normalizeAuditFilters,} from '@/api/audit'
import {cn} from '@/lib/utils'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from '@/components/ui/sheet'

type AuditFilterSheetProps = {
    filters: AuditEventFilters
    onSearchChange: (search: AuditEventFilters) => void
}

type FilterFormState = {
    from: string
    to: string
    userId: string
    project: string
    env: string
    risk: string
    decision: string
    sqlHash: string
    tool: string
    size: string
}

export function AuditFilterSheet({
                                     filters,
                                     onSearchChange,
                                 }: AuditFilterSheetProps) {
    const [open, setOpen] = useState(false)
    const [form, setForm] = useState<FilterFormState>(() => formState(filters))

    useEffect(() => {
        setForm(formState(filters))
    }, [
        filters.from,
        filters.to,
        filters.userId,
        filters.project,
        filters.env,
        filters.risk,
        filters.decision,
        filters.sqlHash,
        filters.tool,
        filters.size,
    ])

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        onSearchChange(
            normalizeAuditFilters({
                from: form.from,
                to: form.to,
                userId: form.userId,
                project: form.project,
                env: form.env,
                risk: form.risk,
                decision: form.decision,
                sqlHash: form.sqlHash,
                tool: form.tool,
                page: 0,
                size: Number(form.size),
                sort: filters.sort ?? AUDIT_DEFAULT_SORT,
                direction: filters.direction ?? AUDIT_DEFAULT_DIRECTION,
            })
        )
        setOpen(false)
    }

    function handleReset() {
        const nextForm = formState({})
        setForm(nextForm)
        onSearchChange(
            normalizeAuditFilters({
                page: 0,
                size: Number(nextForm.size),
                sort: filters.sort ?? AUDIT_DEFAULT_SORT,
                direction: filters.direction ?? AUDIT_DEFAULT_DIRECTION,
            })
        )
        setOpen(false)
    }

    return (
        <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
                <Button type='button'>
                    <Filter className='size-4'/>
                    高级筛选
                </Button>
            </SheetTrigger>
            <SheetContent className='overflow-y-auto sm:max-w-lg'>
                <form className='grid h-full gap-6' onSubmit={handleSubmit}>
                    <SheetHeader>
                        <SheetTitle>高级筛选</SheetTitle>
                        <SheetDescription>
                            按时间、用户、项目、环境、risk、decision、SQL hash 和 tool
                            查询审计事件。
                        </SheetDescription>
                    </SheetHeader>

                    <div className='grid gap-4'>
                        <TextField
                            id='audit-filter-from'
                            label='起始时间'
                            value={form.from}
                            placeholder='2026-05-01T00:00:00Z'
                            onChange={(from) => setForm((current) => ({...current, from}))}
                        />
                        <TextField
                            id='audit-filter-to'
                            label='结束时间'
                            value={form.to}
                            placeholder='2026-05-02T00:00:00Z'
                            onChange={(to) => setForm((current) => ({...current, to}))}
                        />
                        <TextField
                            id='audit-filter-user-id'
                            label='用户 ID'
                            value={form.userId}
                            placeholder='1001'
                            onChange={(userId) =>
                                setForm((current) => ({...current, userId}))
                            }
                        />
                        <TextField
                            id='audit-filter-project'
                            label='项目'
                            value={form.project}
                            placeholder='billing-core'
                            onChange={(project) =>
                                setForm((current) => ({...current, project}))
                            }
                        />
                        <TextField
                            id='audit-filter-env'
                            label='环境'
                            value={form.env}
                            placeholder='prod'
                            onChange={(env) => setForm((current) => ({...current, env}))}
                        />
                        <SelectField
                            id='audit-filter-risk'
                            label='Risk'
                            value={form.risk}
                            options={['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']}
                            onChange={(risk) => setForm((current) => ({...current, risk}))}
                        />
                        <SelectField
                            id='audit-filter-decision'
                            label='Decision'
                            value={form.decision}
                            options={[
                                'EXECUTED',
                                'POLICY_DENIED',
                                'REQUIRES_CONFIRMATION',
                                'FAILED',
                            ]}
                            onChange={(decision) =>
                                setForm((current) => ({...current, decision}))
                            }
                        />
                        <TextField
                            id='audit-filter-sql-hash'
                            label='SQL Hash'
                            value={form.sqlHash}
                            placeholder='sha256:'
                            onChange={(sqlHash) =>
                                setForm((current) => ({...current, sqlHash}))
                            }
                        />
                        <SelectField
                            id='audit-filter-tool'
                            label='Tool'
                            value={form.tool}
                            options={[
                                'dbflow_execute_sql',
                                'dbflow_explain_sql',
                                'dbflow_inspect_schema',
                            ]}
                            onChange={(tool) => setForm((current) => ({...current, tool}))}
                        />
                        <SelectField
                            id='audit-filter-size'
                            label='每页'
                            value={form.size}
                            options={['10', '20', '50']}
                            onChange={(size) => setForm((current) => ({...current, size}))}
                        />
                    </div>

                    <SheetFooter className='mt-auto'>
                        <Button type='button' variant='outline' onClick={handleReset}>
                            重置
                        </Button>
                        <Button type='submit'>应用筛选</Button>
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    )
}

function TextField({
                       id,
                       label,
                       value,
                       placeholder,
                       onChange,
                   }: {
    id: string
    label: string
    value: string
    placeholder: string
    onChange: (value: string) => void
}) {
    return (
        <div className='grid gap-2'>
            <Label htmlFor={id}>{label}</Label>
            <Input
                id={id}
                value={value}
                placeholder={placeholder}
                onChange={(event) => onChange(event.target.value)}
            />
        </div>
    )
}

function SelectField({
                         id,
                         label,
                         value,
                         options,
                         onChange,
                     }: {
    id: string
    label: string
    value: string
    options: string[]
    onChange: (value: string) => void
}) {
    return (
        <div className='grid gap-2'>
            <Label htmlFor={id}>{label}</Label>
            <select
                id={id}
                value={value}
                className={selectClassName}
                onChange={(event) => onChange(event.target.value)}
            >
                <option value=''>全部</option>
                {options.map((option) => (
                    <option key={option} value={option}>
                        {option}
                    </option>
                ))}
            </select>
        </div>
    )
}

function formState(filters: AuditEventFilters): FilterFormState {
    return {
        from: filters.from ?? '',
        to: filters.to ?? '',
        userId: filters.userId ?? '',
        project: filters.project ?? '',
        env: filters.env ?? '',
        risk: filters.risk ?? '',
        decision: filters.decision ?? '',
        sqlHash: filters.sqlHash ?? '',
        tool: filters.tool ?? '',
        size: String(filters.size ?? 20),
    }
}

const selectClassName = cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors outline-none',
    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
)
