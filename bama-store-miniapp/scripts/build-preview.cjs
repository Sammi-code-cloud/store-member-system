// Browser design preview compiled from the real customer Vue pages.
// This adapter is for local visual QA, not a WeChat production build.
const fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'..'), web=path.resolve(root,'../bama-store-web');
const compiler=require(path.join(web,'node_modules/@vue/compiler-sfc'));
const esbuild=require(path.join(web,'node_modules/esbuild'));
const out=path.join(web,'dist/miniapp');fs.mkdirSync(out,{recursive:true});
const variables=Object.fromEntries([...fs.readFileSync(path.join(root,'uni.scss'),'utf8').matchAll(/\$([\w-]+):\s*([^;]+);/g)].map(m=>[m[1],m[2]]));
let css='';let serial=0;
function style(source){return source.replace(/\$([\w-]+)/g,(_,n)=>variables[n]).replace(/(-?[\d.]+)rpx/g,(_,n)=>`calc(var(--rpx) * ${n})`).replace(/\bview\b/g,'div').replace(/\btext(?=[\s.{:#>])/g,'span').replace(/(?<![.\w-])page\s*\{/g,'body{')}
const app=compiler.parse(fs.readFileSync(path.join(root,'App.vue'),'utf8')).descriptor;
css+=app.styles.map(s=>style(s.content)).join('\n');
const entry=`import {createApp,ref,h} from 'vue';
import Home from './pages/customer/home.vue';import Rooms from './pages/customer/rooms.vue';import Profile from './pages/customer/profile.vue';import Login from './pages/customer/login.vue';import Paycode from './pages/customer/paycode.vue';
import StaffProfile from './pages/staff/profile.vue';import StaffLogin from './pages/staff/login.vue';import Workbench from './pages/staff/workbench.vue';import Scan from './pages/staff/scan.vue';import Charge from './pages/staff/charge.vue';import Recharge from './pages/staff/recharge.vue';import Dashboard from './pages/staff/dashboard.vue';
const pages={home:Home,rooms:Rooms,profile:Profile,login:Login,paycode:Paycode,'staff-profile':StaffProfile,'staff-login':StaffLogin,'staff-workbench':Workbench,'staff-scan':Scan,'staff-charge':Charge,'staff-recharge':Recharge,'staff-dashboard':Dashboard};const current=ref(location.hash.slice(1)||'home');
const navigate=({url})=>{location.hash=(url.startsWith('/pages/staff/')?'staff-':'')+url.split('/').pop();};window.addEventListener('hashchange',()=>{current.value=location.hash.slice(1)||'home';window.scrollTo(0,0)});
window.uni={getStorageSync:key=>{try{return JSON.parse(localStorage.getItem('mini_preview_'+key))}catch{return null}},setStorageSync:(k,v)=>localStorage.setItem('mini_preview_'+k,JSON.stringify(v)),removeStorageSync:k=>localStorage.removeItem('mini_preview_'+k),getSystemInfoSync:()=>({statusBarHeight:16}),scanCode:({fail})=>{window.uni.showToast({title:"浏览器预览请使用手动输入付款码"});fail?.({errMsg:"scanCode:fail browser preview"})},navigateTo:navigate,redirectTo:navigate,reLaunch:navigate,showToast:({title})=>{const el=document.getElementById('toast');el.textContent=title;el.hidden=false;setTimeout(()=>el.hidden=true,2500)},showModal:({content,success})=>success({confirm:confirm(content)}),request:async o=>{try{const url=new URL(o.url);const r=await fetch(url.pathname+url.search,{method:o.method,headers:o.header,body:o.method==='GET'?undefined:JSON.stringify(o.data)});o.success({statusCode:r.status,data:await r.json()})}catch(e){o.fail(e)}}};
const app=createApp({setup:()=>()=>h(pages[current.value.split('?')[0]]||Home,{key:current.value})});
app.component('Picker',{props:['range','rangeKey','value','disabled'],emits:['change'],setup:(p,{emit,slots})=>()=>h('div',{class:'preview-picker'},[slots.default?.(),h('select',{value:p.value,disabled:p.disabled,'aria-label':p.rangeKey==='label'?'结束时间':p.rangeKey==='time'?'开始时间':'选择分店',onChange:e=>emit('change',{detail:{value:e.target.value}})},(p.range||[]).map((s,i)=>h('option',{value:i},s[p.rangeKey])))])});
app.mixin({mounted(){this.$options.onLoad?.call(this,Object.fromEntries(new URLSearchParams(location.hash.split('?')[1]||'')));this.$options.onShow?.call(this)},beforeUnmount(){this.$options.onHide?.call(this);this.$options.onUnload?.call(this)}});app.mount('#app');`;
esbuild.build({stdin:{contents:entry,resolveDir:root,sourcefile:'preview-entry.js'},bundle:true,outfile:path.join(out,'app.js'),alias:{'@':root,'vue':path.join(web,'node_modules/vue/dist/vue.esm-bundler.js')},define:{__VUE_OPTIONS_API__:'true',__VUE_PROD_DEVTOOLS__:'false',__VUE_PROD_HYDRATION_MISMATCH_DETAILS__:'false','process.env.NODE_ENV':'"production"'},plugins:[{name:'uni-vue-preview',setup(build){build.onLoad({filter:/\.vue$/},args=>{
 const {descriptor:d,errors}=compiler.parse(fs.readFileSync(args.path,'utf8'),{filename:args.path});if(errors.length)throw errors[0];
 compiler.compileScript(d,{id:args.path});
 const native=compiler.compileTemplate({source:d.template.content,filename:args.path,id:args.path});if(native.errors.length)throw native.errors[0];
 const id='data-v-mini-'+serial++;
 const template=d.template.content.replace(/@tap/g,'@click').replace(/<(\/?)(view|scroll-view)\b/g,'<$1div').replace(/<(\/?)text\b/g,'<$1span').replace(/<(\/?)image\b/g,'<$1img').replace(/<(\/?)picker\b/g,'<$1Picker').replace(/\spassword(?=\s)/g,' type="password"');
 const result=compiler.compileTemplate({source:template,filename:args.path,id,scoped:true});if(result.errors.length)throw result.errors[0];
 for(const sheet of d.styles){const parsed=compiler.compileStyle({source:style(sheet.content),filename:args.path,id,scoped:sheet.scoped});if(parsed.errors.length)throw parsed.errors[0];css+=parsed.code+'\n'}
 const script=compiler.rewriteDefault(d.script?.content||'export default {}','component');
 return{contents:script+'\n'+result.code+'\ncomponent.render=render;component.__scopeId="'+id+'";export default component;',loader:'js',resolveDir:path.dirname(args.path)};
})}}]}).then(()=>{
 css+='\n:root{--rpx:min(.133333vw,.573333px)}*{box-sizing:border-box}body{margin:0;background:#f0e3d6}#app{max-width:430px;min-height:100vh;margin:auto;background:#fff7ef}button,input{font:inherit}button{cursor:pointer;border:0;width:100%;line-height:2.8}input{display:block;width:100%;outline:none}span{box-sizing:border-box}img{object-fit:cover}.customer-nav{max-width:430px;margin:auto}.preview-picker{position:relative}.preview-picker select{position:absolute;inset:0;opacity:0;width:100%;height:100%;cursor:pointer}.datechips{overflow-x:auto}.datechips::-webkit-scrollbar{display:none}#toast{position:fixed;z-index:99;bottom:110px;left:50%;transform:translateX(-50%);background:#8e2825e8;color:white;border-radius:12px;padding:14px 22px;max-width:90%;font-size:14px}';
 fs.writeFileSync(path.join(out,'style.css'),css);
 fs.writeFileSync(path.join(out,'index.html'),'<!doctype html><html lang="zh-CN"><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>八马茶业 · 小程序样式预览</title><link rel="stylesheet" href="style.css"><body><div id="app"></div><div id="toast" hidden></div><script src="app.js"></script></body></html>');
 console.log('Compiled customer and staff pages with separate navigation. Preview: http://127.0.0.1:5175/miniapp/index.html');
}).catch(e=>{console.error(e);process.exit(1)});
