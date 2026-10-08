import { privacyLogin } from './privacy-consent.mjs'
export function parseStaffScene(scene) {
  if (typeof scene !== 'string' || scene.length > 96) return null
  let decoded
  try { decoded = decodeURIComponent(scene) } catch { return null }
  return /^b=([A-Za-z0-9_-]{22})$/.exec(decoded)?.[1] || null
}

export async function bindStaffWechat(ticket, phone, runtime, request) {
  if (typeof ticket !== 'string' || !/^[A-Za-z0-9_-]{22}$/.test(ticket)) throw new Error('绑定码无效，请重新扫码')
  if (!/^1[3-9]\d{9}$/.test(phone)) throw new Error('请填写员工手机号')
  const login = await privacyLogin(runtime)
  if (!login.code) throw new Error('未获取到微信凭证，请在微信小程序内操作')
  return request({url: '/api/wechat/staff/bind', method: 'POST', data: {ticket, phone, code: login.code}})
}
