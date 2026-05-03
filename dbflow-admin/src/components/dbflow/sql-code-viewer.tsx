import Editor from '@monaco-editor/react'
import {Database} from 'lucide-react'
import {formatText} from '@/lib/format'
import '@/lib/monaco'
import {cn} from '@/lib/utils'
import {useTheme} from '@/context/theme-provider'
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from '@/components/ui/card'

type SqlCodeViewerProps = {
    sql: string | null | undefined
    title?: string
    description?: string
    height?: string
    className?: string
}

export function SqlCodeViewer({
                                  sql,
                                  title = 'SQL 文本',
                                  description = '后端返回的脱敏 SQL 原文，当前使用 Monaco readonly viewer 展示。',
                                  height = '220px',
                                  className,
                              }: SqlCodeViewerProps) {
    const {resolvedTheme} = useTheme()
    const value = formatText(sql, '没有可展示的 SQL 文本')

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
                <div className='overflow-hidden rounded-md border bg-muted/40'>
                    <Editor
                        height={height}
                        width='100%'
                        language='sql'
                        value={value}
                        theme={resolvedTheme === 'dark' ? 'vs-dark' : 'light'}
                        loading={
                            <div className='flex h-[220px] items-center px-4 font-mono text-xs text-muted-foreground'>
                                正在加载 SQL Viewer...
                            </div>
                        }
                        options={{
                            ariaLabel: 'Monaco SQL Viewer',
                            readOnly: true,
                            domReadOnly: true,
                            contextmenu: false,
                            minimap: {enabled: false},
                            automaticLayout: true,
                            scrollBeyondLastLine: false,
                            wordWrap: 'on',
                            lineNumbersMinChars: 3,
                            folding: false,
                            renderLineHighlight: 'none',
                            overviewRulerLanes: 0,
                            scrollbar: {
                                alwaysConsumeMouseWheel: false,
                            },
                        }}
                    />
                </div>
            </CardContent>
        </Card>
    )
}
