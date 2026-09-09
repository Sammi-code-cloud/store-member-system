// 简易登录态管理（基于本地存储）

export const auth = {
  setCustomerLogin(res) {
    if (!res?.token || !res?.memberId) throw new Error('微信登录结果无效，请重试')
    uni.setStorageSync('app_identity', 'customer')
    uni.setStorageSync('customer_token', res.token)
    uni.setStorageSync('customer_user', { memberId: res.memberId, name: res.name })
  },
  setLogin(res) {
    uni.setStorageSync('app_identity','staff')
    this.saveStaffSession(res)
  },
  saveStaffSession(res) {
    if (!res?.token || !res?.staffId) throw new Error('员工登录结果无效，请重试')
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

// Entry visibility comes from a fresh authenticated response, never cached permissions.
export async function loadStaffEntry(auth, fetchSession, resolveWechatStaff, isCurrent = () => true) {
  let token = auth.getToken()
  try {
    let staff
    if (token) {
      try { staff = await fetchSession() } catch {
        if (auth.getToken()) return null
        token = ''
      }
    }
    if (!token) {
      if (!resolveWechatStaff) return null
      staff = await resolveWechatStaff()
      if (!isCurrent() || auth.getToken() !== token || !staff?.token || !staff?.staffId) return null
      auth.saveStaffSession(staff)
      token = auth.getToken()
    }
    if (!isCurrent() || auth.getToken() !== token || !staff?.staffId || !Array.isArray(staff.permissions)) return null
    return staff.permissions.includes('dashboard:view') ? staff : null
  } catch {
    return null
  }
}
