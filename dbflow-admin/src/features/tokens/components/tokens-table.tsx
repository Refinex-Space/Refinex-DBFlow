import type {IssuedTokenResponse, TokenRow} from '@/types/token'
import {formatDateTime, formatText} from '@/lib/format'
import {StatusBadge} from '@/components/dbflow/status-badge'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {TokenActions} from './token-actions'

type TokensTableProps = {
    tokens: TokenRow[]
    onTokenIssued: (token: IssuedTokenResponse) => void
}

export function TokensTable({tokens, onTokenIssued}: TokensTableProps) {
    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead className='w-20'>ID</TableHead>
                    <TableHead>用户</TableHead>
                    <TableHead>Prefix</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>过期时间</TableHead>
                    <TableHead>最后使用</TableHead>
                    <TableHead className='w-48 text-end'>操作</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {tokens.map((token) => (
                    <TableRow key={token.id}>
                        <TableCell className='font-mono text-xs text-muted-foreground'>
                            {token.id}
                        </TableCell>
                        <TableCell className='font-medium'>
                            {formatText(token.username)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(token.tokenPrefix)}
                        </TableCell>
                        <TableCell>
                            <StatusBadge status={token.status}/>
                        </TableCell>
                        <TableCell>{formatDateTime(token.expiresAt)}</TableCell>
                        <TableCell>{formatDateTime(token.lastUsedAt)}</TableCell>
                        <TableCell>
                            <TokenActions token={token} onTokenIssued={onTokenIssued}/>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}
