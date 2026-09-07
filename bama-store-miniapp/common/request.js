// 网络请求封装（基于 uni.request）

// 后端地址：本地联调用 localhost；真机 / 微信小程序需换成已备案的 https 域名
export const BASE_URL = 'http://localhost:8080'

export default function request(options) {
  return new Promise((resolve, reject) => {
    const customer = options.url.startsWith('/api/customer/')
    const token = uni.getStorageSync(customer ? 'customer_token' : 'token')
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {}),
        ...(options.header || {})
      },
      success: (res) => {
        const body = res.data
        // 统一返回体 { code, message, data }
        if (res.statusCode === 200 && body && body.code === 200) {
          resolve(body.data)
          return
        }
        const msg = (body && body.message) || '请求失败'
        if (body && body.code === 401) {
          uni.removeStorageSync(customer ? 'customer_token' : 'token')
          if (customer) uni.removeStorageSync('customer_user')
          uni.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          setTimeout(() => uni.reLaunch({ url: customer ? '/pages/customer/login' : '/pages/staff/login' }), 800)
        } else {
          uni.showToast({ title: msg, icon: 'none' })
        }
        reject(body || res)
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请检查后端服务', icon: 'none' })
        reject(err)
      }
    })
  })
}
