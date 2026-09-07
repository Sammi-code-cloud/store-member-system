import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../pages/customer/rooms.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
function setup(reserve){
 const component=new Function('api','CustomerNav',source)({customerReserve:reserve},{})
 const vm=component.data()
 for(const [name,method] of Object.entries(component.methods))vm[name]=method.bind(vm)
 vm.booking={roomId:1,roomName:'测试包间',storeName:'测试分店',date:'2026-09-08',time:'14:00',end:'15:00',hours:1,amount:'100'}
 vm.contact={name:' 测试联系人 ',phone:13800000000,guests:2,remark:' 测试 '}
 vm.loadSlots=()=>{}
 return vm
}
test('numeric phone from browser number input submits as text and closes form',async()=>{
 let payload;const vm=setup(async data=>{payload=data})
 await vm.submitBooking()
 assert.equal(payload.contactPhone,'13800000000')
 assert.equal(payload.contactName,'测试联系人')
 assert.equal(payload.guests,2)
 assert.equal(vm.booking,null)
 assert(vm.bookingResult)
 assert.equal(vm.submitting,false)
})
test('empty or malformed number shows validation without sending',async()=>{
 for(const phone of ['',null,123]){
 let calls=0;const vm=setup(async()=>calls++);vm.contact.phone=phone
 await vm.submitBooking();assert.equal(calls,0);assert.match(vm.bookingError,/手机号/);assert(vm.booking)
 }
})
test('server rejection keeps form and displays reason',async()=>{
 const vm=setup(async()=>{throw {message:'所选时段已被占用'}})
 await vm.submitBooking();assert.equal(vm.bookingError,'所选时段已被占用');assert(vm.booking);assert.equal(vm.submitting,false)
})
