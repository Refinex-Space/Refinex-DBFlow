import {type AxiosAdapter, type AxiosResponse} from 'axios'
import type {ApiResult} from '@/types/api'
import {afterEach, describe, expect, it} from 'vitest'
import {apiClient} from './client'
import {createUser, disableUser, enableUser, fetchUsers, resetUserPassword,} from './users'

const originalAdapter = apiClient.defaults.adapter

afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
})

describe('users API client', () => {
    it('loads users with username and status filters without exposing passwordHash', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('get')
            expect(config.url).toBe('/users')
            expect(config.params).toEqual({
                username: 'alice',
                status: 'ACTIVE',
            })
            return apiResponse([
                {
                    id: 1,
                    username: 'alice',
                    displayName: 'Alice',
                    role: 'ROLE_ADMIN',
                    status: 'ACTIVE',
                    grantCount: 2,
                    activeTokenCount: 1,
                },
            ])
        })

        const users = await fetchUsers({username: 'alice', status: 'ACTIVE'})

        expect(users).toHaveLength(1)
        expect(users[0]).not.toHaveProperty('passwordHash')
    })

    it('creates users through the JSON admin API', async () => {
        useAdapter((config) => {
            expect(config.method).toBe('post')
            expect(config.url).toBe('/users')
            expect(JSON.parse(String(config.data))).toEqual({
                username: 'bob',
                displayName: 'Bob',
                password: 'optional-secret',
            })
            return apiResponse({
                id: 2,
                username: 'bob',
                displayName: 'Bob',
                role: 'ROLE_ADMIN',
                status: 'ACTIVE',
                grantCount: 0,
                activeTokenCount: 0,
            })
        })

        await expect(
            createUser({
                username: 'bob',
                displayName: 'Bob',
                password: 'optional-secret',
            })
        ).resolves.toMatchObject({username: 'bob'})
    })

    it('calls enable, disable, and reset password mutation endpoints', async () => {
        const calls: Array<{ method?: string; url?: string; data?: unknown }> = []
        useAdapter((config) => {
            calls.push({
                method: config.method,
                url: config.url,
                data: config.data ? JSON.parse(String(config.data)) : undefined,
            })
            return apiResponse({ok: true})
        })

        await disableUser(7)
        await enableUser(7)
        await resetUserPassword(7, {newPassword: 'NewSecret123!'})

        expect(calls).toEqual([
            {method: 'post', url: '/users/7/disable', data: undefined},
            {method: 'post', url: '/users/7/enable', data: undefined},
            {
                method: 'post',
                url: '/users/7/reset-password',
                data: {newPassword: 'NewSecret123!'},
            },
        ])
    })
})

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
