// 后端接口封装
import request from './request.js'

// 拼接 query 参数
function qs(params) {
  const s = Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null)
    .map((k) => `${k}=${encodeURIComponent(params[k])}`)
    .join('&')
  return s ? `?${s}` : ''
}

export default {
  // ===== 认证 =====
  login: (data) => request({ url: '/api/auth/login', method: 'POST', data }),
  me: () => request({ url: '/api/auth/me' }),

  // ===== 数据概览 =====
  dashboard: () => request({ url: '/api/dashboard' }),

  // ===== 会员 =====
  memberPage: (params) => request({ url: '/api/members' + qs(params) }),
  memberAccount: (id) => request({ url: `/api/members/${id}/account` }),

  // ===== 储值 =====
  recharge: (data) => request({ url: '/api/account/recharge', method: 'POST', data }),

  // ===== 付款码 / 扫码扣费 =====
  paycodeGenerate: (memberId) =>
    request({ url: '/api/paycode/generate' + qs({ memberId }), method: 'POST' }),
  chargeResolve: (payCode) =>
    request({ url: '/api/charge/resolve' + qs({ payCode }), method: 'POST' }),
  chargeConfirm: (data) => request({ url: '/api/charge/confirm', method: 'POST', data }),

  // ===== 货品 =====
  productPage: (params) => request({ url: '/api/products' + qs(params) }),

  // ===== 茶室 / 预定 =====
  roomList: () => request({ url: '/api/rooms' }),
  reservationPage: (params) => request({ url: '/api/reservations' + qs(params) }),
  reservationCreate: (data) => request({ url: '/api/reservations', method: 'POST', data }),
  reservationVerify: (id) => request({ url: `/api/reservations/${id}/verify`, method: 'POST' }),

  // ===== 员工 =====
  staffPage: (params) => request({ url: '/api/staff' + qs(params) }),
  roles: () => request({ url: '/api/roles' }),

  // ===== 顾客端公开接口（免登录） =====
  customerHome: (memberId) => request({ url: `/api/customer/${memberId}` }),
  customerRecords: (memberId) => request({ url: `/api/customer/${memberId}/records` }),
  customerRooms: () => request({ url: '/api/customer/rooms' }),
  customerProducts: () => request({ url: '/api/customer/products' }),
  customerReservations: (memberId) => request({ url: `/api/customer/${memberId}/reservations` }),
  customerSlots: (roomId, date) => request({ url: `/api/customer/rooms/${roomId}/slots` + qs({ date }) }),
  customerReserve: (data) => request({ url: '/api/customer/reserve', method: 'POST', data }),
  customerPaycode: (memberId) => request({ url: `/api/customer/${memberId}/paycode`, method: 'POST' })
}
