// 网络请求封装（基于 uni.request）
import { networkError } from './network-error.mjs'
import { CONSENT_KEY, assertPrivacyConsent, isPublicRequest, isMemberRequest } from './privacy-consent.mjs'

// 后端地址：本地联调用 localhost；真机 / 微信小程序需换成已备案的 https 域名
export const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8092').replace(/\/$/, '')

export default function request(options) {
  return new Promise((resolve, reject) => {
    const publicRequest = isPublicRequest(options)
    let acceptedAt
    if (!publicRequest) {
      try { assertPrivacyConsent(uni, isMemberRequest(options)); acceptedAt = uni.getStorageSync(CONSENT_KEY).acceptedAt }
      catch (error) { reject(error); return }
    }
    const customer = options.url.startsWith('/api/customer/')
    // WeChat exchanges are anonymous: never attach an unrelated/stale staff session.
    const anonymous = publicRequest || options.url.startsWith('/api/wechat/') || options.url === '/api/auth/login' || options.url === '/api/customer/stores'
    const token = anonymous ? '' : uni.getStorageSync(customer ? 'customer_token' : 'token')
    if (typeof window === 'undefined' && !BASE_URL.startsWith('https://')) {
      reject(new Error('小程序服务地址尚未配置，请联系门店管理员配置 HTTPS 服务地址'))
      return
    }
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      timeout: 15000,
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
        ...(options.header || {})
      },
      success: (res) => {
        if (!publicRequest) {
          try {
            assertPrivacyConsent(uni, isMemberRequest(options))
            if (uni.getStorageSync(CONSENT_KEY).acceptedAt !== acceptedAt) throw new Error('授权状态已改变，请重试')
          } catch (error) { reject(error); return }
        }
        const body = res.data
        // 统一返回体 { code, message, data }
        if (res.statusCode === 200 && body && body.code === 200) {
          resolve(body.data)
          return
        }
        const msg = (body && body.message) || '请求失败'
        if (body && body.code === 401) {
          const tokenKey = customer ? 'customer_token' : 'token'
          if (uni.getStorageSync(tokenKey) === token) {
            uni.removeStorageSync(tokenKey)
            uni.removeStorageSync(customer ? 'customer_user' : 'staff')
          }
          if (!options.silent) {
            uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
            setTimeout(() => uni.reLaunch({ url: customer ? '/pages/customer/login' : '/pages/staff/login' }), 800)
          }
        } else if (!options.silent) {
          uni.showToast({ title: msg, icon: 'none' })
        }
        reject(body || res)
      },
      fail: (err) => {
        const error = networkError(err)
        // Keep the native reason for debugging; never log credentials, bodies or query strings.
        error.errMsg = err.errMsg
        console.warn('[门店请求失败]', options.url.split('?')[0], String(err.errMsg || '').replace(/https?:\/\/\S+/g, '[服务地址]'))
        if (!options.silent) uni.showToast({ title: error.message, icon: 'none', duration: 4000 })
        reject(error)
      }
    })
  })
}
