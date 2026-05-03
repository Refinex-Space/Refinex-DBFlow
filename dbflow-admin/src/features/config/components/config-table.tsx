import type {KeyboardEvent} from 'react'
import type {ConfigRow} from '@/types/config'
import {formatText} from '@/lib/format'
import {StatusBadge} from '@/components/dbflow/status-badge'
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from '@/components/ui/table'

type ConfigTableProps = {
    rows: ConfigRow[]
    onRowSelect: (row: ConfigRow) => void
}

export function ConfigTable({rows, onRowSelect}: ConfigTableProps) {
    function handleKeyDown(event: KeyboardEvent<HTMLTableRowElement>, row: ConfigRow) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            onRowSelect(row)
        }
    }

    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>project</TableHead>
                    <TableHead>projectName</TableHead>
                    <TableHead>env</TableHead>
                    <TableHead>envName</TableHead>
                    <TableHead>datasource</TableHead>
                    <TableHead>type</TableHead>
                    <TableHead>host</TableHead>
                    <TableHead>port</TableHead>
                    <TableHead>schema</TableHead>
                    <TableHead>username</TableHead>
                    <TableHead>limits</TableHead>
                    <TableHead>syncStatus</TableHead>
                </TableRow>
            </TableHeader>
            <TableBody>
                {rows.map((row) => (
                    <TableRow
                        key={`${row.project}:${row.env}`}
                        role='button'
                        tabIndex={0}
                        aria-label={`查看 ${row.project}/${row.env} 配置详情`}
                        className='cursor-pointer'
                        onClick={() => onRowSelect(row)}
                        onKeyDown={(event) => handleKeyDown(event, row)}
                    >
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.project)}
                        </TableCell>
                        <TableCell>{formatText(row.projectName)}</TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.env)}
                        </TableCell>
                        <TableCell>{formatText(row.envName)}</TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.datasource)}
                        </TableCell>
                        <TableCell>{formatText(row.type)}</TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.host)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.port)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.schema)}
                        </TableCell>
                        <TableCell className='font-mono text-xs'>
                            {formatText(row.username)}
                        </TableCell>
                        <TableCell className='text-xs text-muted-foreground'>
                            {formatText(row.limits)}
                        </TableCell>
                        <TableCell>
                            <StatusBadge status={row.syncStatus}/>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    )
}
