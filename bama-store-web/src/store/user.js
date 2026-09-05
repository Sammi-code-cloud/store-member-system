import { defineStore } from 'pinia'
import api from '@/api'

/**
 * 登录态管理
 * 后端 LoginResponse 返回 token、员工信息与权限码集合 permissions
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('bm_token') || '',
    user: JSON.parse(localStorage.getItem('bm_user') || 'null')
  }),
  getters: {
    isLogin: state => !!state.token,
    permissions: state => state.user?.permissions || [],
    name: state => state.user?.name || '',
    storeId: state => state.user?.storeId || 1
  },
  actions: {
    async login(form) {
      const data = await api.login(form)
      this.token = data.token
      this.user = data
      localStorage.setItem('bm_token', data.token)
      localStorage.setItem('bm_user', JSON.stringify(data))
      return data
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('bm_token')
      localStorage.removeItem('bm_user')
    },
    // 判断是否拥有某权限码，如 account:recharge
    has(code) {
      return !code || this.permissions.includes(code)
    }
  }
})
