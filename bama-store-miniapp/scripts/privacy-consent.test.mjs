import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {CONSENT_KEY,PRIVACY_VERSION,hasPrivacyConsent,savePrivacyConsent,revokePrivacyConsent,
  privacyLogin,assertPrivacyConsent,isPublicRequest,isMemberRequest} from '../common/privacy-consent.mjs'
function runtime() {
  const storage=new Map(), calls=[]
  const uni={getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,v),removeStorageSync:k=>storage.delete(k),
    login:o=>{calls.push('login');o.success({code:'fresh'})},request:o=>calls.push(o),showToast(){}}
  return {uni,storage,calls}
}
function accept(uni,member=true){savePrivacyConsent(uni,{agreed:true,member,financial:member,nativeAuthorized:true})}
function requestWith(uni) {
  const source=readFileSync(new URL('../common/request.js',import.meta.url),'utf8').replace(/^import .*$/gm,'')
    .replace('export const','const').replace('export default function request','return function request')
    .replace('import.meta.env.VITE_API_BASE_URL',"'https://example.test'")
  return new Function('uni','networkError','CONSENT_KEY','assertPrivacyConsent','isPublicRequest','isMemberRequest',source)(uni,e=>e,CONSENT_KEY,assertPrivacyConsent,isPublicRequest,isMemberRequest)
}
test('missing, malformed and outdated consent is refused; nothing defaults to accepted',()=>{
 const {uni,storage}=runtime();assert.equal(hasPrivacyConsent(uni),false)
 for(const record of [{}, {version:'old',terms:true,privacy:true,acceptedAt:1}, {version:PRIVACY_VERSION,terms:true,privacy:false,acceptedAt:1}]){
  storage.set(CONSENT_KEY,record);assert.equal(hasPrivacyConsent(uni),false)
 }
})
test('unchecked, native-refused and missing financial consent cannot be saved',()=>{
 const {uni}=runtime()
 for(const choice of [{agreed:false,nativeAuthorized:true},{agreed:true,nativeAuthorized:false},
  {agreed:true,nativeAuthorized:true,member:true,financial:false}])assert.throws(()=>savePrivacyConsent(uni,choice))
 assert.equal(hasPrivacyConsent(uni),false)
 accept(uni,false);assert.equal(hasPrivacyConsent(uni),true);assert.equal(hasPrivacyConsent(uni,true),false)
 accept(uni);assert.equal(hasPrivacyConsent(uni,true),true)
})
test('no consent blocks WeChat code collection, login, SMS and profile HTTP calls',async()=>{
 const {uni,calls}=runtime(),request=requestWith(uni)
 assert.throws(()=>privacyLogin(uni),/同意/)
 for(const url of ['/api/auth/login','/api/wechat/mini','/api/wechat/customer/sms','/api/customer/1'])
  await assert.rejects(request({url,method:'POST',data:{phone:'13800000000'}}),/同意/)
 assert.deepEqual(calls,[])
})
test('general consent does not authorize customer financial endpoints',async()=>{
 const {uni,calls}=runtime();accept(uni,false)
 await assert.rejects(requestWith(uni)({url:'/api/customer/1/records'}),/资金/)
 assert.equal(calls.length,0)
})
test('refusal still permits public browsing without stale customer or staff Authorization',async()=>{
 const {uni,storage,calls}=runtime();storage.set('token','old-staff');storage.set('customer_token','old-customer')
 const request=requestWith(uni)
 for(const url of ['/api/customer/stores','/api/customer/banners?storeId=1','/api/customer/rooms/1/slots?date=2026-10-08']){
  const promise=request({url});const options=calls.at(-1);assert.equal(options.header.Authorization,undefined)
  options.success({statusCode:200,data:{code:200,data:[]}});assert.deepEqual(await promise,[])
 }
 assert.equal(isPublicRequest({url:'/api/customer/stores',method:'POST'}),false)
 assert.equal(isPublicRequest({url:'/api/customer/booking-contact'}),false)
})
test('withdrawal clears both identities and blocks future login/data collection',async()=>{
 const {uni,storage,calls}=runtime();accept(uni)
 for(const key of ['token','staff','customer_token','customer_user','app_identity'])storage.set(key,'private')
 revokePrivacyConsent(uni);assert.equal(storage.size,0)
 assert.throws(()=>privacyLogin(uni),/同意/)
 await assert.rejects(requestWith(uni)({url:'/api/customer/reserve',method:'POST'}))
 assert.equal(calls.length,0)
})
test('in-flight private responses and login codes are discarded after withdrawal',async()=>{
 const {uni,calls}=runtime();accept(uni)
 const request=requestWith(uni),pending=request({url:'/api/customer/1'})
 revokePrivacyConsent(uni);calls[0].success({statusCode:200,data:{code:200,data:{phone:'13800000000'}}})
 await assert.rejects(pending,/同意/)
 accept(uni);let loginOptions;uni.login=o=>{loginOptions=o}
 const login=privacyLogin(uni,true);revokePrivacyConsent(uni);loginOptions.success({code:'private-code'})
 await assert.rejects(login,/同意/)
})
test('unconsented homepage does not silently look up WeChat staff',async()=>{
 const {uni,calls}=runtime()
 const source=readFileSync(new URL('../common/wechat.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replaceAll('export ','')
 const resolve=new Function('uni','wx','hasPrivacyConsent','privacyLogin','request',source+';return resolveWechatStaff')(uni,{login(){}},hasPrivacyConsent,privacyLogin,()=>assert.fail('No request'))
 assert.equal(await resolve(),null);assert.equal(calls.length,0)
})
const consentSource=readFileSync(new URL('../components/PrivacyConsent.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
function consentComponent(uni,wx,ready=true){
 const component=new Function('uni','wx','hasPrivacyConsent','savePrivacyConsent','legalReady',consentSource)(uni,wx,hasPrivacyConsent,savePrivacyConsent,ready)
 const state={...component.data(),member:true,$emit(){}}
 for(const [key,value] of Object.entries(component.methods))state[key]=value.bind(state)
 return {state,component}
}
test('first consent form is unchecked and merely reading documents grants nothing',async()=>{
 const {uni,storage}=runtime();uni.navigateTo=()=>{}
 const {state}=consentComponent(uni,undefined)
 assert.equal(state.agreed,false);assert.equal(state.financial,false)
 state.open('privacy');state.open('service');assert.equal(storage.size,0)
 assert.equal(await state.ensure(),false);assert.equal(storage.size,0)
})
test('native refusal saves nothing; affirmative native button plus both checkboxes is required',async()=>{
 const {uni}=runtime(),{state}=consentComponent(uni,{getPrivacySetting:o=>o.success({needAuthorization:true})})
 state.agreed=true;state.financial=true
 const refused=state.ensure();await Promise.resolve();assert.equal(state.nativePending,true)
 state.nativeDecline();assert.equal(await refused,false);assert.equal(hasPrivacyConsent(uni),false)
 const agreed=state.ensure();await Promise.resolve();state.nativeAgree()
 assert.equal(await agreed,true);assert.equal(hasPrivacyConsent(uni,true),true)
})
test('native API failure, old WeChat and missing operator information fail closed',async()=>{
 for(const [wx,ready] of [[{getPrivacySetting:o=>o.fail({})},true],[{},true],[undefined,false]]){
  const {uni}=runtime(),{state}=consentComponent(uni,wx,ready);state.agreed=true;state.financial=true
  assert.equal(await state.ensure(),false);assert.equal(hasPrivacyConsent(uni),false)
 }
})
test('appointment provider refusal sends no reservation or subscription request',async()=>{
 const source=readFileSync(new URL('../pages/customer/rooms.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
 const component=new Function('api','CustomerNav','requestBookingNotice',source)({customerReserve:()=>assert.fail('No booking')},{},()=>assert.fail('No subscription'))
 const state={...component.data(),booking:{roomId:1}}
 for(const [key,value] of Object.entries(component.methods))state[key]=value.bind(state)
 await state.submitBooking();assert.match(state.bookingError,/同意/);assert.equal(state.bookingPrivacyAccepted,false)
})
