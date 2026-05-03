import type {HealthPage} from '@/types/health'
import {apiGet} from '@/api/client'

export const healthQueryKey = ['health']

export function fetchHealthPage(): Promise<HealthPage> {
    return apiGet<HealthPage>('/health')
}
