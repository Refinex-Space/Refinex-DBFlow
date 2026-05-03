import {z} from 'zod'
import {useForm} from 'react-hook-form'
import {CaretSortIcon, CheckIcon} from '@radix-ui/react-icons'
import {zodResolver} from '@hookform/resolvers/zod'
import {showSubmittedData} from '@/lib/show-submitted-data'
import {cn} from '@/lib/utils'
import {Button} from '@/components/ui/button'
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList,} from '@/components/ui/command'
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage,} from '@/components/ui/form'
import {Input} from '@/components/ui/input'
import {Popover, PopoverContent, PopoverTrigger,} from '@/components/ui/popover'
import {DatePicker} from '@/components/date-picker'

const languages = [
    {label: '英文', value: 'en'},
    {label: '法文', value: 'fr'},
    {label: '德文', value: 'de'},
    {label: '西班牙文', value: 'es'},
    {label: '葡萄牙文', value: 'pt'},
    {label: '俄文', value: 'ru'},
    {label: '日文', value: 'ja'},
    {label: '韩文', value: 'ko'},
    {label: '中文', value: 'zh'},
] as const

const accountFormSchema = z.object({
    name: z
        .string()
        .min(1, '请输入名称。')
        .min(2, '名称至少 2 个字符。')
        .max(30, '名称不能超过 30 个字符。'),
    dob: z.date('请选择日期。'),
    language: z.string('请选择语言。'),
})

type AccountFormValues = z.infer<typeof accountFormSchema>

const defaultValues: Partial<AccountFormValues> = {
    name: '',
}

export function AccountForm() {
    const form = useForm<AccountFormValues>({
        resolver: zodResolver(accountFormSchema),
        defaultValues,
    })

    function onSubmit(data: AccountFormValues) {
        showSubmittedData(data)
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-8'>
                <FormField
                    control={form.control}
                    name='name'
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>名称</FormLabel>
                            <FormControl>
                                <Input placeholder='管理员' {...field} />
                            </FormControl>
                            <FormMessage/>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name='dob'
                    render={({field}) => (
                        <FormItem className='flex flex-col'>
                            <FormLabel>日期</FormLabel>
                            <DatePicker selected={field.value} onSelect={field.onChange}/>
                            <FormMessage/>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name='language'
                    render={({field}) => (
                        <FormItem className='flex flex-col'>
                            <FormLabel>语言</FormLabel>
                            <Popover>
                                <PopoverTrigger asChild>
                                    <FormControl>
                                        <Button
                                            variant='outline'
                                            role='combobox'
                                            className={cn(
                                                'w-50 justify-between',
                                                !field.value && 'text-muted-foreground'
                                            )}
                                        >
                                            {field.value
                                                ? languages.find(
                                                    (language) => language.value === field.value
                                                )?.label
                                                : '选择语言'}
                                            <CaretSortIcon className='ms-2 h-4 w-4 shrink-0 opacity-50'/>
                                        </Button>
                                    </FormControl>
                                </PopoverTrigger>
                                <PopoverContent className='w-50 p-0'>
                                    <Command>
                                        <CommandInput placeholder='搜索语言'/>
                                        <CommandEmpty>没有匹配语言。</CommandEmpty>
                                        <CommandGroup>
                                            <CommandList>
                                                {languages.map((language) => (
                                                    <CommandItem
                                                        value={language.label}
                                                        key={language.value}
                                                        onSelect={() => {
                                                            form.setValue('language', language.value)
                                                        }}
                                                    >
                                                        <CheckIcon
                                                            className={cn(
                                                                'size-4',
                                                                language.value === field.value
                                                                    ? 'opacity-100'
                                                                    : 'opacity-0'
                                                            )}
                                                        />
                                                        {language.label}
                                                    </CommandItem>
                                                ))}
                                            </CommandList>
                                        </CommandGroup>
                                    </Command>
                                </PopoverContent>
                            </Popover>
                            <FormMessage/>
                        </FormItem>
                    )}
                />
                <Button type='submit'>保存账户</Button>
            </form>
        </Form>
    )
}
