import type {AdminSession} from '@/types/session'
import {apiGet} from '@/api/client'

/**
 * 读取当前管理端服务端 Session。
 */
export function getCurrentSession(): Promise<AdminSession> {
    return apiGet<AdminSession>('/session')
}
