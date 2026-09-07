import { test } from 'node:test'
import assert from 'node:assert/strict'
import { networkError } from '../common/network-error.mjs'
test('native WeChat request failures give distinct actionable errors', () => {
  for (const [reason, expected] of [
    ['request:fail url not in domain list', /合法域名/],
    ['request:fail ssl hand shake error', /证书/],
    ['request:fail timeout', /超时/],
    ['request:fail net::ERR_NAME_NOT_RESOLVED', /解析/],
    ['request:fail net::ERR_INTERNET_DISCONNECTED', /手机网络/],
    ['request:fail connect refused', /切换/]
  ]) {
    const error = networkError({errMsg:reason})
    assert.ok(error instanceof Error)
    assert.match(error.message,expected)
  }
})
