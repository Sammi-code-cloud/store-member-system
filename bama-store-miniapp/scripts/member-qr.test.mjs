import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../common/wechat.js',import.meta.url),'utf8')
const parse=new Function(source.slice(source.indexOf('export function parseMemberScene'),source.indexOf('export async function bindMemberWechat')).replace('export function','return function'))()
test('member QR scene requires its own prefix and complete random ticket',()=>{assert.equal(parse('m='+'a'.repeat(22)),'a'.repeat(22));assert.equal(parse('b='+'a'.repeat(22)),null);assert.equal(parse('m=123'),null);assert.equal(parse('%ZZ'),null)})
test('member binding passes invitation and fresh WeChat code, never member ID',async()=>{const calls=[];const bind=new Function('request','uni',source.slice(source.indexOf('export async function bindMemberWechat')).replace('export async function','return async function'))(async p=>calls.push(p),{login:o=>o.success({code:'fresh'})});await assert.rejects(bind('bad','13800000003'));await bind('a'.repeat(22),'13800000003');assert.deepEqual(calls[0].data,{ticket:'a'.repeat(22),phone:'13800000003',code:'fresh'});assert.equal(calls[0].url,'/api/wechat/customer/member-bind')})
