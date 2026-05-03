import {z} from 'zod'
import {useFieldArray, useForm} from 'react-hook-form'
import {zodResolver} from '@hookform/resolvers/zod'
import {showSubmittedData} from '@/lib/show-submitted-data'
import {cn} from '@/lib/utils'
import {Button} from '@/components/ui/button'
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage,} from '@/components/ui/form'
import {Input} from '@/components/ui/input'
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from '@/components/ui/select'
import {Textarea} from '@/components/ui/textarea'

const profileFormSchema = z.object({
    username: z
        .string('请输入用户名。')
        .min(2, '用户名至少 2 个字符。')
        .max(30, '用户名不能超过 30 个字符。'),
    email: z.email({
        error: (iss) =>
            iss.input === undefined
                ? '请选择邮箱。'
                : undefined,
    }),
    bio: z.string().max(160).min(4),
    urls: z
        .array(
            z.object({
                value: z.url('请输入有效链接。'),
            })
        )
        .optional(),
})

type ProfileFormValues = z.infer<typeof profileFormSchema>

const defaultValues: Partial<ProfileFormValues> = {
    bio: 'DBFlow 管理员',
    urls: [
        {value: 'https://dbflow.local'},
    ],
}

export function ProfileForm() {
    const form = useForm<ProfileFormValues>({
        resolver: zodResolver(profileFormSchema),
        defaultValues,
        mode: 'onChange',
    })

    const {fields, append} = useFieldArray({
        name: 'urls',
        control: form.control,
    })

    return (
        <Form {...form}>
            <form
                onSubmit={form.handleSubmit((data) => showSubmittedData(data))}
                className='space-y-8'
            >
                <FormField
                    control={form.control}
                    name='username'
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>用户名</FormLabel>
                            <FormControl>
                                <Input placeholder='admin' {...field} />
                            </FormControl>
                            <FormMessage/>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name='email'
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>邮箱</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                                <FormControl>
                                    <SelectTrigger>
                                        <SelectValue placeholder='选择邮箱'/>
                                    </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                    <SelectItem value='m@example.com'>m@example.com</SelectItem>
                                    <SelectItem value='m@google.com'>m@google.com</SelectItem>
                                    <SelectItem value='m@support.com'>m@support.com</SelectItem>
                                </SelectContent>
                            </Select>
                            <FormMessage/>
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name='bio'
                    render={({field}) => (
                        <FormItem>
                            <FormLabel>备注</FormLabel>
                            <FormControl>
                                <Textarea
                                    placeholder='填写备注'
                                    className='resize-none'
                                    {...field}
                                />
                            </FormControl>
                            <FormMessage/>
                        </FormItem>
                    )}
                />
                <div>
                    {fields.map((field, index) => (
                        <FormField
                            control={form.control}
                            key={field.id}
                            name={`urls.${index}.value`}
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel className={cn(index !== 0 && 'sr-only')}>
                                        链接
                                    </FormLabel>
                                    <FormControl className={cn(index !== 0 && 'mt-1.5')}>
                                        <Input {...field} />
                                    </FormControl>
                                    <FormMessage/>
                                </FormItem>
                            )}
                        />
                    ))}
                    <Button
                        type='button'
                        variant='outline'
                        size='sm'
                        className='mt-2'
                        onClick={() => append({value: ''})}
                    >
                        添加链接
                    </Button>
                </div>
                <Button type='submit'>保存资料</Button>
            </form>
        </Form>
    )
}
