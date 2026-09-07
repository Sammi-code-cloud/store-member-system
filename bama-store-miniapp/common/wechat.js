import request from './request.js'
export async function wechatLogin(audience) {
  const config = await request({url:'/api/wechat/config'})
  if(typeof wx !== 'undefined' && typeof wx.login === 'function') {
    if(!config.miniEnabled)throw new Error('微信小程序尚未配置，请先使用账号密码登录')
    const result=await new Promise((resolve,reject)=>uni.login({provider:'weixin',success:resolve,fail:()=>reject(new Error('微信授权未完成，请重试'))}))
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
