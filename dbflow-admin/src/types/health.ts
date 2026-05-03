export type HealthItem = {
    name: string
    component: string
    status: string
    description: string
    detail: string
    tone: string
}

export type HealthPage = {
    overall: string
    tone: string
    totalCount: number
    unhealthyCount: number
    items: HealthItem[]
}
