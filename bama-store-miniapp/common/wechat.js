import { hasPrivacyConsent, assertPrivacyConsent, privacyLogin } from './privacy-consent.mjs'
import request from './request.js'
// A fresh WeChat code resolves an existing employee binding without opening a bind form.
let staffLookup = null
export async function resolveWechatStaff() {
  if (!hasPrivacyConsent(uni)) return null
  if (typeof wx === 'undefined' || typeof wx.login !== 'function') return null
  if (staffLookup) return staffLookup
  staffLookup = (async () => {
    try {
      const login = await privacyLogin(uni)
      if (!login.code) return null
      const result = await request({url:'/api/wechat/mini',method:'POST',silent:true,data:{code:login.code,audience:'STAFF'}})
      return result?.audience === 'STAFF' && result.bindRequired === false ? result.account : null
    } catch { return null }
  })()
  try { return await staffLookup } finally { staffLookup = null }
}
export async function wechatLogin(audience) {
  if(audience==='CUSTOMER')return loginWechatWithPhone('')
  assertPrivacyConsent(uni, audience === 'CUSTOMER')
  const config = await request({url:'/api/wechat/config'})
  if(typeof wx !== 'undefined' && typeof wx.login === 'function') {
    if(!config.miniEnabled)throw new Error('微信小程序尚未配置，请先使用账号密码登录')
    const result=await privacyLogin(uni)
    return request({url:'/api/wechat/mini',method:'POST',data:{code:result.code,audience}})
  }
  if(typeof window==='undefined')throw new Error('请在微信小程序或浏览器中使用微信登录')
  if(!config.webEnabled)throw new Error('网站微信登录尚未配置，请先使用账号密码登录')
  return new Promise((resolve,reject)=>{
    const popup=window.open('/wechat-login.html?audience='+audience,'wechat_login','width=480,height=700')
    if(!popup)return reject(new Error('请允许弹出微信登录窗口'))
    const cleanup=()=>{clearInterval(poll);clearTimeout(timer);window.removeEventListener('message',receive)}
    const receive=e=>{if(e.origin!==location.origin||e.source!==popup||e.data?.type!=='bama-wechat-login')return;cleanup();popup.close();if(e.data.error)return reject(new Error(e.data.error));if(e.data.result?.audience!==audience)return reject(new Error('登录身份不匹配'));resolve(e.data.result)}
    const poll=setInterval(()=>{if(popup.closed){cleanup();reject(new Error('已取消微信登录'))}},500)
    const timer=setTimeout(()=>{cleanup();popup.close();reject(new Error('微信登录超时'))},300000)
    window.addEventListener('message',receive)
  })
}
export const bindWechat = (ticket,phone,password) => request({url:'/api/wechat/bind',method:'POST',data:{ticket,phone,password}})

export async function loginWechatWithPhone(value,name='') {
  const phone=String(value || '').trim()
  if(phone && !/^1[3-9]\d{9}$/.test(phone))throw new Error('请输入正确的11位手机号')
  if(typeof wx==='undefined' || typeof wx.login!=='function')throw new Error('请在微信小程序中登录')
  const login=await privacyLogin(uni, true)
  if(!login.code)throw new Error('未取得微信登录凭证，请重试')
  return request({url:'/api/wechat/customer/manual-login',method:'POST',data:{code:login.code,phone,name:String(name || '').trim()}})
}

export function parseMemberScene(scene){
  if(typeof scene!=='string'||scene.length>96)return null
  try{return /^m=([A-Za-z0-9_-]{22})$/.exec(decodeURIComponent(scene))?.[1] || null}catch{return null}
}
export async function bindMemberWechat(ticket,value){
  const phone=String(value || '').trim()
  if(!/^[A-Za-z0-9_-]{22}$/.test(ticket || ''))throw new Error('绑定码无效，请门店重新生成')
  if(!/^1[3-9]\d{9}$/.test(phone))throw new Error('请输入登记的11位会员手机号')
  const login=await privacyLogin(uni, true)
  if(!login.code)throw new Error('未取得微信登录凭证，请重试')
  return request({url:'/api/wechat/customer/member-bind',method:'POST',data:{ticket,phone,code:login.code}})
}
