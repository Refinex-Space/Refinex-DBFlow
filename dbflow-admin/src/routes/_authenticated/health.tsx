import {createFileRoute} from '@tanstack/react-router'
import {HealthPageView} from '@/features/health'

export const Route = createFileRoute('/_authenticated/health')({
    component: HealthPageView,
})
