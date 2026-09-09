/* Shared same-origin website login popup. No AppSecret or openid accepted from the browser. */
window.WechatLogin = {
 async open(audience) {
  const response=await fetch('./api/wechat/config');const configuration=await response.json();
  if(configuration.code!==200 || !configuration.data?.webEnabled)throw new Error(configuration.message && configuration.code!==200 ? configuration.message : '网站微信登录尚未配置，请先使用账号密码登录');
  return new Promise((resolve,reject)=>{
   const popup=window.open('./wechat-login.html?audience='+encodeURIComponent(audience),'wechat_login','width=480,height=700');
   if(!popup)return reject(new Error('请允许弹出微信登录窗口'));
   let timer, poll;
   const cleanup=()=>{clearTimeout(timer);clearInterval(poll);window.removeEventListener('message',receive)};
   const receive=e=>{if(e.origin!==location.origin||e.source!==popup||e.data?.type!=='bama-wechat-login')return;cleanup();popup.close();if(e.data.error)return reject(new Error(e.data.error));if(e.data.result?.audience!==audience)return reject(new Error('登录身份不匹配，请重试'));resolve(e.data.result)};
   window.addEventListener('message',receive);
   timer=setTimeout(()=>{cleanup();popup.close();reject(new Error('微信登录超时，请重试'))},300000);
   poll=setInterval(()=>{if(popup.closed){cleanup();reject(new Error('已取消微信登录'))}},500);
  });
 }
};
