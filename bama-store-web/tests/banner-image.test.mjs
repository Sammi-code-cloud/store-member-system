import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'
const line=readFileSync(new URL('../src/views/banners.vue',import.meta.url),'utf8').split('\n').find(l=>l.startsWith('const imageUrl='))
test('Banner list and edit preview use the same API base as requests',()=>{for(const base of ['/api','/bama/api','/bama/api/','https://api.example.com/api']){const resolve=new Function('request',line+';return imageUrl')({defaults:{baseURL:base}});assert.equal(resolve('/api/banner-images/12?v=7'),base.replace(/\/$/,'')+'/banner-images/12?v=7')}})
