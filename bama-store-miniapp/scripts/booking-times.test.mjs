import {test} from 'node:test'
import assert from 'node:assert/strict'
import {endOptions} from '../common/booking-times.mjs'
const room={minHours:0.5,closeTime:'22:00'}
test('allows half-hour, one-hour and one-and-a-half-hour bookings',()=>{
 assert.deepEqual(endOptions(room,{},'10:00').slice(0,3).map(o=>[o.time,o.hours]),[['10:30',0.5],['11:00',1],['11:30',1.5]])
})
test('honors room minimum and the earlier branch closing time',()=>{
 assert.deepEqual(endOptions({...room,minHours:1},{closeTime:'11:30'},'10:00').map(o=>o.hours),[1,1.5])
})
test('cannot extend across bookings or temporary closures; ending at the boundary is valid',()=>{
 assert.deepEqual(endOptions(room,{},'10:00',[{start:'11:00',end:'12:00'}]).map(o=>o.time),['10:30','11:00'])
 assert.deepEqual(endOptions(room,{},'11:30',[{start:'11:00',end:'12:00'}]),[])
 assert.equal(endOptions(room,{},'12:00',[{start:'11:00',end:'12:00'}])[0].time,'12:30')
})
test('does not offer durations shorter than the minimum near closing',()=>{
 assert.deepEqual(endOptions({...room,minHours:1}, {}, '21:30'),[])
 assert.deepEqual(endOptions(room,{},''),[])
})
