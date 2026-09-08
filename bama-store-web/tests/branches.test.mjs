import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source=readFileSync(new URL('../src/views/branches.vue',import.meta.url),'utf8').split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm,'')
function fixture({cancel=false,reject=false,active=2}={}) {
  const events=[]
  const user={storeId:active,selectStore(id){events.push(['select',id]);this.storeId=id},async loadStores(){events.push(['reload',this.storeId])}}
  const api={async storeDelete(id){events.push(['delete',id]);if(reject)throw new Error('有关联数据')},async storeStatus(id,status){events.push(['status',id,status])}}
  const router={async push(options){events.push(['route',options.path,user.storeId])}}
  const flow=new Function('ref','useUserStore','useRouter','api','ElMessageBox','ElMessage',source+';return {remove,setStatus,enter,busyId}')(value=>({value}),()=>user,()=>router,api,{async confirm(){if(cancel)throw new Error('cancel')}},{success:()=>{},warning:()=>{}})
  return {flow,events}
}
test('cancelled confirmation and active branches never delete',async()=>{
  for(const options of [{cancel:true},{}]){const {flow,events}=fixture(options);await flow.remove({id:2,name:'分店',status:options.cancel?0:1});assert.deepEqual(events,[])}
})
test('deleting current branch clears selection before reloading permissions',async()=>{
  const {flow,events}=fixture();await flow.remove({id:2,name:'分店',status:0})
  assert.deepEqual(events,[['delete',2],['select',null],['reload',null]])
  assert.equal(flow.busyId.value,null)
})
test('server rejection keeps selected branch and releases busy state',async()=>{
  const {flow,events}=fixture({reject:true});await assert.rejects(flow.remove({id:2,name:'分店',status:0}))
  assert.deepEqual(events,[['delete',2]]);assert.equal(flow.busyId.value,null)
})
test('status toggle updates only status and branch navigation selects before routing',async()=>{
  const {flow,events}=fixture();await flow.setStatus({id:2,name:'分店',status:1});await flow.setStatus({id:2,name:'分店',status:0});await flow.enter({id:3},'/store')
  assert.deepEqual(events,[['status',2,0],['reload',2],['status',2,1],['reload',2],['select',3],['route','/store',3]])
})
