import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const apiSource=readFileSync(new URL('../common/api.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace('export default','return')
test('customer store API hides paused and deleted stores',async()=>{
  const api=new Function('request',apiSource)(async()=>[{id:1,status:0},{id:2,status:1},{id:3,status:1,deleted:1}])
  assert.deepEqual((await api.customerStores()).map(s=>s.id),[2])
})
test('returning to booking page refreshes stores and clears stale rooms when all close',async()=>{
  const source=readFileSync(new URL('../pages/customer/rooms.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
  const storage=new Map([['customer_store_id',1]])
  const uni={getStorageSync:k=>storage.get(k),removeStorageSync:k=>storage.delete(k)}
  const component=new Function('api','CustomerNav','uni',source)({customerStores:async()=>[]},{},uni)
  const state=component.data();state.rooms=[{id:10}];state.stores=[{id:1}];state.booking={roomId:10}
  Object.assign(state,component.methods)
  await state.initStores()
  assert.deepEqual(state.stores,[]);assert.deepEqual(state.rooms,[]);assert.equal(state.booking,null)
  assert.equal(storage.has('customer_store_id'),false)
  let refreshes=0
  component.onShow.call({loadContact(){},initStores(){refreshes++}})
  assert.equal(refreshes,1)
})
