import { withConsentRuntime } from './privacy-test-runtime.mjs'
import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {requestBookingNotice} from '../common/booking-notice.mjs'
const source=readFileSync(new URL('../pages/customer/rooms.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
function setup(platform,api){
 platform=withConsentRuntime(platform)
 const component=new Function('api','CustomerNav','requestBookingNotice','uni',source)(api,{},requestBookingNotice,platform)
 const vm=component.data();for(const [name,method] of Object.entries(component.methods))vm[name]=method.bind(vm)
 vm.bookingNoticeIds=['template'];return {vm,component}
}
test('manual subscription opens on tap, prevents duplicate taps and preserves page state on return',async()=>{
 let prompt,registered=0,refreshes=0;const toasts=[]
 const platform={requestSubscribeMessage:o=>{prompt=o},login:o=>o.success({code:'code'}),showToast:o=>toasts.push(o.title)}
 const {vm,component}=setup(platform,{customerBookingNoticeRegister:async()=>registered++})
 vm.initStores=()=>refreshes++;vm.loadContact=()=>refreshes++;vm.loadBookingNoticeSettings=()=>refreshes++
 const pending=vm.subscribeBookingNotice();assert(prompt);assert.equal(vm.bookingNoticeBusy,true)
 await vm.subscribeBookingNotice();component.onShow.call(vm);assert.equal(refreshes,0)
 await prompt.success({template:'accept'});await pending
 assert.equal(registered,1);assert.equal(vm.bookingNoticeBusy,false);assert.match(toasts[0],/已登记/)
})
test('declining manual subscription does not submit a reservation or show success',async()=>{
 let reservations=0;const toasts=[]
 const {vm}=setup({requestSubscribeMessage:o=>o.success({template:'reject'}),showToast:o=>toasts.push(o.title)}, {customerReserve:async()=>reservations++})
 await vm.subscribeBookingNotice();assert.equal(reservations,0);assert.equal(vm.bookingNoticeBusy,false);assert.match(toasts[0],/未完成订阅/)
})
