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
