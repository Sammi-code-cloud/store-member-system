import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const source=readFileSync(new URL('../common/store.js',import.meta.url),'utf8').replaceAll('export ', '')
test('staff login is preferred, explicit customer switch persists, logout removes preference',()=>{
 const store=new Map();let route='';const uni={getStorageSync:k=>store.get(k),setStorageSync:(k,v)=>store.set(k,v),removeStorageSync:k=>store.delete(k),reLaunch:({url})=>route=url}
 const auth=new Function('uni',source+';return auth')(uni)
 assert.equal(auth.preferStaff(),false)
 store.set('customer_token','customer-session')
 auth.setLogin({token:'staff-session',staffId:1,name:'员工'})
 assert.equal(auth.preferStaff(),true)
 auth.switchToCustomer();assert.equal(auth.preferStaff(),false);assert.equal(route,'/pages/customer/home');assert.equal(store.get('token'),'staff-session')
 const reopened=new Function('uni',source+';return auth')(uni);assert.equal(reopened.preferStaff(),false)
 auth.switchToStaff();assert.equal(route,'/pages/staff/workbench');assert.equal(auth.preferStaff(),true)
 auth.logout();assert.equal(auth.preferStaff(),false);assert.equal(store.get('customer_token'),'customer-session')
 auth.switchToStaff();assert.equal(route,'/pages/staff/login');assert.equal(auth.preferStaff(),false)
})
test('existing employee session without a saved choice defaults to staff',()=>{
 const uni={getStorageSync:k=>k==='token'?'existing-session':undefined};const auth=new Function('uni',source+';return auth')(uni);assert.equal(auth.preferStaff(),true)
})
