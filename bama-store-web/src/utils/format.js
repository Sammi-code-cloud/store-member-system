export const money = value => value == null ? '—' : Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
export const dateText = value => value ? value.replace('T', ' ').slice(0, 19) : '—'
export const localDate = (offset = 0) => {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  const get = key => parts.find(p => p.type === key).value
  const date = new Date(`${get('year')}-${get('month')}-${get('day')}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + offset)
  return date.toISOString().slice(0, 10)
}
export const reservationStates = { PENDING: '待店员确认', REJECTED: '已拒绝', WAITING: '待到店', USING: '使用中', VERIFIED: '已完成', CANCELLED: '已取消' }
export const stateTag = status => ({ PENDING: 'warning', REJECTED: 'danger', WAITING: 'success', USING: 'primary', VERIFIED: 'success', CANCELLED: 'info' }[status] || 'info')
export const txnTypes = { RECHARGE: '充值本金', GIFT: '储值赠送', CONSUME: '消费扣款', REFUND: '退款' }
export const levels = { NORMAL: '普通顾客', GOLD: '金卡会员', BLACK_GOLD: '黑金会员' }
export const endTime = row => {
  if (!row.startTime || row.hours == null) return '—'
  const [h, m] = row.startTime.split(':').map(Number)
  const total = h * 60 + m + Math.round(Number(row.hours) * 60)
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}
export async function confirmAction(ElMessageBox, text, title = '操作确认') {
  try { await ElMessageBox.confirm(text, title, { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); return true } catch { return false }
}
