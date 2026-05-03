import {createFileRoute} from '@tanstack/react-router'
import {AuditDetailPage} from '@/features/audit/detail'

export const Route = createFileRoute('/_authenticated/audit/$eventId')({
    component: AuditDetailRoute,
})

function AuditDetailRoute() {
    const {eventId} = Route.useParams()

    return <AuditDetailPage eventId={Number(eventId)}/>
}
