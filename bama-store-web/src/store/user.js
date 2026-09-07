import { defineStore } from 'pinia'
import api from '@/api'

/**
 * 登录态管理
 * 后端 LoginResponse 返回 token、员工信息与权限码集合 permissions
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    stores: [],
    activeStoreId: Number(localStorage.getItem('bm_active_store')) || null,
    token: localStorage.getItem('bm_token') || '',
    user: JSON.parse(localStorage.getItem('bm_user') || 'null')
  }),
  getters: {
    isLogin: state => !!state.token,
    permissions: state => state.user?.permissions || [],
    name: state => state.user?.name || '',
    storeId: state => state.activeStoreId || state.user?.storeId || 1
  },
  actions: {
    async loadStores() {
      this.stores = await api.storeList()
      const valid = this.stores.some(s => s.id === this.activeStoreId)
      const fallback = this.stores.find(s => s.id === this.user?.storeId) || this.stores[0]
      this.selectStore(valid ? this.activeStoreId : fallback?.id)
    },
    selectStore(id) {
      if (id == null) {
        this.activeStoreId = null
        localStorage.removeItem('bm_active_store')
        return
      }
      this.activeStoreId = Number(id)
      localStorage.setItem('bm_active_store', String(id))
    },
    async login(form) {
      const data = await api.login(form)
      this.acceptLogin(data)
      return data
    },
    acceptLogin(data) {
      this.token = data.token
      this.user = data
      this.selectStore(data.storeId)
      localStorage.setItem('bm_token', data.token)
      localStorage.setItem('bm_user', JSON.stringify(data))
      return data
    },
    logout() {
      this.activeStoreId = null
      this.stores = []
      localStorage.removeItem('bm_active_store')
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
