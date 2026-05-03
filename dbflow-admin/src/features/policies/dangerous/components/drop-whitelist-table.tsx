import type {PolicyWhitelistRow} from '@/types/policy'
import {formatText} from '@/lib/format'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {RiskBadge} from '@/components/dbflow/risk-badge'

type DropWhitelistTableProps = {
    rows: PolicyWhitelistRow[]
    onReasonSelect: (row: PolicyWhitelistRow) => void
}

export function DropWhitelistTable({
                                       rows,
                                       onReasonSelect,
                                   }: DropWhitelistTableProps) {
    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>operation</TableHead>
                    <TableHead>risk</TableHead>
                    <TableHead>project</TableHead>
                    <TableHead>env</TableHead>
                    <TableHead>schema</TableHead>
                    <TableHead>table</TableHead>
                    <TableHead>allowProd</TableHead>
                    <TableHead>prodRule</TableHead>
                    <TableHead className='text-end'>说明</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map((row) => (
                    <TableRow
                        key={[
                            row.operation,
                            row.project,
                            row.env,
                            row.schema,
                            row.table,
                            row.allowProd,
                        ].join(':')}
                    >
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.operation)}
                        </TableCell>
                        <TableCell>
                            <RiskBadge risk={row.risk}/>
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.project)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.env)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.schema)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.table)}
                        </TableCell>
                        <TableCell>
                            <AllowProdBadge value={row.allowProd}/>
                        </TableCell>
                        <TableCell className='min-w-56 text-muted-foreground'>
                            {formatText(row.prodRule)}
                        </TableCell>
                        <TableCell className='text-end'>
                            <Button
                                type='button'
                                variant='ghost'
                                size='sm'
                                onClick={() => onReasonSelect(row)}
                            >
                                <span className='sr-only'>查看 {row.operation} 白名单说明</span>
                                查看
                            </Button>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}

function AllowProdBadge({value}: { value: string }) {
    const normalized = value.trim().toUpperCase()
    const className =
        normalized === 'YES'
            ? 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300'
            : 'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300'

    return (
        <Badge variant='outline' className={className}>
            {formatText(value)}
        </Badge>
    )
}
