import type {ConfigPage} from '@/types/config'
import {apiGet} from '@/api/client'

export const configQueryKey = ['config']

export function fetchConfigPage(): Promise<ConfigPage> {
    return apiGet<ConfigPage>('/config')
}
