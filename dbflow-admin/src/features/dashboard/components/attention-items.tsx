import type { AttentionItem } from '@/types/overview'
import { ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { DecisionBadge } from '@/components/dbflow/decision-badge'
import { EmptyState } from '@/components/dbflow/empty-state'

type AttentionItemsProps = {
  items: AttentionItem[]
}

export function AttentionItems({ items }: AttentionItemsProps) {
  return (
    <section className='space-y-3'>
      <div className='flex items-center justify-between gap-3'>
        <h2 className='text-base font-semibold'>需要关注事项</h2>
        <Button variant='ghost' size='sm' asChild>
          <a href='/admin/policies/dangerous'>危险策略</a>
        </Button>
      </div>

      {items.length === 0 ? (
        <EmptyState
          title='当前无需要关注的运行时事项。'
          className='py-8'
        />
      ) : (
        <Card className='gap-0 rounded-md py-0 shadow-none'>
          {items.map((item) => (
            <div
              key={`${item.status}-${item.label}`}
              className='flex items-center justify-between gap-3 border-b px-4 py-3 last:border-b-0'
            >
              <div className='min-w-0'>
                <p className='truncate text-sm font-medium'>{item.label}</p>
                <p className='text-xs text-muted-foreground'>{item.href}</p>
              </div>
              <div className='flex shrink-0 items-center gap-2'>
                <DecisionBadge decision={item.status} />
                <Button variant='ghost' size='icon' asChild>
                  <a href={item.href} aria-label={`${item.label} 详情`}>
                    <ExternalLink />
                  </a>
                </Button>
              </div>
            </div>
          ))}
        </Card>
      )}
    </section>
  )
}
