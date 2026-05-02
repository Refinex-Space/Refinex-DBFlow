import {useMutation, useQueryClient} from '@tanstack/react-query'
import {Trash2} from 'lucide-react'
import {toast} from 'sonner'
import {grantOptionsQueryKey, grantsQueryKey, revokeGrant} from '@/api/grants'
import type {GrantEnvironmentOption, GrantGroupRow} from '@/types/access'
import {formatText} from '@/lib/format'
import {isApiClientError} from '@/lib/errors'
import {EnvBadge} from '@/components/dbflow/env-badge'
import {StatusBadge} from '@/components/dbflow/status-badge'
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import {Button} from '@/components/ui/button'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {EditProjectGrantsSheet} from './edit-project-grants-sheet'

type GrantsTableProps = {
    rows: GrantGroupRow[]
    environmentOptions: GrantEnvironmentOption[]
}

export function GrantsTable({rows, environmentOptions}: GrantsTableProps) {
    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>用户</TableHead>
                    <TableHead>项目</TableHead>
                    <TableHead>已授权环境</TableHead>
                    <TableHead className='w-44 text-end'>操作</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map((row) => (
                    <TableRow key={`${row.userId}:${row.projectKey}`}>
                        <TableCell className='font-medium'>
                            {formatText(row.username)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.projectKey)}
                        </TableCell>
                        <TableCell>
                            <div className='flex flex-wrap gap-2'>
                                {row.environments.map((environment) => (
                                    <div
                                        key={environment.grantId}
                                        className='flex items-center gap-1 rounded-md border bg-background px-2 py-1'
                                    >
                                        <EnvBadge environment={environment.environmentKey}/>
                                        <span className='text-xs text-muted-foreground'>
                                            {environment.grantType}
                                        </span>
                                        <StatusBadge status={environment.status}/>
                                        <RevokeGrantButton row={row} grantId={environment.grantId}
                                                           environmentKey={environment.environmentKey}/>
                                    </div>
                                ))}
                            </div>
                        </TableCell>
                        <TableCell>
                            <div className='flex justify-end'>
                                <EditProjectGrantsSheet
                                    row={row}
                                    environments={environmentOptions.filter(
                                        (option) => option.projectKey === row.projectKey
                                    )}
                                />
                            </div>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}

type RevokeGrantButtonProps = {
    row: GrantGroupRow
    grantId: number
    environmentKey: string
}

function RevokeGrantButton({
                               row,
                               grantId,
                               environmentKey,
                           }: RevokeGrantButtonProps) {
    const queryClient = useQueryClient()
    const mutation = useMutation({
        mutationFn: () => revokeGrant(grantId),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({queryKey: grantsQueryKey()}),
                queryClient.invalidateQueries({queryKey: grantOptionsQueryKey}),
            ])
            toast.success('授权已撤销')
        },
        onError: (mutationError) => {
            toast.error(errorMessage(mutationError))
        },
    })

    return (
        <AlertDialog>
            <AlertDialogTrigger asChild>
                <Button
                    type='button'
                    size='icon'
                    variant='ghost'
                    className='size-7 text-muted-foreground'
                    aria-label={`撤销 ${row.username} ${environmentKey}`}
                >
                    <Trash2 className='size-4'/>
                </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>撤销环境授权</AlertDialogTitle>
                    <AlertDialogDescription>
                        将撤销 {row.username} 对 {row.projectKey}/{environmentKey} 的访问授权。
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel disabled={mutation.isPending}>取消</AlertDialogCancel>
                    <AlertDialogAction
                        disabled={mutation.isPending}
                        onClick={() => mutation.mutate()}
                    >
                        {mutation.isPending ? '撤销中...' : '确认撤销'}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    )
}

function errorMessage(error: unknown): string {
    if (isApiClientError(error)) {
        return error.message
    }

    return error instanceof Error ? error.message : '撤销授权失败'
}
