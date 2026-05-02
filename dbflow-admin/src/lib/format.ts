const defaultLocale = 'en-US'
const fallbackText = '-'

export function formatText(
    value: string | number | null | undefined,
    fallback = fallbackText
): string {
    if (value === null || value === undefined) {
        return fallback
    }

    const text = String(value).trim()
    return text || fallback
}

export function formatNumber(
    value: number | null | undefined,
    fallback = fallbackText
): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return fallback
    }

    return new Intl.NumberFormat(defaultLocale).format(value)
}

export function formatPercent(
    value: number | null | undefined,
    fallback = fallbackText
): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return fallback
    }

    return new Intl.NumberFormat(defaultLocale, {
        maximumFractionDigits: 1,
        style: 'percent',
    }).format(value)
}

export function formatDateTime(
    value: string | number | Date | null | undefined,
    fallback = fallbackText
): string {
    const date = toValidDate(value)
    if (!date) {
        return fallback
    }

    return new Intl.DateTimeFormat(defaultLocale, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

export function formatDate(
    value: string | number | Date | null | undefined,
    fallback = fallbackText
): string {
    const date = toValidDate(value)
    if (!date) {
        return fallback
    }

    return new Intl.DateTimeFormat(defaultLocale, {
        dateStyle: 'medium',
    }).format(date)
}

export function formatDurationMs(
    value: number | null | undefined,
    fallback = fallbackText
): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
        return fallback
    }

    if (Math.abs(value) < 1000) {
        return `${formatNumber(value)} ms`
    }

    const seconds = value / 1000
    return `${new Intl.NumberFormat(defaultLocale, {
        maximumFractionDigits: 2,
    }).format(seconds)} s`
}

function toValidDate(value: string | number | Date | null | undefined) {
    if (value === null || value === undefined || value === '') {
        return null
    }

    const date = value instanceof Date ? value : new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
}
