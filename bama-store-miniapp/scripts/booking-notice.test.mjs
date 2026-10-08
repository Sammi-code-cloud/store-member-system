import { withConsentRuntime } from './privacy-test-runtime.mjs'
import {test} from 'node:test'
import assert from 'node:assert/strict'
import {requestBookingNotice as originalrequestBookingNotice} from '../common/booking-notice.mjs'

test('subscription prompt opens synchronously; registers only after acceptance and login',async()=>{
 const calls=[]
 const api={customerBookingNoticeRegister:async code=>calls.push(code)}
 const platform={requestSubscribeMessage:opts=>{calls.push('prompt');opts.success({template:'accept'})},login:opts=>opts.success({code:'verified-code'})}
 const result=requestBookingNotice(api,['template'],platform)
 assert.equal(calls[0],'prompt');assert.equal(await result,true)
 assert.deepEqual(calls,['prompt','verified-code'])
})
test('refusal, unavailable API and failed authorization permit booking without registration',async()=>{
 let registered=0
 const api={customerBookingNoticeRegister:async()=>registered++}
 for(const platform of [{},{requestSubscribeMessage:o=>o.success({template:'reject'})},{requestSubscribeMessage:o=>o.fail({})},
  {requestSubscribeMessage:()=>{throw new Error('unavailable')}},
  {requestSubscribeMessage:o=>o.success({template:'accept'}),login:o=>o.fail({})}]){
  assert.equal(await requestBookingNotice(api,['template'],platform),false)
 }
 assert.equal(registered,0)
})
test('registration errors do not prevent booking',async()=>{
 const platform={requestSubscribeMessage:o=>o.success({template:'accept'}),login:o=>o.success({code:'code'})}
 assert.equal(await requestBookingNotice({customerBookingNoticeRegister:async()=>{throw new Error('offline')}},['template'],platform),false)
})

test('closed notification switch offers settings and never registers',async()=>{
 let failure
 const platform={requestSubscribeMessage:o=>o.fail({errCode:20004})}
 assert.equal(await requestBookingNotice({},['template'],platform,r=>failure=r),false)
 assert.equal(failure.settings,true);assert.equal(failure.code,20004)
})

test('refusal and registration failures have distinct recovery messages',async()=>{
 let failure
 await requestBookingNotice({},['template'],{requestSubscribeMessage:o=>o.success({template:'reject'})},r=>failure=r)
 assert.equal(failure.settings,true)
 await requestBookingNotice({customerBookingNoticeRegister:async()=>{throw {message:'当前微信与会员不一致'}}},['template'],{
  requestSubscribeMessage:o=>o.success({template:'accept'}),login:o=>o.success({code:'code'})
 },r=>failure=r)
 assert.equal(failure.stage,'register');assert.equal(failure.settings,false);assert.match(failure.message,/会员不一致/)
})

function requestBookingNotice(api,ids,platform,report){return originalrequestBookingNotice(api,ids,withConsentRuntime(platform),report)}
