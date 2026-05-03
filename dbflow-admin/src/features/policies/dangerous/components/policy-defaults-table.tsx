import type {PolicyDefaultRow} from '@/types/policy'
import {formatText} from '@/lib/format'
import {Button} from '@/components/ui/button'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'
import {DecisionBadge} from '@/components/dbflow/decision-badge'
import {RiskBadge} from '@/components/dbflow/risk-badge'

type PolicyDefaultsTableProps = {
    rows: PolicyDefaultRow[]
    onReasonSelect: (row: PolicyDefaultRow) => void
}

export function PolicyDefaultsTable({
                                        rows,
                                        onReasonSelect,
                                    }: PolicyDefaultsTableProps) {
    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>operation</TableHead>
                    <TableHead>risk</TableHead>
                    <TableHead>decision</TableHead>
                    <TableHead>requirement</TableHead>
                    <TableHead className='text-end'>说明</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map((row) => (
                    <TableRow key={`${row.operation}:${row.decision}`}>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.operation)}
                        </TableCell>
                        <TableCell>
                            <RiskBadge risk={row.risk}/>
                        </TableCell>
                        <TableCell>
                            <DecisionBadge decision={row.decision}/>
                        </TableCell>
                        <TableCell className='min-w-72 text-muted-foreground'>
                            {formatText(row.requirement)}
                        </TableCell>
                        <TableCell className='text-end'>
                            <Button
                                type='button'
                                variant='ghost'
                                size='sm'
                                onClick={() => onReasonSelect(row)}
                            >
                <span className='sr-only'>
                  查看 {row.operation} 默认策略说明
                </span>
                                查看
                            </Button>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}
