import {type FormEvent, useState} from 'react'
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
    const defaultForm = formState(filters)
    const formKey = JSON.stringify(defaultForm)

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        const form = formValues(event.currentTarget)
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
                <form key={formKey} className='grid h-full gap-6' onSubmit={handleSubmit}>
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
                            name='from'
                            label='起始时间'
                            defaultValue={defaultForm.from}
                            placeholder='2026-05-01T00:00:00Z'
                        />
                        <TextField
                            id='audit-filter-to'
                            name='to'
                            label='结束时间'
                            defaultValue={defaultForm.to}
                            placeholder='2026-05-02T00:00:00Z'
                        />
                        <TextField
                            id='audit-filter-user-id'
                            name='userId'
                            label='用户 ID'
                            defaultValue={defaultForm.userId}
                            placeholder='1001'
                        />
                        <TextField
                            id='audit-filter-project'
                            name='project'
                            label='项目'
                            defaultValue={defaultForm.project}
                            placeholder='billing-core'
                        />
                        <TextField
                            id='audit-filter-env'
                            name='env'
                            label='环境'
                            defaultValue={defaultForm.env}
                            placeholder='prod'
                        />
                        <SelectField
                            id='audit-filter-risk'
                            name='risk'
                            label='Risk'
                            defaultValue={defaultForm.risk}
                            options={['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']}
                        />
                        <SelectField
                            id='audit-filter-decision'
                            name='decision'
                            label='Decision'
                            defaultValue={defaultForm.decision}
                            options={[
                                'EXECUTED',
                                'POLICY_DENIED',
                                'REQUIRES_CONFIRMATION',
                                'FAILED',
                            ]}
                        />
                        <TextField
                            id='audit-filter-sql-hash'
                            name='sqlHash'
                            label='SQL Hash'
                            defaultValue={defaultForm.sqlHash}
                            placeholder='sha256:'
                        />
                        <SelectField
                            id='audit-filter-tool'
                            name='tool'
                            label='Tool'
                            defaultValue={defaultForm.tool}
                            options={[
                                'dbflow_execute_sql',
                                'dbflow_explain_sql',
                                'dbflow_inspect_schema',
                            ]}
                        />
                        <SelectField
                            id='audit-filter-size'
                            name='size'
                            label='每页'
                            defaultValue={defaultForm.size}
                            options={['10', '20', '50']}
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
                       name,
                       label,
                       defaultValue,
                       placeholder,
                   }: {
    id: string
    name: keyof FilterFormState
    label: string
    defaultValue: string
    placeholder: string
}) {
    return (
        <div className='grid gap-2'>
            <Label htmlFor={id}>{label}</Label>
            <Input
                id={id}
                name={name}
                defaultValue={defaultValue}
                placeholder={placeholder}
            />
        </div>
    )
}

function SelectField({
                         id,
                         name,
                         label,
                         defaultValue,
                         options,
                     }: {
    id: string
    name: keyof FilterFormState
    label: string
    defaultValue: string
    options: string[]
}) {
    return (
        <div className='grid gap-2'>
            <Label htmlFor={id}>{label}</Label>
            <select
                id={id}
                name={name}
                defaultValue={defaultValue}
                className={selectClassName}
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

function formValues(form: HTMLFormElement): FilterFormState {
    const data = new FormData(form)

    return {
        from: formValue(data, 'from'),
        to: formValue(data, 'to'),
        userId: formValue(data, 'userId'),
        project: formValue(data, 'project'),
        env: formValue(data, 'env'),
        risk: formValue(data, 'risk'),
        decision: formValue(data, 'decision'),
        sqlHash: formValue(data, 'sqlHash'),
        tool: formValue(data, 'tool'),
        size: formValue(data, 'size'),
    }
}

function formValue(data: FormData, name: keyof FilterFormState): string {
    return String(data.get(name) ?? '')
}

const selectClassName = cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-colors outline-none',
    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
)
