import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const source = readFileSync(new URL('../src/views/staff.vue', import.meta.url), 'utf8').split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
function fixture(failCode = false) {
  const events = []
  const api = {
    async staffCreate() {events.push('create'); return 42},
    async staffUpdate() {events.push('update')},
    async staffWechatCode(id) {events.push(['code',id]); if(failCode) throw new Error('微信尚未配置'); return {image:'data:image/png;base64,test'}},
    async staffPage() {return {records:[],total:0}}
  }
  const user = {storeId:1, async loadStores(){},has(){return true}}
  const flow = new Function('ref','reactive','computed','onMounted','useUserStore','api','ElMessage',source+';return {form,onSave,bindVisible,bindImage,bindError,dialog,saving}')(
    value=>({value}), value=>value, getter=>({get value(){return getter()}}), ()=>{}, ()=>user, api, {success(){},warning(message){throw new Error(message)}})
  Object.assign(flow.form,{name:'员工',phone:'13800000001',password:'password',roleIds:[1],storeIds:[1],homeStoreId:1})
  flow.dialog.value = true
  return {flow,events}
}
test('new employee opens target QR automatically after successful creation',async()=>{
  const {flow,events}=fixture()
  await flow.onSave()
  assert.deepEqual(events,['create',['code',42]])
  assert.equal(flow.bindVisible.value,true)
  assert.match(flow.bindImage.value,/^data:image/)
  assert.equal(flow.dialog.value,false)
})
test('QR failure preserves successful creation and displays retryable error',async()=>{
  const {flow,events}=fixture(true)
  await flow.onSave()
  assert.deepEqual(events,['create',['code',42]])
  assert.equal(flow.bindVisible.value,true)
  assert.equal(flow.bindError.value,'微信尚未配置')
  assert.equal(flow.saving.value,false)
  assert.equal(flow.dialog.value,false)
})
test('editing an employee does not generate a new QR automatically',async()=>{
  const {flow,events}=fixture()
  flow.form.id=42
  await flow.onSave()
  assert.deepEqual(events,['update'])
  assert.equal(flow.bindVisible.value,false)
})
