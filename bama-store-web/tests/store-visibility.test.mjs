import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../src/store/user.js',import.meta.url),'utf8').replace(/^import .*$/gm,'').replace('export const useUserStore =','return')
test('global store selector hides inactive stores and never falls back to a paused home store',async()=>{
  let rows=[{id:1,status:0},{id:2,status:1},{id:3,status:1,deleted:1}]
  const storage=new Map([['bm_active_store','1']])
  const localStorage={getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)}
  const definition=new Function('defineStore','api','localStorage',source)((_,d)=>d,{storeList:async()=>rows},localStorage)
  const state=definition.state();state.user={storeId:1}
  for(const [name,fn] of Object.entries(definition.actions))state[name]=fn.bind(state)
  await state.loadStores()
  assert.deepEqual(state.stores.map(s=>s.id),[2])
  assert.equal(definition.getters.storeId(state),2)
  rows=[{id:1,status:0}]
  await state.loadStores()
  assert.equal(definition.getters.storeId(state),null)
  assert.equal(storage.has('bm_active_store'),false)
})
