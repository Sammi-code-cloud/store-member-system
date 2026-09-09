import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const script=readFileSync(new URL('../pages/staff/recharge.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
const apiSource=readFileSync(new URL('../common/api.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace('export default','return')
const member={id:23,name:'会员甲',phone:'13800000003',status:1,balance:268}
function fixture(api,confirm=true){const messages=[],payments=[];const component=new Function('api','uni',script)({...api,recharge:async data=>payments.push(data)},{showToast:o=>messages.push(o.title),showModal:o=>{messages.push(o.content);o.success?.({confirm})},navigateBack(){}});const state=Object.assign(component.data(),component.methods);state.phone=member.phone;return{state,component,messages,payments}}
test('phone lookup ignores name-only matches and disabled members',async()=>{
 let url
 const api=new Function('request',apiSource)(async o=>{url=o.url;return {total:3,records:[{...member,id:1,phone:'13800000001',name:member.phone},{...member,id:2,status:0},member]}})
 assert.equal((await api.memberByPhone(member.phone)).id,23)
 assert(url.includes('status=1'));assert(url.includes('keyword='+member.phone))
 await assert.rejects(api.memberByPhone('123'),/11位/)
 const empty=new Function('request',apiSource)(async()=>({total:0,records:[]}))
 await assert.rejects(empty.memberByPhone(member.phone),/未找到/)
})
test('editing phone invalidates pending search and selected member',async()=>{
 let resolve
 const {state}=fixture({memberByPhone:()=>new Promise(r=>{resolve=r})})
 const pending=state.searchMember();state.phone='13800000004';state.clearMember();resolve(member);await pending
 assert.equal(state.member,null);assert.equal(state.searching,false)
})
test('no default member, invalid amounts and cancelled confirmation never recharge',async()=>{
 const {state,payments}=fixture({memberByPhone:async()=>member},false)
 assert.equal(state.member,null);state.form.amount='100';await state.submit();assert.equal(payments.length,0)
 await state.searchMember();state.form.amount='-1';await state.submit();assert.equal(payments.length,0)
 state.form.amount='100';await state.submit();assert.equal(payments.length,0)
})
test('verified phone charges exact member once with separate principal and gift',async()=>{
 const {state,payments,messages}=fixture({memberByPhone:async()=>member})
 await state.searchMember();state.form={amount:'100.50',giftAmount:'10',remark:'测试'}
 await Promise.all([state.submit(),state.submit()]);await state.submit()
 assert.deepEqual(payments,[{memberId:23,amount:100.5,giftAmount:10,remark:'测试'}])
 assert(messages.some(m=>m.includes(member.phone)&&m.includes(member.name)))
 assert.equal(state.completed,true)
})
test('a changed member mapping blocks recharge at submit time',async()=>{
 const {state,payments}=fixture({memberByPhone:async()=>({...member,id:99})})
 state.member=member;state.form.amount='100';await state.submit()
 assert.equal(payments.length,0);assert.equal(state.member,null)
})
