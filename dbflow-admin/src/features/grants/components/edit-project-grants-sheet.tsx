import {type FormEvent, useState} from 'react'
import {useMutation, useQueryClient} from '@tanstack/react-query'
import {Pencil, TriangleAlert} from 'lucide-react'
import {toast} from 'sonner'
import {grantOptionsQueryKey, grantsQueryKey, updateProjectGrants} from '@/api/grants'
import type {GrantEnvironmentOption, GrantGroupRow, GrantType,} from '@/types/access'
import {isApiClientError} from '@/lib/errors'
import {cn} from '@/lib/utils'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Checkbox} from '@/components/ui/checkbox'
import {Label} from '@/components/ui/label'
import {
    Sheet,
    SheetContent,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    SheetTrigger,
} from '@/components/ui/sheet'

type EditProjectGrantsSheetProps = {
    row: GrantGroupRow
    environments: GrantEnvironmentOption[]
}

const grantTypes: GrantType[] = ['READ', 'WRITE', 'ADMIN']

export function EditProjectGrantsSheet({
                                           row,
                                           environments,
                                       }: EditProjectGrantsSheetProps) {
    const [open, setOpen] = useState(false)

    return (
        <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
                <Button
                    type='button'
                    size='sm'
                    variant='outline'
                    aria-label={`编辑 ${row.username} / ${row.projectKey}`}
                >
                    <Pencil className='size-4'/>
                    编辑
                </Button>
            </SheetTrigger>
            <SheetContent aria-describedby={undefined} className='sm:max-w-md'>
                <EditProjectGrantsForm
                    key={editFormKey(row)}
                    row={row}
                    environments={environments}
                    closeSheet={() => setOpen(false)}
                />
            </SheetContent>
        </Sheet>
    )
}

function EditProjectGrantsForm({
                                   row,
                                   environments,
                                   closeSheet,
                               }: EditProjectGrantsSheetProps & {
    closeSheet: () => void
}) {
    const queryClient = useQueryClient()
    const [grantType, setGrantType] = useState<GrantType | string>(
        row.environments[0]?.grantType ?? 'WRITE'
    )
    const [environmentKeys, setEnvironmentKeys] = useState<string[]>(
        row.environments.map((environment) => environment.environmentKey)
    )
    const [error, setError] = useState<string | null>(null)

    const mutation = useMutation({
        mutationFn: updateProjectGrants,
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({queryKey: grantsQueryKey()}),
                queryClient.invalidateQueries({queryKey: grantOptionsQueryKey}),
            ])
            toast.success('授权已保存')
            closeSheet()
        },
        onError: (mutationError) => {
            const message = errorMessage(mutationError)
            setError(message)
            toast.error(message)
        },
    })

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)
        mutation.mutate({
            userId: row.userId,
            projectKey: row.projectKey,
            environmentKeys,
            grantType,
        })
    }

    return (
        <form className='flex h-full flex-col' onSubmit={handleSubmit}>
                    <SheetHeader>
                        <SheetTitle>编辑授权</SheetTitle>
                    </SheetHeader>

                    <div className='grid gap-4 overflow-y-auto px-4'>
                        {error && (
                            <Alert variant='destructive'>
                                <TriangleAlert/>
                                <AlertTitle>保存失败</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}

                        {environments.length === 0 && (
                            <Alert>
                                <TriangleAlert/>
                                <AlertTitle>没有可授权环境</AlertTitle>
                                <AlertDescription>
                                    当前项目没有可选择的环境配置。
                                </AlertDescription>
                            </Alert>
                        )}

                        <div className='grid gap-2'>
                            <Label htmlFor={`edit-grant-type-${row.userId}-${row.projectKey}`}>
                                授权类型
                            </Label>
                            <select
                                id={`edit-grant-type-${row.userId}-${row.projectKey}`}
                                value={grantType}
                                className={selectClassName}
                                onChange={(event) => setGrantType(event.target.value)}
                            >
                                {grantTypes.map((type) => (
                                    <option key={type} value={type}>
                                        {type}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className='grid gap-2'>
                            <Label>项目环境</Label>
                            <p className='text-xs text-muted-foreground'>
                                取消所有勾选并保存，会撤销该用户在此项目下的全部环境授权。
                            </p>
                            <div className='grid gap-2 rounded-md border p-3'>
                                {environments.map((environment) => {
                                    const checked = environmentKeys.includes(
                                        environment.environmentKey
                                    )
                                    return (
                                        <label
                                            key={environment.environmentKey}
                                            className='flex items-center gap-2 text-sm'
                                        >
                                            <Checkbox
                                                checked={checked}
                                                onCheckedChange={(nextChecked) => {
                                                    setEnvironmentKeys((current) =>
                                                        nextChecked
                                                            ? [...current, environment.environmentKey]
                                                            : current.filter(
                                                                (key) =>
                                                                    key !== environment.environmentKey
                                                            )
                                                    )
                                                }}
                                            />
                                            <span>
                                                {environment.environmentKey} /{' '}
                                                {environment.environmentName}
                                            </span>
                                        </label>
                                    )
                                })}
                            </div>
                        </div>
                    </div>

                    <SheetFooter>
                        <Button type='submit' disabled={mutation.isPending}>
                            {mutation.isPending ? '保存中...' : '保存变更'}
                        </Button>
                    </SheetFooter>
        </form>
    )
}

function editFormKey(row: GrantGroupRow): string {
    return [
        row.userId,
        row.projectKey,
        ...row.environments.map(
            (environment) =>
                `${environment.environmentKey}:${environment.grantType}`
        ),
    ].join('|')
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '保存授权失败'
}

const selectClassName = cn(
    'flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs outline-none transition-colors',
    'focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50'
)
