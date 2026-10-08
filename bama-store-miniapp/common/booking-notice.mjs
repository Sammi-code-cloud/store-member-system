import { hasPrivacyConsent, privacyLogin } from './privacy-consent.mjs'
// Call directly from the user's submit handler, before any asynchronous request.
export function requestBookingNotice(api, templateIds, platform=globalThis.uni, report=()=>{}) {
  const failure=(stage,error={},status='')=>{
    const code=Number(error.errCode || error.errcode || 0)
    let message='微信订阅未完成，请重试。'
    let settings=false
    if(stage==='unavailable')message='当前微信环境不支持订阅消息，请在手机微信中打开小程序。'
    else if(stage==='login')message='微信登录授权失败，请重新登录小程序后订阅。'
    else if(stage==='register')message=error.message || '微信已允许订阅，但门店登记失败，请检查网络后重试。'
    else if(status==='reject' || code===20004){settings=true;message='预约提醒未获允许。请在通知设置中开启接收通知，并允许“预约提醒”，然后返回再次点击订阅。'}
    else if(status==='ban' || code===20005)message='微信已限制此订阅消息，请联系门店管理员处理。'
    else if(status==='filter')message='预约模板被微信过滤，请联系门店管理员检查模板。'
    else if(code===20001 || code===10004)message='微信未识别预约模板，请联系门店管理员核对配置。'
    else if(code===10002 || code===10003)message='微信订阅请求网络失败，请切换网络后重试。'
    else if(code===10005)message='微信未能显示授权窗口，请保持小程序在前台后重试。'
    if(code)message+=`（错误码 ${code}）`
    try{report({stage,code,status,message,settings})}catch{}
    return false
  }
  if(!hasPrivacyConsent(platform, true)) {
    try { report({stage:'privacy',message:'请先登录并完成隐私授权',settings:false}) } catch {}
    return Promise.resolve(false)
  }
  if(!templateIds.length || typeof platform?.requestSubscribeMessage!=='function')return Promise.resolve(failure('unavailable'))
  const ids=[...templateIds]
  return new Promise(resolve=>{
    try { platform.requestSubscribeMessage({
      tmplIds:ids,
      success:async result=>{
        if(!ids.some(id=>result[id]==='accept')){resolve(failure('subscribe',{},result[ids[0]]));return}
        let stage='login'
        try {
          const login=await privacyLogin(platform, true)
          if(!login.code)throw new Error('Missing login code')
          stage='register'
          await api.customerBookingNoticeRegister(login.code)
          resolve(true)
        }catch(error){resolve(failure(stage,error || {}))}
      },
      fail:error=>resolve(failure('subscribe',error || {}))
    }) }catch(error){resolve(failure('subscribe',error || {}))}
  })
}
