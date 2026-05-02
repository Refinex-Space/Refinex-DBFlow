/**
 * 管理端 Shell 元信息。
 */
export interface AdminSessionShell {
    adminName: string
    mcpStatus: string
    mcpTone: string
    configSourceLabel: string
}

/**
 * 当前管理员会话信息。
 */
export interface AdminSession {
    authenticated: boolean
    username: string
    displayName: string
    roles: string[]
    shell: AdminSessionShell
}
