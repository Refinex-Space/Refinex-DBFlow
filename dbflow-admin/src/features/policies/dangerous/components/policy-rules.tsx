import type {PolicyRuleRow} from '@/types/policy'
import {ShieldAlert} from 'lucide-react'
import {formatText} from '@/lib/format'
import {Badge} from '@/components/ui/badge'
import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card'

type PolicyRulesProps = {
    rules: PolicyRuleRow[]
}

export function PolicyRules({rules}: PolicyRulesProps) {
    return (
        <div className='grid gap-3 md:grid-cols-3'>
            {rules.map((rule) => (
                <Card key={rule.name} className='rounded-md shadow-none'>
                    <CardHeader className='gap-3'>
                        <div
                            className='flex size-9 items-center justify-center rounded-md border bg-muted/40 text-muted-foreground'>
                            <ShieldAlert className='size-4'/>
                        </div>
                        <div className='space-y-1'>
                            <CardTitle className='text-sm'>{formatText(rule.name)}</CardTitle>
                            <div className='flex flex-wrap items-center gap-2'>
                                <RuleStatusBadge tone={rule.tone} status={rule.status}/>
                                <span className='text-xs text-muted-foreground'>
                  {formatText(rule.description)}
                </span>
                            </div>
                        </div>
                    </CardHeader>
                    <CardContent className='text-sm text-muted-foreground'>
                        {formatText(rule.detail)}
                    </CardContent>
                </Card>
            ))}
        </div>
    )
}

function RuleStatusBadge({tone, status}: { tone: string; status: string }) {
    const normalizedTone = tone.trim().toLowerCase()
    const className =
        normalizedTone === 'bad'
            ? 'border-red-500/30 bg-red-500/10 text-red-700 dark:text-red-300'
            : normalizedTone === 'warn'
                ? 'border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300'
                : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'

    return (
        <Badge variant='outline' className={className}>
            {formatText(status)}
        </Badge>
    )
}
