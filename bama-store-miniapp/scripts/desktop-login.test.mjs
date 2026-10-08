import { evaluateSource } from './privacy-test-runtime.mjs'
import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const text=readFileSync(new URL('../pages/staff/desktop-login.vue',import.meta.url),'utf8')
const source=text.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
function fixture(fail=false){const calls=[];let count=0;const c=evaluateSource('request','uni',source)(async o=>{calls.push(o);if(fail)throw Error('未绑定员工')},{login:o=>{count++;o.success({code:'fresh'})},removeStorageSync(){},redirectTo(){}});const state=Object.assign(c.data(),c.methods);c.onLoad.call(state,{ticket:'A'.repeat(22)});return{state,calls,count:()=>count}}
test('desktop login requires explicit confirmation and uses fresh wx code only',async()=>{const f=fixture();assert.equal(f.calls.length,0);await Promise.all([f.state.confirm(),f.state.confirm()]);assert.equal(f.calls.length,1);assert.equal(f.count(),1);assert.deepEqual(f.calls[0].data,{ticket:'A'.repeat(22),code:'fresh'});assert.equal(f.state.done,true)})
test('unbound staff rejection never shows login confirmed',async()=>{const f=fixture(true);await f.state.confirm();assert.equal(f.state.done,false);assert.equal(f.state.loading,false);assert.equal(f.state.error,'未绑定员工')})
