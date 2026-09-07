import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
// Run after HBuilderX has compiled the WeChat development target.
const compiled=readFileSync(new URL('../unpackage/dist/dev/mp-weixin/pages/customer/rooms.js',import.meta.url),'utf8')
const renderSource=compiled.slice(compiled.indexOf('function _sfc_render('),compiled.indexOf('const MiniProgramPage'))
const vendor={e:Object.assign,t:String,f:(list,fn)=>list.map(fn),o:fn=>fn,p:props=>props,m:fn=>fn}
const render=new Function('common_vendor',renderSource+';return _sfc_render')(vendor)
const source=readFileSync(new URL('../pages/customer/rooms.vue',import.meta.url),'utf8').match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm,'').replace('export default','return')
const component=new Function('api','CustomerNav',source)({}, {})
for(const [label,loading,slots] of [['loading',true,undefined],['failed',false,undefined],['empty',false,[]],['occupied',false,[{start:'14:00',end:'15:00',type:'PENDING'}]]]){
 test('native WeChat render handles '+label+' availability',()=>{
 const data=component.data();data.rooms=[{id:1,name:'茶室',priceHour:100}];data.slotsLoading=loading;if(slots!==undefined)data.occupied[1]=slots
 const methods={};for(const [k,f] of Object.entries(component.methods))methods[k]=f.bind(data)
 const output=render({},[],{}, {},data,methods)
 assert.equal(output.k[0].m,slots?.length||0)
 if(slots?.length)assert.equal(output.k[0].n[0].c,'待确认占用')
 })
}
