import type {ConfigRow} from '@/types/config'
import {formatText} from '@/lib/format'
import {StatusBadge} from '@/components/dbflow/status-badge'
import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle,} from '@/components/ui/sheet'

type ConfigDetailSheetProps = {
    row: ConfigRow | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function ConfigDetailSheet({
                                      row,
                                      open,
                                      onOpenChange,
                                  }: ConfigDetailSheetProps) {
    if (!row) {
        return null
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className='sm:max-w-lg'>
                <SheetHeader>
                    <SheetTitle>{row.project}/{row.env}</SheetTitle>
                    <SheetDescription>
                        仅展示后端返回的脱敏配置摘要。
                    </SheetDescription>
                </SheetHeader>

                <dl className='grid gap-3 overflow-y-auto px-4 pb-4 text-sm'>
                    <DetailItem label='project' value={row.project}/>
                    <DetailItem label='projectName' value={row.projectName}/>
                    <DetailItem label='env' value={row.env}/>
                    <DetailItem label='envName' value={row.envName}/>
                    <DetailItem label='datasource' value={row.datasource}/>
                    <DetailItem label='type' value={row.type}/>
                    <DetailItem label='host' value={row.host}/>
                    <DetailItem label='port' value={row.port}/>
                    <DetailItem label='schema' value={row.schema}/>
                    <DetailItem label='username' value={row.username}/>
                    <DetailItem label='limits' value={row.limits}/>
                    <div className='grid gap-1 rounded-md border bg-muted/30 p-3'>
                        <dt className='text-xs text-muted-foreground'>syncStatus</dt>
                        <dd>
                            <StatusBadge status={row.syncStatus}/>
                        </dd>
                    </div>
                </dl>
            </SheetContent>
        </Sheet>
    )
}

type DetailItemProps = {
    label: string
    value: string
}

function DetailItem({label, value}: DetailItemProps) {
    return (
        <div className='grid gap-1 rounded-md border bg-muted/30 p-3'>
            <dt className='text-xs text-muted-foreground'>{label}</dt>
            <dd className='font-mono text-xs break-all'>{formatText(value)}</dd>
        </div>
    )
}
