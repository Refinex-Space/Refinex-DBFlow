import type {PolicyReason} from '@/types/policy'
import {formatText} from '@/lib/format'
import {Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle,} from '@/components/ui/sheet'
import {DecisionBadge} from '@/components/dbflow/decision-badge'
import {RiskBadge} from '@/components/dbflow/risk-badge'

type PolicyReasonSheetProps = {
    reason: PolicyReason | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function PolicyReasonSheet({
                                      reason,
                                      open,
                                      onOpenChange,
                                  }: PolicyReasonSheetProps) {
    if (!reason) {
        return null
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className='sm:max-w-lg'>
                <SheetHeader>
                    <SheetTitle>{reason.title}</SheetTitle>
                    <SheetDescription>
                        危险策略只读说明，来自当前后端生效配置。
                    </SheetDescription>
                </SheetHeader>

                <dl className='grid gap-3 overflow-y-auto px-4 pb-4 text-sm'>
                    <div className='grid gap-1 rounded-md border bg-muted/30 p-3'>
                        <dt className='text-xs text-muted-foreground'>risk</dt>
                        <dd>
                            <RiskBadge risk={reason.risk}/>
                        </dd>
                    </div>
                    {reason.kind === 'default' ? (
                        <>
                            <div className='grid gap-1 rounded-md border bg-muted/30 p-3'>
                                <dt className='text-xs text-muted-foreground'>decision</dt>
                                <dd>
                                    <DecisionBadge decision={reason.decision}/>
                                </dd>
                            </div>
                            <DetailItem label='requirement' value={reason.requirement}/>
                        </>
                    ) : (
                        <>
                            <DetailItem label='scope' value={reason.scope}/>
                            <DetailItem label='allowProd' value={reason.allowProd}/>
                            <DetailItem label='prodRule' value={reason.prodRule}/>
                        </>
                    )}
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
            <dd className='text-sm break-words'>{formatText(value)}</dd>
        </div>
    )
}
