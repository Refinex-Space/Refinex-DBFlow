import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {fetchAuditEvents} from './audit'
import {apiClient} from './client'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('audit API client', () => {
    it('loads audit events with normalized server-side filters', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/audit-events')
            expect(config.params).toEqual({
                from: '2026-05-01T00:00:00Z',
                to: '2026-05-02T00:00:00Z',
                userId: '1001',
                project: 'billing-core',
                env: 'prod',
                risk: 'HIGH',
                decision: 'POLICY_DENIED',
                sqlHash: 'sha256:abc',
                tool: 'dbflow_execute_sql',
                page: 2,
                size: 50,
                sort: 'createdAt',
                direction: 'desc',
            })
            return apiResponse({
                content: [auditRow],
                page: 2,
                size: 50,
                totalElements: 101,
                totalPages: 3,
                sort: 'createdAt',
                direction: 'desc',
            })
        })

        const page = await fetchAuditEvents({
            from: '2026-05-01T00:00:00Z',
            to: '2026-05-02T00:00:00Z',
            userId: '1001',
            project: 'billing-core',
            env: 'prod',
            risk: 'HIGH',
            decision: 'POLICY_DENIED',
            sqlHash: 'sha256:abc',
            tool: 'dbflow_execute_sql',
            page: 2,
            size: 50,
            sort: 'createdAt',
            direction: 'desc',
        })

        expect(page.content).toHaveLength(1)
        expect(JSON.stringify(page)).not.toContain('plain-db-password')
        expect(JSON.stringify(page)).not.toContain('jdbc:mysql://')
        expect(JSON.stringify(page)).not.toContain('plaintextToken')
        expect(JSON.stringify(page)).not.toContain('tokenHash')
    })
})

const auditRow = {
    id: 42,
    requestId: 'req-1',
    userId: 1001,
    projectKey: 'billing-core',
    environmentKey: 'prod',
    clientName: 'Codex',
    clientVersion: '1.0.0',
    tool: 'dbflow_execute_sql',
    operationType: 'DROP_TABLE',
    riskLevel: 'HIGH',
    status: 'DENIED',
    decision: 'POLICY_DENIED',
    sqlHash: 'sha256:abc',
    resultSummary: 'DROP 高危 DDL 未命中 YAML 白名单',
    affectedRows: 0,
    createdAt: '2026-05-01T12:00:00Z',
}

function useAdapter(
    handler: Parameters<AxiosAdapter>[0] extends infer Config
        ? (config: Config) => AxiosResponse | Promise<AxiosResponse>
        : never
) {
    apiClient.defaults.adapter = async (config) => handler(config)
}

function apiResponse<T>(data: T): AxiosResponse<ApiResult<T>> {
    return {
        data: {
            success: true,
            code: 'OK',
            message: 'OK',
            data,
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config: {} as AxiosResponse<ApiResult<T>>['config'],
    }
}
