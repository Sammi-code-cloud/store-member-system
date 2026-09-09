import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../src/views/member.vue',import.meta.url),'utf8').split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm,'')
function fixture(fail=false){const events=[];const flow=new Function('ref','reactive','onMounted','useUserStore','useRouter','api','ElMessage',source+';return {newForm,createMember,showCode,creating,creatingBusy,codeDialog,bindingCode,codeError,codeMember}')(
 value=>({value}),v=>v,()=>{},()=>({has:()=>true}),()=>({}),{memberCreate:async p=>{events.push('create');return{id:42,...p}},memberPage:async()=>({records:[],total:0}),memberWechatCode:async id=>{events.push(['code',id]);if(fail)throw new Error('生成失败');return{image:'data:image/png;base64,test'}}},{success(){},warning(){}});Object.assign(flow.newForm,{name:'会员',phone:'13800000003'});flow.creating.value=true;return{flow,events}}
test('new member opens a QR for the created member only',async()=>{const f=fixture();await Promise.all([f.flow.createMember(),f.flow.createMember()]);assert.deepEqual(f.events,['create',['code',42]]);assert.equal(f.flow.codeDialog.value,true);assert.equal(f.flow.creating.value,false)})
test('failed QR can retry without duplicating the created member',async()=>{const f=fixture(true);await f.flow.createMember();assert.equal(f.flow.creating.value,false);assert.equal(f.flow.codeError.value,'生成失败');await f.flow.showCode(f.flow.codeMember.value);assert.deepEqual(f.events,['create',['code',42],['code',42]])})
