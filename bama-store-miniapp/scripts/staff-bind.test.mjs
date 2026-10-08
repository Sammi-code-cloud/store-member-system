import { withConsentRuntime } from './privacy-test-runtime.mjs'
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseStaffScene, bindStaffWechat as originalbindStaffWechat } from '../common/staff-bind.mjs'
const ticket='abcdefghijklmnopqrstuv'

test('only valid staff scenes are accepted', () => {
  assert.equal(parseStaffScene('b%3D'+ticket), ticket)
  for (const scene of ['s=12', 'e=0', 'e=-1', 'e=1&admin=1', '%', 'e=9007199254740992', undefined]) assert.equal(parseStaffScene(scene), null)
})
test('binding sends a fresh WeChat code with invitation and phone, without password', async () => {
  const calls = []
  let n = 0
  const runtime = {login(options) {options.success({code: `code-${++n}`})}}
  const request = async data => { calls.push(data); return {account: {staffId: 12}} }
  for (let i=0;i<2;i++) assert.equal((await bindStaffWechat(ticket,'13800000001',runtime,request)).account.staffId,12)
  assert.equal(calls[0].url,'/api/wechat/staff/bind')
  assert.deepEqual(calls[0].data,{ticket,phone:'13800000001',code:'code-1'})
  assert.equal(calls[1].data.code,'code-2')
})
test('invalid inputs and failed WeChat authorization never submit binding', async () => {
  const request = async () => assert.fail('must not submit')
  await assert.rejects(bindStaffWechat(null,'13800000001',{},request),/无效/)
  await assert.rejects(bindStaffWechat(ticket,'bad',{},request),/手机号/)
  await assert.rejects(bindStaffWechat(ticket,'13800000001',{login(o){o.fail()}},request),/授权失败/)
})

function bindStaffWechat(ticket,phone,runtime,request){return originalbindStaffWechat(ticket,phone,withConsentRuntime(runtime),request)}
