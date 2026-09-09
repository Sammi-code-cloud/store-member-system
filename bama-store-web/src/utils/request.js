import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 统一请求实例：开发期由 Vite 代理转发到后端，生产期由 Nginx 反代
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || import.meta.env.BASE_URL + 'api',
  timeout: 15000
})

// 请求拦截：携带 JWT
request.interceptors.request.use(config => {
  const token = localStorage.getItem('bm_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  const storeId = localStorage.getItem('bm_active_store')
  if (token && storeId && !config.url.startsWith('/auth/') && config.url !== '/store' && !config.url.startsWith('/customer/')) config.headers['X-Store-Id'] = storeId
  return config
})

// 响应拦截：拆包 Result{code,message,data}，统一错误提示
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) return res.data
    if (res.code === 401) {
      localStorage.removeItem('bm_token')
      localStorage.removeItem('bm_user')
      router.replace('/login')
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    const status = error.response?.status
    if (status === 401) {
      // Token 失效，清理登录态并回到登录页
      localStorage.removeItem('bm_token')
      localStorage.removeItem('bm_user')
      router.replace('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('无此操作权限')
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
