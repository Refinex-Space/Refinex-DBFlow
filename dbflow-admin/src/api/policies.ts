import type {DangerousPolicyPage} from '@/types/policy'
import {apiGet} from '@/api/client'

export const dangerousPoliciesQueryKey = ['policies', 'dangerous']

export function fetchDangerousPolicies(): Promise<DangerousPolicyPage> {
    return apiGet<DangerousPolicyPage>('/policies/dangerous')
}
