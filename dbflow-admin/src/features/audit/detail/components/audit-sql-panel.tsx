import type {AuditEventDetail} from '@/types/audit'
import {SqlCodeViewer} from '@/components/dbflow/sql-code-viewer'

type AuditSqlPanelProps = {
    detail: AuditEventDetail
}

export function AuditSqlPanel({detail}: AuditSqlPanelProps) {
    return <SqlCodeViewer sql={detail.sqlText}/>
}
