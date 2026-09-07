// 简易登录态管理（基于本地存储）

export const auth = {
  setLogin(res) {
    uni.setStorageSync('app_identity','staff')
    uni.setStorageSync('token', res.token)
    uni.setStorageSync('staff', {
      staffId: res.staffId,
      name: res.name,
      staffNo: res.staffNo,
      storeId: res.storeId,
      permissions: res.permissions || []
    })
  },
  getStaff() {
    return uni.getStorageSync('staff') || null
  },
  getToken() {
    return uni.getStorageSync('token') || ''
  },
  isLogin() {
    return !!uni.getStorageSync('token')
  },
  preferStaff() { return this.isLogin() && uni.getStorageSync('app_identity') !== 'customer' },
  switchToCustomer() { uni.setStorageSync('app_identity','customer'); uni.reLaunch({url:'/pages/customer/home'}) },
  switchToStaff() { uni.setStorageSync('app_identity','staff'); uni.reLaunch({url:this.isLogin()?'/pages/staff/workbench':'/pages/staff/login'}) },
  // 是否拥有某权限
  can(code) {
    const staff = this.getStaff()
    return staff && staff.permissions && staff.permissions.indexOf(code) > -1
  },
  logout() {
    uni.removeStorageSync('token')
    uni.removeStorageSync('staff')
    uni.removeStorageSync('app_identity')
  }
}
