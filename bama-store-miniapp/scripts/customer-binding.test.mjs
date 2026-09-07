import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source=readFileSync(new URL('../pages/customer/bind-phone.vue',import.meta.url),'utf8').split('<script>')[1].split('</script>')[0].replace(/^import .*$/gm,'').replace('export default','return')
function fixture({enabled=true,expired=false,failSend=false,failBind=false}={}) {
  const calls=[],routes=[],logins=[]
  const uni={showToast:()=>{},reLaunch:({url})=>routes.push(url),redirectTo:({url})=>routes.push(url)}
  const request=async options=>{
    calls.push(options)
    if(options.url.endsWith('/sms')) {if(failSend)throw new Error('短信发送失败');return {challenge:'challenge',retryAfter:60}}
    if(failBind)throw new Error('验证码错误')
    return {account:{token:'verified-token',memberId:9}}
  }
  const pending=()=>({ticket:'ticket',smsEnabled:enabled,expiresAt:Date.now()+(expired?-1:300000)})
  const component=new Function('uni','request','auth','pendingCustomerBinding','clearCustomerBinding','wechatLogin','completeCustomerWechat',source)(uni,request,{setCustomerLogin:r=>logins.push(r)},pending,()=>{},async()=>({}),()=>false)
  const vm=component.data()
  for(const [name,method] of Object.entries(component.methods))vm[name]=method.bind(vm)
  for(const [name,get] of Object.entries(component.computed))Object.defineProperty(vm,name,{get:get.bind(vm)})
  component.onLoad.call(vm)
  return {vm,calls,routes,logins}
}
test('first binding sends SMS, waits for verified response, then logs in',async()=>{
  const {vm,calls,routes,logins}=fixture()
  vm.phone='13912345678'
  assert.equal(!!vm.canBind,false)
  await vm.send()
  assert.equal(vm.remaining,60);assert.equal(logins.length,0)
  vm.code='123456';await vm.bind()
  assert.equal(calls[1].data.phone,'13912345678')
  assert.equal(calls[1].data.challenge,'challenge')
  assert.equal(logins[0].token,'verified-token')
  assert.deepEqual(routes,['/pages/customer/home'])
})
test('disabled SMS, expired login and invalid phone never request a code',async()=>{
  for(const options of [{enabled:false},{expired:true},{}]) {
    const {vm,calls}=fixture(options);vm.phone=Object.keys(options).length?'13912345678':'123'
    await vm.send();assert.equal(calls.length,0)
  }
})
test('changing the phone invalidates the previous challenge and cooldown persists',async()=>{
  const {vm,calls}=fixture();vm.phone='13912345678';await vm.send()
  vm.phone='13912345679';vm.phoneChanged();vm.code='123456'
  await vm.bind();await vm.send()
  assert.equal(calls.length,1);assert.equal(vm.challenge,'');assert.equal(!!vm.canBind,false)
})
test('SMS provider failure and invalid code do not store a login',async()=>{
  const failed=fixture({failSend:true});failed.vm.phone='13912345678';await failed.vm.send()
  assert.equal(failed.vm.challenge,'');assert.match(failed.vm.error,/发送失败/);assert.equal(failed.logins.length,0)
  const invalid=fixture({failBind:true});invalid.vm.phone='13912345678';await invalid.vm.send();invalid.vm.code='123456';await invalid.vm.bind()
  assert.equal(invalid.logins.length,0);assert.equal(invalid.routes.length,0);assert.match(invalid.vm.error,/验证码错误/)
})
