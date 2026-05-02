import {useState} from 'react'
import {Check, Copy} from 'lucide-react'
import {toast} from 'sonner'
import {cn} from '@/lib/utils'
import {Button} from '@/components/ui/button'

type CopyButtonProps = {
    value: string
    label?: string
    successMessage?: string
    errorMessage?: string
    className?: string
}

export function CopyButton({
                               value,
                               label = '复制',
                               successMessage = '已复制到剪贴板',
                               errorMessage = '复制失败',
                               className,
                           }: CopyButtonProps) {
    const [copied, setCopied] = useState(false)

    const handleCopy = async () => {
        try {
            await navigator.clipboard.writeText(value)
            setCopied(true)
            toast.success(successMessage)
            window.setTimeout(() => setCopied(false), 1200)
        } catch {
            toast.error(errorMessage)
        }
    }

    const Icon = copied ? Check : Copy

    return (
        <Button
            type='button'
            variant='outline'
            size='sm'
            className={cn('h-8', className)}
            onClick={handleCopy}
        >
            <Icon/>
            {label}
        </Button>
    )
}
