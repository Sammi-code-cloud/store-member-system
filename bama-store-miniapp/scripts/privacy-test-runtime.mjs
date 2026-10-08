import { CONSENT_KEY, PRIVACY_VERSION, assertPrivacyConsent, hasPrivacyConsent,
  privacyLogin, isPublicRequest, isMemberRequest } from '../common/privacy-consent.mjs'

// Existing business tests represent a user who has explicitly accepted the current policy.
// Refusal and withdrawal are tested against the actual, unmodified guard separately.
export function withConsentRuntime(runtime = {}) {
  const local = new Map([[CONSENT_KEY, {version:PRIVACY_VERSION,acceptedAt:1,terms:true,privacy:true,memberFinancial:true}]])
  return { ...runtime,
    getStorageSync(key) { return key === CONSENT_KEY ? local.get(key) : runtime.getStorageSync?.(key) },
    setStorageSync(key, value) { if(key === CONSENT_KEY)local.set(key,value);else runtime.setStorageSync?.(key,value) },
    removeStorageSync(key) { if(key === CONSENT_KEY)local.delete(key);else runtime.removeStorageSync?.(key) }
  }
}

export function evaluateSource(...args) {
  const body = args.pop(), names = args
  const injected = {PrivacyConsent:{},LegalLinks:{},CONSENT_KEY,assertPrivacyConsent,hasPrivacyConsent,
    privacyLogin,isPublicRequest,isMemberRequest}
  const evaluate = new Function(...Object.keys(injected), ...names, body)
  return (...values) => {
    const runtimeValues = values.map((value,i) => names[i] === 'uni' ? withConsentRuntime(value) : value)
    const result = evaluate(...Object.values(injected), ...runtimeValues)
    if(result?.data && result?.methods) {
      const data = result.data
      result.data = function() { return {...data.call(this), bookingPrivacyAccepted:true,
        $refs:{privacyConsent:{ensure:async()=>true}}} }
    }
    return result
  }
}
