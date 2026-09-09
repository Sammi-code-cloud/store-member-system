import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const storeSource=readFileSync(new URL('../common/store.js',import.meta.url),'utf8').replaceAll('export ', '')
const loadStaffEntry=new Function(storeSource+';return loadStaffEntry')()

test('guests do not request employee data and cached permission cannot reveal the entry',async()=>{
  let calls=0
  assert.equal(await loadStaffEntry({getToken:()=>''},async()=>{calls++}),null)
  assert.equal(calls,0)
  const auth={getToken:()=>'staff-token',can:()=>true}
  assert.equal(await loadStaffEntry(auth,async()=>({staffId:1,permissions:[]})),null)
  assert.equal(await loadStaffEntry(auth,async()=>({staffId:1,permissions:['member:view']})),null)
  assert.equal(await loadStaffEntry(auth,async()=>{throw {code:401}}),null)
})

test('current data permission permits entry, changed sessions discard old responses',async()=>{
  let token='first'
  const auth={getToken:()=>token}
  const staff={staffId:1,name:'店员',permissions:['dashboard:view']}
  assert.deepEqual(await loadStaffEntry(auth,async()=>staff),staff)
  assert.equal(await loadStaffEntry(auth,async()=>{token='second';return staff}),null)
})

const source=readFileSync(new URL('../pages/customer/home.vue',import.meta.url),'utf8')
const script=source.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return') + ';'
test('clicking the entry rechecks revoked permission and does not navigate',async()=>{
  let navigated=false,accepted=false
  const auth={getToken:()=>'token',setLogin:()=>{accepted=true}}
  const component=new Function('auth','api','CustomerNav','uni','loadStaffEntry', 'resolveWechatStaff',script)(auth,{staffSession:async()=>({staffId:1,permissions:[]})},{},{reLaunch:()=>{navigated=true}},loadStaffEntry)
  const state=Object.assign(component.data(),component.methods,{staffEntry:{name:'店员'}})
  await state.openStaff()
  assert.equal(state.staffEntry,null)
  assert.equal(navigated,false)
  assert.equal(accepted,false)
  assert.equal(state.staffOpening,false)
})

test('leaving homepage ignores an in-flight permission response',async()=>{
  let resolve
  const component=new Function('auth','api','CustomerNav','uni','loadStaffEntry', 'resolveWechatStaff',script)({getToken:()=>'token'},{staffSession:()=>new Promise(r=>{resolve=r})},{},{},loadStaffEntry)
  const state=Object.assign(component.data(),component.methods,{stopBanners(){}})
  const pending=state.refreshStaffEntry()
  component.onHide.call(state)
  resolve({staffId:1,permissions:['dashboard:view']})
  assert.equal(await pending,null)
  assert.equal(state.staffEntry,null)
})

test('authorized entry refreshes employee permissions before opening workbench',async()=>{
  let route,account
  const auth={getToken:()=>'token',setLogin:value=>{account=value}}
  const staff={staffId:1,name:'店员',permissions:['dashboard:view']}
  const component=new Function('auth','api','CustomerNav','uni','loadStaffEntry', 'resolveWechatStaff',script)(auth,{staffSession:async()=>staff},{},{reLaunch:({url})=>{route=url}},loadStaffEntry)
  const state=Object.assign(component.data(),component.methods,{staffEntry:staff})
  await state.openStaff()
  assert.equal(route,'/pages/staff/workbench')
  assert.deepEqual(account,{...staff,token:'token'})
})

test('background permission check expires staff session without redirecting a customer',async()=>{
  const requestSource=readFileSync(new URL('../common/request.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace('export const','const').replace('export default function request','return function request').replace('import.meta.env.VITE_API_BASE_URL',"'https://example.test'")
  const storage=new Map([['token','expired'],['staff',{staffId:1}],['customer_token','customer']])
  let options
  const uni={getStorageSync:k=>storage.get(k),removeStorageSync:k=>storage.delete(k),request:o=>{options=o},showToast:()=>{throw Error('Unexpected toast')},reLaunch:()=>{throw Error('Unexpected navigation')}}
  const request=new Function('uni','networkError',requestSource)(uni,e=>e)
  const pending=request({url:'/api/auth/me',silent:true})
  options.success({statusCode:200,data:{code:401}})
  await assert.rejects(pending)
  assert.equal(storage.has('token'),false)
  assert.equal(storage.has('staff'),false)
  assert.equal(storage.get('customer_token'),'customer')
})

test('bound WeChat employee is recognized after customer login without a previous employee token',async()=>{
  const storage=new Map([['customer_token','customer'],['app_identity','customer']])
  const uni={getStorageSync:k=>storage.get(k),setStorageSync:(k,v)=>storage.set(k,v)}
  const auth=new Function('uni',storeSource+';return auth')(uni)
  const employee={staffId:3,name:'员工',token:'staff-new',permissions:['dashboard:view']}
  const result=await loadStaffEntry(auth,()=>{throw Error('No token should not request me')},async()=>employee)
  assert.deepEqual(result,employee)
  assert.equal(storage.get('token'),'staff-new')
  assert.equal(storage.get('customer_token'),'customer')
  assert.equal(storage.get('app_identity'),'customer')
})

test('unbound WeChat, no data permission and stale page never expose workbench',async()=>{
  let token='',saved=0
  const auth={getToken:()=>token,saveStaffSession:s=>{token=s.token;saved++}}
  assert.equal(await loadStaffEntry(auth,null,async()=>null),null)
  assert.equal(saved,0)
  const employee={staffId:3,token:'staff-new',permissions:['dashboard:view']}
  assert.equal(await loadStaffEntry(auth,null,async()=>employee,()=>false),null)
  assert.equal(saved,0)
  assert.equal(await loadStaffEntry(auth,null,async()=>({...employee,permissions:[]})),null)
})

test('expired employee session can be recovered from current WeChat in the same refresh',async()=>{
  let token='expired'
  const auth={getToken:()=>token,saveStaffSession:s=>{token=s.token}}
  const employee={staffId:3,token:'fresh',permissions:['dashboard:view']}
  assert.deepEqual(await loadStaffEntry(auth,async()=>{token='';throw {code:401}},async()=>employee),employee)
})

test('WeChat lookup uses a fresh code, only accepts STAFF accounts, and never binds automatically',async()=>{
  const source=readFileSync(new URL('../common/wechat.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replaceAll('export ','')
  let response={audience:'STAFF',bindRequired:false,account:{staffId:3,token:'staff',permissions:['dashboard:view']}}
  const calls=[]
  const lookup=new Function('request','uni','wx',source+';return resolveWechatStaff')(async o=>{calls.push(o);return response},{login:({success})=>success({code:'fresh-code'})},{login(){}})
  assert.equal((await lookup()).staffId,3)
  assert.deepEqual(calls[0].data,{code:'fresh-code',audience:'STAFF'})
  assert.equal(calls[0].silent,true)
  response={audience:'STAFF',bindRequired:true,bindTicket:'ignored'}
  assert.equal(await lookup(),null)
  response={audience:'CUSTOMER',bindRequired:false,account:{token:'customer'}}
  assert.equal(await lookup(),null)
  assert(calls.every(c=>c.url==='/api/wechat/mini'))
})
