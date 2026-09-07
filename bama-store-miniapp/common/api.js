// 后端接口封装
import request from './request.js'
const customerId = () => uni.getStorageSync('customer_user')?.memberId

// 拼接 query 参数
function qs(params) {
  const s = Object.keys(params)
    .filter((k) => params[k] !== undefined && params[k] !== null)
    .map((k) => `${k}=${encodeURIComponent(params[k])}`)
    .join('&')
  return s ? `?${s}` : ''
}

export default {
  reservationConfirm: id => request({url:`/api/reservations/${id}/confirm`,method:'POST'}),
  reservationReject: (id,reason) => request({url:`/api/reservations/${id}/reject`,method:'POST',data:{reason}}),
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

  // ===== 顾客身份与个人数据 =====
  customerLogin: data => request({ url: '/api/customer/auth/login', method: 'POST', data }),
  customerRegister: data => request({ url: '/api/customer/auth/register', method: 'POST', data }),
  customerCancel: (id, reason) => request({ url: `/api/customer/reservations/${id}/cancel`, method: 'POST', data: { reason } }),
  customerHome: (memberId = customerId()) => request({ url: `/api/customer/${memberId}` }),
  customerBookingContact: () => request({ url: '/api/customer/booking-contact' }),
  customerRecords: (memberId = customerId()) => request({ url: `/api/customer/${memberId}/records` }),
  customerStores: () => request({ url: '/api/customer/stores' }),
  customerBanners: (storeId) => request({ url: '/api/customer/banners' + qs({ storeId }) }),
  customerRooms: (storeId) => request({ url: '/api/customer/rooms' + qs({ storeId }) }),
  customerProducts: (storeId) => request({ url: '/api/customer/products' + qs({ storeId }) }),
  customerReservations: (memberId = customerId()) => request({ url: `/api/customer/${memberId}/reservations` }),
  customerSlots: (roomId, date, hours = 1) => request({ url: `/api/customer/rooms/${roomId}/slots` + qs({ date, hours }) }),
  customerReserved: (roomId, date) => request({ url: `/api/customer/rooms/${roomId}/reserved` + qs({ date }) }),
  customerReserve: (data) => request({ url: '/api/customer/reserve', method: 'POST', data }),
  customerPaycode: (memberId = customerId()) => request({ url: `/api/customer/${memberId}/paycode`, method: 'POST' })
}
