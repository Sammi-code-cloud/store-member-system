import request from '@/utils/request'

/**
 * 后端接口封装
 * 路径与 bama-store-admin 的 Controller 一一对应
 */
export const api = {
  // ---------- 认证 ----------
  login: data => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),

  // ---------- 数据概览（源自员工端） ----------
  dashboard: () => request.get('/dashboard'),

  // ---------- 会员 ----------
  memberPage: params => request.get('/members', { params }),
  memberDetail: id => request.get(`/members/${id}`),
  memberAccount: id => request.get(`/members/${id}/account`),

  // ---------- 代客储值（源自员工端） ----------
  recharge: data => request.post('/account/recharge', data),

  // ---------- 预定与核销（源自员工端） ----------
  reservationPage: params => request.get('/reservations', { params }),
  reservationCreate: data => request.post('/reservations', data),
  reservationVerify: id => request.post(`/reservations/${id}/verify`),

  // ---------- 货品 ----------
  productPage: params => request.get('/products', { params }),
  productSave: data => request.post('/products', data),
  productStatus: (id, status) => request.put(`/products/${id}/status`, null, { params: { status } }),
  productDelete: id => request.delete(`/products/${id}`),

  // ---------- 员工与权限 ----------
  staffPage: params => request.get('/staff', { params }),
  staffCreate: data => request.post('/staff', data),
  staffStatus: (id, status) => request.put(`/staff/${id}/status`, null, { params: { status } }),
  staffResetPassword: (id, password) => request.put(`/staff/${id}/password`, null, { params: { password } }),
  roles: () => request.get('/roles'),
  permissions: () => request.get('/permissions'),

  // ---------- 茶室 ----------
  roomList: () => request.get('/rooms'),
  roomSave: data => request.post('/rooms', data),
  roomDelete: id => request.delete(`/rooms/${id}`),

  // ---------- 门店 ----------
  storeDetail: id => request.get(`/store/${id}`),
  storeUpdate: (id, data) => request.put(`/store/${id}`, data)
}

export default api
