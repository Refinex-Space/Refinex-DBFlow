import type {Overview} from '@/types/overview'
import {apiGet} from '@/api/client'

export function fetchOverview(): Promise<Overview> {
    return apiGet<Overview>('/overview')
}
