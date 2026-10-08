<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <span class="mark">八马</span>
        <div>
          <div class="t">门店管理后台</div>
          <div class="s">会员储值 · 扫码收款 · 门店运营</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="管理员账号 / 手机号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="onSubmit">登 录</el-button>
      </el-form>

      <el-button class="wechat-btn" :loading="wechatLoading" @click="wechat">微信扫码登录</el-button>
      <el-dialog v-model="qrVisible" title="微信扫码登录后台" width="360px" @closed="closeQr"><div class="qr-panel"><img v-if="qrImage && !qrExpired" :src="qrImage" alt="后台登录二维码" style="width:250px;height:250px"/><p>{{qrMessage}}</p><el-button v-if="qrExpired" :loading="wechatLoading" @click="wechat">刷新二维码</el-button><p class="tip">仅确认本人正在操作的电脑登录。二维码有效期 2 分钟。</p></div></el-dialog>
      <div class="tip">仅限已授权的门店员工登录，账号由管理员在「员工与权限」中录入</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const wechatLoading = ref(false)
const qrVisible=ref(false),qrImage=ref(''),qrMessage=ref(''),qrExpired=ref(false)
let qrSession=null,qrTimer=null,qrGeneration=0
function stopQr(){qrGeneration++;clearTimeout(qrTimer);const old=qrSession;qrSession=null;if(old)request.post('/wechat/desktop/cancel',old).catch(()=>{})}
function closeQr(){stopQr();wechatLoading.value=false}
onBeforeUnmount(stopQr)
async function wechat(){
 if(wechatLoading.value)return
 stopQr();const generation=qrGeneration
 qrVisible.value=true;wechatLoading.value=true;qrImage.value='';qrExpired.value=false;qrMessage.value='正在生成登录二维码…'
 try{
  const result=await request.post('/wechat/desktop/create')
  if(generation!==qrGeneration){request.post('/wechat/desktop/cancel',{ticket:result.ticket,secret:result.secret}).catch(()=>{});return}
  qrSession={ticket:result.ticket,secret:result.secret};qrImage.value=result.image;qrMessage.value='请用已绑定员工的微信扫码，并在手机上确认登录'
  const deadline=Date.now()+result.expiresIn*1000
  async function poll(){
   if(generation!==qrGeneration)return
   if(Date.now()>=deadline){qrExpired.value=true;qrMessage.value='二维码已过期，请刷新';stopQr();return}
   try{
    const state=await request.post('/wechat/desktop/poll',qrSession)
    if(generation!==qrGeneration)return
    if(state.status==='CONFIRMED'){qrSession=null;stopQr();qrVisible.value=false;userStore.acceptLogin(state.account);ElMessage.success('登录成功');router.replace('/dashboard');return}
    qrTimer=setTimeout(poll,2000)
   }catch(e){if(generation!==qrGeneration)return;qrExpired.value=true;qrMessage.value=e.message||'登录请求失效，请刷新';stopQr()}
  }
  qrTimer=setTimeout(poll,2000)
 }catch(e){if(generation===qrGeneration){qrExpired.value=true;qrMessage.value=e.message||'生成失败，请重试'}}
 finally{if(generation===qrGeneration)wechatLoading.value=false}
}
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入管理员账号或手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch (e) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #7d1a22 0%, #3f6b4b 100%); }
.login-box { width: 380px; background: #fff; border-radius: 12px; padding: 34px 32px 26px;
  box-shadow: 0 12px 40px rgba(0,0,0,.18); }
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.brand .mark { background: var(--bm-brand); color: #fff; border-radius: 6px;
  padding: 8px 10px; font-size: 16px; font-weight: 700; }
.brand .t { font-size: 18px; font-weight: 600; color: #1f2329; }
.brand .s { font-size: 12px; color: #8a9099; margin-top: 3px; }
.btn { width: 100%; margin-top: 4px; }
.tip { font-size: 12px; color: #a4a9b0; margin-top: 18px; line-height: 1.6; text-align: center; }
.wechat-btn{width:100%;margin:16px 0 0!important;color:#368352;border-color:#bad4c1;background:#f4faf5}</style>
