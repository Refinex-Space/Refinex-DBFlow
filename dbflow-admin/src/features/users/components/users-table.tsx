import type {AdminUserRow} from '@/types/access'
import {formatNumber, formatText} from '@/lib/format'
import {StatusBadge} from '@/components/dbflow/status-badge'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {UserActions} from './user-actions'

type UsersTableProps = {
    users: AdminUserRow[]
}

export function UsersTable({users}: UsersTableProps) {
    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead className='w-20'>ID</TableHead>
                    <TableHead>用户名</TableHead>
                    <TableHead>显示名</TableHead>
                    <TableHead>角色</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead className='text-end'>授权数</TableHead>
                    <TableHead className='text-end'>活跃 Token</TableHead>
                    <TableHead className='w-48 text-end'>操作</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {users.map((user) => (
                    <TableRow key={user.id}>
                        <TableCell className='font-mono text-xs text-muted-foreground'>
                            {user.id}
                        </TableCell>
                        <TableCell className='font-medium'>
                            {formatText(user.username)}
                        </TableCell>
                        <TableCell>{formatText(user.displayName)}</TableCell>
                        <TableCell>{formatText(user.role)}</TableCell>
                        <TableCell>
                            <StatusBadge status={user.status}/>
                        </TableCell>
                        <TableCell className='text-end'>
                            {formatNumber(user.grantCount)}
                        </TableCell>
                        <TableCell className='text-end'>
                            {formatNumber(user.activeTokenCount)}
                        </TableCell>
                        <TableCell>
                            <UserActions user={user}/>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}
