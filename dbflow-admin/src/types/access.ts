export type UserStatus = 'ACTIVE' | 'DISABLED'

export type AdminUserRow = {
    id: number
    username: string
    displayName: string
    role: string
    status: UserStatus | string
    grantCount: number
    activeTokenCount: number
}

export type UserFilters = {
    username?: string
    status?: UserStatus | string
}

export type CreateUserRequest = {
    username: string
    displayName: string
    password?: string
}

export type ResetPasswordRequest = {
    newPassword: string
}

export type UserMutationResult = Record<string, boolean>

export type GrantType = 'READ' | 'WRITE' | 'ADMIN'

export type GrantStatus = 'ACTIVE' | 'REVOKED'

export type GrantEnvEntry = {
    grantId: number
    environmentKey: string
    grantType: GrantType | string
    status: GrantStatus | string
}

export type GrantGroupRow = {
    userId: number
    username: string
    projectKey: string
    environments: GrantEnvEntry[]
}

export type GrantEnvironmentOption = {
    projectKey: string
    projectName: string
    environmentKey: string
    environmentName: string
}

export type UserOption = {
    id: number
    username: string
    displayName: string
}

export type GrantOptionsResponse = {
    users: UserOption[]
    environments: GrantEnvironmentOption[]
}

export type GrantFilters = {
    username?: string
    projectKey?: string
    environmentKey?: string
    status?: string
}

export type UpdateProjectGrantsRequest = {
    userId: number
    projectKey: string
    environmentKeys: string[]
    grantType: GrantType | string
}

export type GrantMutationResult = Record<string, boolean>
