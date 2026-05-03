import {vi} from 'vitest'

export function installClipboardMock() {
    const writeText = vi.fn()

    Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: {writeText},
    })
    writeText.mockResolvedValue(undefined)

    return writeText
}
