import {Separator} from '@/components/ui/separator'

type ContentSectionProps = {
    title: string
    children: React.JSX.Element
}

export function ContentSection({title, children}: ContentSectionProps) {
    return (
        <div className='flex flex-1 flex-col'>
            <div className='flex-none'>
                <h3 className='text-lg font-medium'>{title}</h3>
            </div>
            <Separator className='my-3 flex-none'/>
            <div className='faded-bottom h-full w-full overflow-y-auto scroll-smooth pe-4 pb-12'>
                <div className='-mx-1 px-1.5 lg:max-w-xl'>{children}</div>
            </div>
        </div>
    )
}
