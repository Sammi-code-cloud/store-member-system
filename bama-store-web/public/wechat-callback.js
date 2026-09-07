(async()=>{
 const message=document.getElementById('message'),form=document.getElementById('binding');let ticket='';
 document.getElementById('close').onclick=()=>window.close();
 async function api(path,body){const r=await fetch('/api/wechat/'+path,{method:body?'POST':'GET',credentials:'same-origin',headers:{'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});const result=await r.json();if(result.code!==200)throw new Error(result.message||'微信登录失败');return result.data}
 function finish(result){if(!window.opener)throw new Error('原登录窗口已关闭，请重新发起登录');if(result.bindRequired){ticket=result.bindTicket;form.hidden=false;message.textContent='请绑定您的员工账号';return}window.opener.postMessage({type:'bama-wechat-login',result},location.origin);message.textContent='登录成功，正在返回…';window.close()}
 form.onsubmit=async e=>{e.preventDefault();const button=form.querySelector('button');button.disabled=true;try{finish(await api('bind',{ticket,phone:form.elements.phone.value,password:form.elements.password.value}))}catch(error){message.textContent=error.message+'；请关闭窗口后重新发起微信登录。';form.hidden=true}finally{form.elements.password.value='';button.disabled=false}};
 try{const params=new URLSearchParams(location.search);const code=params.get('code'),state=params.get('state');history.replaceState(null,'',location.pathname);
  if(code&&state)finish(await api('exchange',{code,state}));
  else if(state)throw new Error('微信授权未完成，请重新登录');
  else{const audience=params.get('audience');if(!['STAFF','CUSTOMER'].includes(audience))throw new Error('请从登录页点击微信登录');const result=await api('start?audience='+audience);location.replace(result.url)}
 }catch(e){message.textContent=e.message||'微信登录失败，请重新发起';if(window.opener){window.opener.postMessage({type:'bama-wechat-login',error:message.textContent},location.origin);window.close()}}
})();
