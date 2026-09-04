// 顾客端演示数据
// 说明：顾客端部分接口（微信登录、按 openid 取会员余额等）需后端补充，
// 此处先用演示数据占位；付款码为真实接口调用。后端补齐后替换为 api 调用即可。

export const demoMember = {
  memberId: 1,
  name: '陈明轩',
  phone: '138****6620',
  level: '黑金会员',
  discountText: '92 折',
  balance: '2860.00',
  points: 1880,
  visits: 38
}

export const demoRooms = [
  { id: 1, name: '观山茶室', roomType: '包厢', capacity: '4-6人', priceHour: 188, tag: '含茶艺师' },
  { id: 2, name: '听雨阁', roomType: '卡座', capacity: '2-4人', priceHour: 128, tag: '临窗' },
  { id: 3, name: '云雾厅', roomType: '包厢', capacity: '6-8人', priceHour: 288, tag: '商务' }
]

export const demoPackages = [
  { amount: 1000, gift: 100 },
  { amount: 3000, gift: 500, hot: true },
  { amount: 5000, gift: 1200 },
  { amount: 10000, gift: 3000, vip: true }
]

export const demoRecords = [
  { icon: '🍵', title: '观山茶室 · 2小时', time: '08-29 15:20 · 扫码消费', amount: '-¥376', type: 'out' },
  { icon: '🛍', title: '赛珍珠1000 铁观音', time: '08-28 11:05 · 扫码消费', amount: '-¥520', type: 'out' },
  { icon: '💳', title: '会员卡储值', time: '08-25 09:40 · 赠 ¥500', amount: '+¥3,500', type: 'in' },
  { icon: '🍵', title: '听雨阁 · 1.5小时', time: '08-23 16:10 · 扫码消费', amount: '-¥192', type: 'out' }
]
