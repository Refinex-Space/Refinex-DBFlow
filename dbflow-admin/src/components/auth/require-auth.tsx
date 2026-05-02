import {Skeleton} from '@/components/ui/skeleton'

export function AuthRoutePending() {
    return (
        <div className='flex min-h-svh items-center justify-center p-6'>
            <div className='w-full max-w-3xl space-y-6'>
                <div className='space-y-3'>
                    <Skeleton className='h-5 w-40'/>
                    <Skeleton className='h-9 w-72'/>
                    <Skeleton className='h-4 w-full max-w-xl'/>
                </div>
                <div className='grid gap-4 md:grid-cols-3'>
                    <Skeleton className='h-28'/>
                    <Skeleton className='h-28'/>
                    <Skeleton className='h-28'/>
                </div>
            </div>
        </div>
    )
}
