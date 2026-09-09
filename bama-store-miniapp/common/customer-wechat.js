import { auth } from './store.js'

// Short-lived credentials stay in memory, never in URLs or persistent storage.
let pending = null
export function pendingCustomerBinding() { return pending }
export function clearCustomerBinding() { pending = null }
export function completeCustomerWechat(result) {
  if (result?.audience !== 'CUSTOMER') throw new Error('登录身份不匹配，请重试')
  if(result.manualPhoneRequired){pending=null;uni.navigateTo({url:'/pages/customer/login?phoneRequired=1'});return false}
  if (result.bindRequired) {
    if (!result.bindTicket) throw new Error('微信登录请求无效，请重试')
    pending = { ticket: result.bindTicket, smsEnabled: result.smsEnabled === true, expiresAt: Date.now() + 300000 }
    uni.removeStorageSync('customer_token')
    uni.removeStorageSync('customer_user')
    uni.navigateTo({ url: '/pages/customer/bind-phone' })
    return false
  }
  auth.setCustomerLogin(result.account)
  pending = null
  return true
}
