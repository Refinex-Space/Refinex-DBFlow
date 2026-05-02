import {type FormEvent, useMemo, useState} from 'react'
import {useMutation, useQueryClient} from '@tanstack/react-query'
import {Plus, TriangleAlert} from 'lucide-react'
import {toast} from 'sonner'
import {grantOptionsQueryKey, grantsQueryKey, updateProjectGrants,} from '@/api/grants'
import type {GrantEnvironmentOption, GrantOptionsResponse, GrantType,} from '@/types/access'
import {isApiClientError} from '@/lib/errors'
import {cn} from '@/lib/utils'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Checkbox} from '@/components/ui/checkbox'
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

type CreateGrantSheetProps = {
    options?: GrantOptionsResponse
}

const grantTypes: GrantType[] = ['READ', 'WRITE', 'ADMIN']

export function CreateGrantSheet({options}: CreateGrantSheetProps) {
    const queryClient = useQueryClient()
    const [open, setOpen] = useState(false)
    const [userId, setUserId] = useState('')
    const [projectKey, setProjectKey] = useState('')
    const [grantType, setGrantType] = useState<GrantType>('WRITE')
    const [environmentKeys, setEnvironmentKeys] = useState<string[]>([])
    const [error, setError] = useState<string | null>(null)
    const groupedEnvironments = useMemo(
        () => groupEnvironmentsByProject(options?.environments ?? []),
        [options?.environments]
    )
    const projectOptions = Object.entries(groupedEnvironments)
    const selectedProjectEnvironments = projectKey
        ? groupedEnvironments[projectKey] ?? []
        : []
    const hasEnvironmentOptions = projectOptions.length > 0

    const mutation = useMutation({
        mutationFn: updateProjectGrants,
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({queryKey: grantsQueryKey()}),
                queryClient.invalidateQueries({queryKey: grantOptionsQueryKey}),
            ])
            toast.success('授权已保存')
            setOpen(false)
            resetForm()
        },
        onError: (mutationError) => {
            const message = errorMessage(mutationError)
            setError(message)
            toast.error(message)
        },
    })

    function resetForm() {
        setUserId('')
        setProjectKey('')
        setGrantType('WRITE')
        setEnvironmentKeys([])
        setError(null)
    }

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)

        if (!hasEnvironmentOptions) {
            setError('请先在 dbflow.projects 中配置项目环境。')
            return
        }

        if (!userId || !projectKey || environmentKeys.length === 0) {
            setError('请选择用户、项目和至少一个环境。')
            return
        }

        mutation.mutate({
            userId: Number(userId),
            projectKey,
            environmentKeys,
            grantType,
        })
    }

    return (
        <Sheet
            open={open}
            onOpenChange={(nextOpen) => {
                setOpen(nextOpen)
                if (!nextOpen) {
                    resetForm()
                }
            }}
        >
            <SheetTrigger asChild>
                <Button>
                    <Plus className='size-4'/>
                    新建授权
                </Button>
            </SheetTrigger>
            <SheetContent className='sm:max-w-md'>
                <form className='flex h-full flex-col' onSubmit={handleSubmit}>
                    <SheetHeader>
                        <SheetTitle>新建授权</SheetTitle>
                        <SheetDescription>
                            为用户批量授权指定项目下的一个或多个环境。
                        </SheetDescription>
                    </SheetHeader>

                    <div className='grid gap-4 overflow-y-auto px-4'>
                        {!hasEnvironmentOptions && (
                            <Alert>
                                <TriangleAlert/>
                                <AlertTitle>没有可授权环境</AlertTitle>
                                <AlertDescription>
                                    请先在 dbflow.projects 中配置项目环境。
                                </AlertDescription>
                            </Alert>
                        )}

                        {error && (
                            <Alert variant='destructive'>
                                <TriangleAlert/>
                                <AlertTitle>保存失败</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}

                        <div className='grid gap-2'>
                            <Label htmlFor='create-grant-user'>授权用户</Label>
                            <select
                                id='create-grant-user'
                                value={userId}
                                className={selectClassName}
                                disabled={!hasEnvironmentOptions}
                                onChange={(event) => setUserId(event.target.value)}
                            >
                                <option value=''>请选择用户</option>
                                {(options?.users ?? []).map((user) => (
                                    <option key={user.id} value={user.id}>
                                        {user.username} / {user.displayName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className='grid gap-2'>
                            <Label htmlFor='create-grant-project'>授权项目</Label>
                            <select
                                id='create-grant-project'
                                value={projectKey}
                                className={selectClassName}
                                disabled={!hasEnvironmentOptions}
                                onChange={(event) => {
                                    setProjectKey(event.target.value)
                                    setEnvironmentKeys([])
                                }}
                            >
                                <option value=''>请选择项目</option>
                                {projectOptions.map(([key, environments]) => (
                                    <option key={key} value={key}>
                                        {key} / {environments[0]?.projectName ?? key}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className='grid gap-2'>
                            <Label htmlFor='create-grant-type'>授权类型</Label>
                            <select
                                id='create-grant-type'
                                value={grantType}
                                className={selectClassName}
                                disabled={!hasEnvironmentOptions}
                                onChange={(event) =>
                                    setGrantType(event.target.value as GrantType)
                                }
                            >
                                {grantTypes.map((type) => (
                                    <option key={type} value={type}>
                                        {type}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {projectKey && (
                            <EnvironmentCheckboxes
                                environments={selectedProjectEnvironments}
                                selected={environmentKeys}
                                onChange={setEnvironmentKeys}
                            />
                        )}
                    </div>

                    <SheetFooter>
                        <Button
                            type='submit'
                            disabled={mutation.isPending || !hasEnvironmentOptions}
                        >
                            {mutation.isPending ? '保存中...' : '保存授权'}
                        </Button>
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    )
}

export function groupEnvironmentsByProject(
    environments: GrantEnvironmentOption[]
): Record<string, GrantEnvironmentOption[]> {
    return environments.reduce<Record<string, GrantEnvironmentOption[]>>(
        (groups, environment) => {
            groups[environment.projectKey] ??= []
            groups[environment.projectKey].push(environment)
            return groups
        },
        {}
    )
}

type EnvironmentCheckboxesProps = {
    environments: GrantEnvironmentOption[]
    selected: string[]
    onChange: (environmentKeys: string[]) => void
}

function EnvironmentCheckboxes({
                                   environments,
                                   selected,
                                   onChange,
                               }: EnvironmentCheckboxesProps) {
    return (
        <div className='grid gap-2'>
            <Label>环境</Label>
            <div className='grid gap-2 rounded-md border p-3'>
                {environments.map((environment) => {
                    const checked = selected.includes(environment.environmentKey)
                    return (
                        <label
                            key={environment.environmentKey}
                            className='flex items-center gap-2 text-sm'
                        >
                            <Checkbox
                                checked={checked}
                                onCheckedChange={(nextChecked) => {
                                    onChange(
                                        nextChecked
                                            ? [...selected, environment.environmentKey]
                                            : selected.filter(
                                                (key) => key !== environment.environmentKey
                                            )
                                    )
                                }}
                            />
                            <span>
                                {environment.environmentKey} / {environment.environmentName}
                            </span>
                        </label>
                    )
                })}
            </div>
        </div>
    )
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
