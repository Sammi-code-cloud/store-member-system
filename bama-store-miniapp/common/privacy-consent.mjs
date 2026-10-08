export const PRIVACY_VERSION = '2026-10-08.1'
export const CONSENT_KEY = 'privacy_consent_v1' // gitleaks:allow (public local storage identifier)

export function hasPrivacyConsent(runtime = globalThis.uni, member = false) {
  const consent = runtime?.getStorageSync?.(CONSENT_KEY)
  return !!(consent && consent.version === PRIVACY_VERSION && consent.terms === true &&
    consent.privacy === true && Number.isFinite(consent.acceptedAt) && consent.acceptedAt > 0 &&
    (!member || consent.memberFinancial === true))
}

export function assertPrivacyConsent(runtime = globalThis.uni, member = false) {
  if (!hasPrivacyConsent(runtime, member)) {
    throw new Error(member ? '请先阅读并同意协议及会员资金信息处理说明' : '请先阅读并同意用户服务协议和隐私政策')
  }
}

// Only the affirmative consent component may call this after its checkbox and native checks.
export function savePrivacyConsent(runtime, { agreed, member = false, financial = false, nativeAuthorized = false }) {
  if (agreed !== true || nativeAuthorized !== true || (member && financial !== true)) {
    throw new Error('尚未完成隐私授权')
  }
  const previousFinancial = hasPrivacyConsent(runtime, true)
  const record = { version: PRIVACY_VERSION, acceptedAt: Date.now(), terms: true, privacy: true,
    memberFinancial: previousFinancial || (member && financial === true) }
  runtime.setStorageSync(CONSENT_KEY, record)
  return record
}

export function clearPrivateSessions(runtime = globalThis.uni) {
  for (const key of ['customer_token', 'customer_user', 'token', 'staff', 'app_identity']) {
    runtime?.removeStorageSync?.(key)
  }
}

export function revokePrivacyConsent(runtime = globalThis.uni) {
  runtime?.removeStorageSync?.(CONSENT_KEY)
  clearPrivateSessions(runtime)
}

export function privacyLogin(runtime = globalThis.uni, member = false) {
  assertPrivacyConsent(runtime, member)
  const acceptedAt = runtime.getStorageSync(CONSENT_KEY).acceptedAt
  return new Promise((resolve, reject) => runtime.login({
    provider: 'weixin',
    success: result => {
      try {
        assertPrivacyConsent(runtime, member)
        if (runtime.getStorageSync(CONSENT_KEY).acceptedAt !== acceptedAt) throw new Error('授权状态已改变，请重试')
        resolve(result)
      } catch (error) { reject(error) }
    },
    fail: () => reject(new Error('微信授权失败，请重试'))
  }))
}

// Public browsing never carries a customer or employee token.
export function isPublicRequest(options) {
  if ((options.method || 'GET').toUpperCase() !== 'GET') return false
  const path = options.url.split('?')[0]
  return ['/api/wechat/config', '/api/customer/stores', '/api/customer/banners',
    '/api/customer/rooms', '/api/customer/products'].includes(path) ||
    /^\/api\/customer\/rooms\/\d+\/(slots|reserved)$/.test(path)
}

export function isMemberRequest(options) {
  return options.url.startsWith('/api/customer/') || options.url.startsWith('/api/wechat/customer/')
}
