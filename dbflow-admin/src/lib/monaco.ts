import {loader} from '@monaco-editor/react'
import 'monaco-editor/esm/vs/basic-languages/sql/sql.contribution.js'
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api.js'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'

type MonacoEnvironment = {
    getWorker: (_moduleId: string, _label: string) => Worker
}
    ;
(
    globalThis as typeof globalThis & { MonacoEnvironment?: MonacoEnvironment }
).MonacoEnvironment = {
    getWorker: () => new editorWorker(),
}

loader.config({monaco})
