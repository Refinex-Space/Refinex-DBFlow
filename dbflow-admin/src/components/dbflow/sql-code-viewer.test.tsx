import type {ComponentProps} from 'react'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import {render} from 'vitest-browser-react'
import {ThemeProvider} from '@/context/theme-provider'
import {SqlCodeViewer} from './sql-code-viewer'

type MonacoEditorProps = ComponentProps<
    typeof import('@monaco-editor/react').default
>

const editorProps = vi.hoisted(() => vi.fn())

vi.mock('@/lib/monaco', () => ({}))

vi.mock('@monaco-editor/react', () => ({
    default: (props: MonacoEditorProps) => {
        editorProps(props)

        return (
            <div
                role='region'
                aria-label='Monaco SQL Viewer'
                data-language={props.language}
                data-theme={props.theme}
                data-readonly={String(props.options?.readOnly)}
                data-minimap={String(props.options?.minimap?.enabled)}
                style={{height: props.height, width: props.width}}
            >
                {props.value}
            </div>
        )
    },
}))

describe('SqlCodeViewer', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        document.cookie = 'vite-ui-theme=; Max-Age=0; path=/'
        document.documentElement.classList.remove('light', 'dark')
    })

    it('renders SQL through a readonly Monaco editor', async () => {
        const screen = await render(
            <ThemeProvider defaultTheme='light'>
                <SqlCodeViewer sql='SELECT * FROM audit_log'/>
            </ThemeProvider>
        )

        const editor = screen.getByRole('region', {name: 'Monaco SQL Viewer'})
        await expect.element(editor).toHaveAttribute('data-language', 'sql')
        await expect.element(editor).toHaveAttribute('data-readonly', 'true')
        await expect.element(editor).toHaveAttribute('data-minimap', 'false')
        await expect.element(editor).toHaveTextContent('SELECT * FROM audit_log')
        expect(editor.element().style.height).toBe('220px')
        expect(editorProps).toHaveBeenLastCalledWith(
            expect.objectContaining({
                height: '220px',
                language: 'sql',
                options: expect.objectContaining({
                    ariaLabel: 'Monaco SQL Viewer',
                    readOnly: true,
                    domReadOnly: true,
                    minimap: {enabled: false},
                }),
                theme: 'light',
                value: 'SELECT * FROM audit_log',
            })
        )
    })

    it('uses Monaco dark theme when DBFlow admin is in dark mode', async () => {
        const screen = await render(
            <ThemeProvider defaultTheme='dark'>
                <SqlCodeViewer sql='DROP TABLE blocked_table'/>
            </ThemeProvider>
        )

        await expect
            .element(screen.getByRole('region', {name: 'Monaco SQL Viewer'}))
            .toHaveAttribute('data-theme', 'vs-dark')
    })
})
