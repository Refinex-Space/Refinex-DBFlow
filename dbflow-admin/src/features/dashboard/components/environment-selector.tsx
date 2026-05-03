import type {OverviewEnvironmentOption} from '@/types/overview'
import {cn} from '@/lib/utils'

type EnvironmentSelectorProps = {
    options: OverviewEnvironmentOption[]
}

export function EnvironmentSelector({options}: EnvironmentSelectorProps) {
    const defaultValue = toSelectValue(options[0])

    return (
        <select
            aria-label='环境范围'
            className={cn(
                'h-8 w-[180px] rounded-md border border-input bg-background px-3 text-sm shadow-xs',
                'disabled:cursor-not-allowed disabled:opacity-60 dark:bg-input/30'
            )}
            disabled
            value={defaultValue}
            onChange={() => undefined}
        >
            {options.map((option) => (
                <option
                    key={`${option.value}-${option.label}`}
                    value={toSelectValue(option)}
                >
                    {option.label}
                </option>
            ))}
        </select>
    )
}

function toSelectValue(option: OverviewEnvironmentOption | undefined) {
    return option?.value || '__all__'
}
