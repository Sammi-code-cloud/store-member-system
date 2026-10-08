import { evaluateSource } from './privacy-test-runtime.mjs'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parseStoreScene, rememberScan } from '../common/scan-entry.mjs'

test('only valid public store scenes are accepted, never URLs, tokens or staff identity', () => {
  assert.equal(parseStoreScene('s=23'), 23)
  assert.equal(parseStoreScene('s%3D23'), 23)
  for (const value of ['', null, 's=0', 's=-1', 's=01', 's=1&audience=STAFF', 'token=secret', 'https://example.com', '%ZZ', 's=9007199254740992']) assert.equal(parseStoreScene(value), null)
})

test('cold and warm scans retain the newest scene, including invalid codes', () => {
  const data = new Map(), storage = { setStorageSync: (k,v) => data.set(k,v) }
  rememberScan({path:'pages/customer/entry',query:{scene:'s=2'}}, storage)
  assert.deepEqual(data.get('pending_store_scene'), {scene:'s=2'})
  rememberScan({path:'pages/customer/home',query:{scene:'s=9'}}, storage)
  assert.deepEqual(data.get('pending_store_scene'), {scene:'s=2'})
  rememberScan({path:'pages/customer/entry',query:{scene:'s=3'}}, storage)
  assert.deepEqual(data.get('pending_store_scene'), {scene:'s=3'})
  rememberScan({path:'pages/customer/entry',query:{}}, storage)
  assert.deepEqual(data.get('pending_store_scene'), {scene:''})
})

const source = readFileSync(new URL('../pages/customer/entry.vue', import.meta.url), 'utf8').split('<script>')[1].split('</script>')[0].replace(/^import .*$/gm,'').replace('export default', 'return')
function fixture(stores = [{id:2,name:'测试分店',status:1}], binding = false) {
  const data = new Map(), routes = []
  const uni = {getStorageSync:k=>data.get(k),setStorageSync:(k,v)=>data.set(k,v),removeStorageSync:k=>data.delete(k),reLaunch:({url})=>routes.push(url),navigateTo:({url})=>routes.push(url)}
  const api = {customerStores:async()=>stores,customerHome:async()=>({})}
  const auth = {setCustomerLogin:account=>{data.set('customer_token',account.token);data.set('app_identity','customer')}}
  const flowSource=readFileSync(new URL('../common/customer-wechat.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace(/export /g,'')
  const complete=evaluateSource('uni','auth',flowSource+';return completeCustomerWechat')(uni,auth)
  const result=binding?{audience:'CUSTOMER',bindRequired:true,bindTicket:'pending-ticket',smsEnabled:true}:{audience:'CUSTOMER',account:{token:'customer-token',memberId:3}}
  const component = evaluateSource('uni','api','auth','wechatLogin','parseStoreScene','completeCustomerWechat',source)(uni,api,auth,async()=>result,parseStoreScene,complete)
  const vm = component.data()
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm)
  vm.scene='s=2'
  return {vm,data,routes}
}
test('scan selects the branch without logging in silently, explicit WeChat login enters the miniapp', async()=>{
  const {vm,data,routes}=fixture()
  data.set('app_identity','staff');data.set('token','existing-staff-session')
  await vm.loadStore()
  assert.equal(data.get('customer_store_id'),2); assert.equal(data.get('app_identity'),'customer')
  assert.equal(data.get('customer_token'),undefined);assert.equal(routes.length,0)
  await vm.login()
  assert.equal(data.get('customer_token'),'customer-token');assert.equal(data.get('token'),'existing-staff-session')
  assert.deepEqual(routes,['/pages/customer/home'])
})
test('disabled or missing store codes cannot overwrite the selected store or log in', async()=>{
  for (const stores of [[],[{id:2,status:0}]]) {
    const {vm,data,routes}=fixture(stores); data.set('customer_store_id',1)
    await vm.loadStore();await vm.login()
    assert.match(vm.error,/暂未营业/);assert.equal(data.get('customer_store_id'),1);assert.equal(routes.length,0)
  }
})
test('an existing customer session is verified before entering the scanned branch', async()=>{
  const {vm,data,routes}=fixture(); data.set('customer_token','valid');data.set('customer_user',{memberId:3})
  await vm.loadStore();assert.deepEqual(routes,['/pages/customer/home']);assert.equal(data.get('customer_store_id'),2)
})

test('first scan login retains branch and sends unbound customer to phone verification without a token',async()=>{
  const {vm,data,routes}=fixture(undefined,true)
  await vm.loadStore();await vm.login()
  assert.equal(data.get('customer_store_id'),2)
  assert.equal(data.get('customer_token'),undefined)
  assert.deepEqual(routes,['/pages/customer/bind-phone'])
})
