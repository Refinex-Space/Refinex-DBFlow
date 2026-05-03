import {createFileRoute} from '@tanstack/react-router'
import {DangerousPoliciesPageView} from '@/features/policies/dangerous'

export const Route = createFileRoute('/_authenticated/policies/dangerous')({
    component: DangerousPoliciesPageView,
})
