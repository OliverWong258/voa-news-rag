import { describe, expect, it } from 'vitest'
import { SseParser } from './sse'

describe('SseParser', () => {
  it('buffers an event split across network chunks', () => {
    const parser = new SseParser()
    expect(parser.push('event:token\ndata:{"te')).toEqual([])
    expect(parser.push('xt":"你好"}\n\n')).toEqual([
      { event: 'token', data: '{"text":"你好"}' },
    ])
  })

  it('parses multiple events and CRLF delimiters', () => {
    const parser = new SseParser()
    expect(
      parser.push(
        'event:sources\r\ndata:{"sources":[]}\r\n\r\nevent:completed\r\ndata:{"grounded":false}\r\n\r\n',
      ),
    ).toEqual([
      { event: 'sources', data: '{"sources":[]}' },
      { event: 'completed', data: '{"grounded":false}' },
    ])
  })

  it('joins multiple data fields and ignores comments', () => {
    const parser = new SseParser()
    expect(parser.push(':heartbeat\nevent:token\ndata:first\ndata:second\n\n')).toEqual([
      { event: 'token', data: 'first\nsecond' },
    ])
  })

  it('flushes a final event without a trailing delimiter', () => {
    const parser = new SseParser()
    parser.push('event:completed\ndata:{"grounded":true}')
    expect(parser.finish()).toEqual([{ event: 'completed', data: '{"grounded":true}' }])
  })
})

