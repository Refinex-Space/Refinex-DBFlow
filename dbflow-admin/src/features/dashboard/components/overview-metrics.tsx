import type {OverviewMetric} from '@/types/overview'
import {cn} from '@/lib/utils'
import {MetricCard} from '@/components/dbflow/metric-card'

type OverviewMetricsProps = {
    metrics: OverviewMetric[]
}

export function OverviewMetrics({metrics}: OverviewMetricsProps) {
    return (
        <section
            aria-label='总览指标'
            className='grid gap-3 sm:grid-cols-2 xl:grid-cols-3'
        >
            {metrics.map((metric) => (
                <MetricCard
                    key={metric.label}
                    title={metric.label}
                    value={
                        <span className={cn(metricValueClass(metric.tone))}>
              {metric.value}
            </span>
                    }
                    description={metric.hint}
                />
            ))}
        </section>
    )
}

function metricValueClass(tone: string) {
    if (tone === 'bad') {
        return 'text-red-700 dark:text-red-300'
    }

    if (tone === 'warn') {
        return 'text-amber-700 dark:text-amber-300'
    }

    if (tone === 'ok') {
        return 'text-emerald-700 dark:text-emerald-300'
    }

    return undefined
}
