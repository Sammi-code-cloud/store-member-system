import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
import {customerShare, receiveSharedStore} from '../common/customer-share.mjs'

test('home and room shares round-trip only the public branch context',()=>{
  for(const page of ['home','rooms']) {
    const store={id:12,name:'测试分店',token:'secret',phone:'13800000000'}
    const share=customerShare(page,store)
    assert.equal(share.path,`/pages/customer/${page}?storeId=12`)
    assert.equal(share.query,'storeId=12')
    assert(!JSON.stringify(share).includes('secret'))
    assert(!JSON.stringify(share).includes('13800000000'))
    const writes=[]
    receiveSharedStore(Object.fromEntries(new URLSearchParams(share.query)),{setStorageSync:(...args)=>writes.push(args)})
    assert.deepEqual(writes,[['customer_store_id',12]])
  }
})

test('missing or malformed shared branches leave existing selection untouched',()=>{
  for(const id of [undefined,'','0','-1','1&token=secret','1.5','9007199254740992',{}]) {
    receiveSharedStore({storeId:id},{setStorageSync:()=>assert.fail('invalid branch persisted')})
    assert.equal(customerShare('rooms',{id}).query,'')
  }
})

test('page hooks share selected branch and receive it before loading page data',()=>{
  for(const page of ['home','rooms']) {
    const source=readFileSync(new URL(`../pages/customer/${page}.vue`,import.meta.url),'utf8')
      .match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
    const writes=[]
    const platform={setStorageSync:(...args)=>writes.push(args),getSystemInfoSync:()=>({statusBarHeight:20})}
    const component=new Function('customerShare','receiveSharedStore','paymentNotice','CustomerNav','uni',source)(customerShare,receiveSharedStore,{}, {},platform)
    const vm={stores:[{id:9,name:'分店'}],storeIndex:0,buildDates(){assert.deepEqual(writes,[['customer_store_id',9]])}}
    component.onLoad.call(vm,{storeId:'9',token:'do-not-copy'})
    assert.deepEqual(writes,[['customer_store_id',9]])
    assert.equal(component.onShareAppMessage.call(vm).path,`/pages/customer/${page}?storeId=9`)
    assert.equal(component.onShareTimeline.call(vm).query,'storeId=9')
    assert.equal(component.onShareTimeline.call(vm).path,undefined)
  }
})
