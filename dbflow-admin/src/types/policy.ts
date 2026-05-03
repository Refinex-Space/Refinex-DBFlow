export type PolicyDefaultRow = {
    operation: string
    risk: string
    decision: string
    requirement: string
    tone: string
}

export type PolicyWhitelistRow = {
    operation: string
    risk: string
    project: string
    env: string
    schema: string
    table: string
    allowProd: string
    prodRule: string
    tone: string
}

export type PolicyRuleRow = {
    name: string
    status: string
    description: string
    detail: string
    tone: string
}

export type DangerousPolicyPage = {
    defaults: PolicyDefaultRow[]
    whitelist: PolicyWhitelistRow[]
    rules: PolicyRuleRow[]
    emptyHint: string
}

export type PolicyReason =
    | {
    kind: 'default'
    title: string
    risk: string
    decision: string
    requirement: string
}
    | {
    kind: 'whitelist'
    title: string
    risk: string
    scope: string
    allowProd: string
    prodRule: string
}
