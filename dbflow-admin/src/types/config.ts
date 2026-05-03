export type ConfigRow = {
    project: string
    projectName: string
    env: string
    envName: string
    datasource: string
    type: string
    host: string
    port: string
    schema: string
    username: string
    limits: string
    syncStatus: string
}

export type ConfigPage = {
    sourceLabel: string
    rows: ConfigRow[]
    emptyHint: string
}
