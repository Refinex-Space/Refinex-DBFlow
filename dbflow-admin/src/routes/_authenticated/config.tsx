import {createFileRoute} from '@tanstack/react-router'
import {ConfigPageView} from '@/features/config'

export const Route = createFileRoute('/_authenticated/config')({
    component: ConfigPageView,
})
