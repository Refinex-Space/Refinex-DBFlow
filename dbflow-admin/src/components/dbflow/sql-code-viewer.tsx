import {Database} from 'lucide-react'
import {formatText} from '@/lib/format'
import {cn} from '@/lib/utils'
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'

type SqlCodeViewerProps = {
    sql: string | null | undefined
    title?: string
    description?: string
    className?: string
}

export function SqlCodeViewer({
                                  sql,
                                  title = 'SQL 文本',
                                  description = '后端返回的脱敏 SQL 原文，当前使用轻量只读 code viewer 展示。',
                                  className,
                              }: SqlCodeViewerProps) {
    return (
        <Card className={cn('rounded-md shadow-none', className)}>
            <CardHeader>
                <CardTitle className='flex items-center gap-2 text-base'>
                    <Database className='size-4 text-muted-foreground'/>
                    {title}
                </CardTitle>
                <CardDescription>{description}</CardDescription>
            </CardHeader>
            <CardContent>
        <pre
            className='max-h-[460px] overflow-auto rounded-md border bg-muted/40 p-4 font-mono text-xs leading-6 whitespace-pre-wrap text-foreground'>
          <code>{formatText(sql, '没有可展示的 SQL 文本')}</code>
        </pre>
            </CardContent>
        </Card>
    )
}
