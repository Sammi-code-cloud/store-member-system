import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../pages/staff/charge.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
const member={memberId:23,name:'测试会员',phone:'13800000003',lookupPhone:'13800000003',discount:80,balance:100}
function fixture({confirm=true,charge,lookup}={}){
 const payments=[],messages=[],storage=new Map()
 const component=new Function('api','uni',source)({chargeMemberByPhone:lookup || (async()=>member),chargeConfirm:async p=>{payments.push({...p});if(charge)return charge(p);return{payAmount:p.amount,balanceAfter:87.66,orderNo:'TEST'}}},{getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,v),removeStorageSync:k=>storage.delete(k),showModal:o=>{messages.push(o.content);o.success?.({confirm})},showToast:o=>messages.push(o.title),reLaunch(){}})
 const state=Object.assign(component.data(),component.methods,{member:{...member},amount:'12.34'})
 return{state,component,payments,messages,storage}
}
test('manual input is actual debit and repeated taps submit once',async()=>{const f=fixture();await Promise.all([f.state.confirm(),f.state.confirm()]);await f.state.confirm();assert.equal(f.payments.length,1);assert.equal(f.payments[0].amount,12.34);assert.equal(f.payments[0].items,undefined);assert.equal(f.payments[0].expectedPhone,member.phone);assert(f.messages.some(m=>m.includes(member.phone)&&m.includes('12.34')));assert.equal(f.storage.has('chargePending'),false)})
test('invalid amounts and cancelling confirmation never debit',async()=>{const f=fixture({confirm:false});for(const amount of ['-1','0','1.001','Infinity','100000000','']){f.state.amount=amount;await f.state.confirm()}f.state.amount='10';await f.state.confirm();assert.equal(f.payments.length,0)})
test('phone mapping change blocks payment',async()=>{const f=fixture({lookup:async()=>({...member,memberId:99})});await f.state.confirm();assert.equal(f.payments.length,0)})
test('unknown network outcome retains same payload across retries and page reload',async()=>{const f=fixture({charge:async()=>{throw new Error('network')}});await f.state.confirm();const first=f.payments[0];f.state.amount='999';await f.state.confirm();assert.deepEqual(f.payments[1],first);f.component.onLoad.call(f.state);assert.equal(f.state.amount,'12.34');assert.equal(f.state.pending.bizNo,first.bizNo)})
test('definitive insufficient balance clears pending so amount can be corrected',async()=>{const f=fixture({charge:async()=>{throw{code:1003,message:'余额不足'}}});await f.state.confirm();assert.equal(f.state.pending,null);assert.equal(f.storage.has('chargePending'),false)})
