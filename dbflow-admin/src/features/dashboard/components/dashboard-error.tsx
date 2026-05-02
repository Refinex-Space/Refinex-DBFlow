import {AlertTriangle} from 'lucide-react'
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert'

type DashboardErrorProps = {
    message: string
}

export function DashboardError({message}: DashboardErrorProps) {
    return (
        <Alert variant='destructive'>
            <AlertTriangle/>
            <AlertTitle>总览加载失败</AlertTitle>
            <AlertDescription>{message}</AlertDescription>
        </Alert>
    )
}
