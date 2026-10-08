<template>
 <view class="confirm-page"><view class="seal">八马</view><text class="title">登录门店管理后台</text><text class="note">电脑正在请求登录。确认后，将使用当前微信绑定的员工账号登录该电脑。</text><view class="notice">请确认这是你本人打开的后台页面。不要确认他人发送的登录二维码。</view><PrivacyConsent ref="privacyConsent" @change="privacyFormAllowed=$event" /><text v-if="error" class="error">{{error}}</text><button class="confirm" :loading="loading" :disabled="loading || done || !ticket" @tap="confirm">{{done?'已确认，请返回电脑':'确认登录'}}</button><button v-if="!done" :disabled="loading" @tap="cancel">取消</button></view>
</template>
<script>
import PrivacyConsent from '@/components/PrivacyConsent.vue'
import { privacyLogin } from '@/common/privacy-consent.mjs'
import request from '@/common/request.js'
export default {
  components: { PrivacyConsent },
 data(){return{privacyFormAllowed:false,ticket:'',loading:false,done:false,error:''}},
 onLoad(options){uni.removeStorageSync('pending_desktop_scene');if(/^[A-Za-z0-9_-]{22}$/.test(options?.ticket||''))this.ticket=options.ticket;else this.error='登录码无效，请在电脑上重新生成'},
 methods:{
  async confirm(){if(this.loading||this.done||!this.ticket)return;this.loading=true;if(!await this.$refs.privacyConsent.ensure()){this.loading=false;return};this.error='';try{const login=await privacyLogin(uni);if(!login.code)throw new Error('微信登录失败，请重试');await request({url:'/api/wechat/desktop/confirm',method:'POST',data:{ticket:this.ticket,code:login.code}});this.done=true}catch(e){this.error=e.message||'确认失败，请重试'}finally{this.loading=false}},
  cancel(){uni.redirectTo({url:'/pages/customer/home'})}
 }
}
</script>
<style scoped>
.confirm-page{padding:80rpx 44rpx;min-height:100vh;background:#fff7ef}.seal{width:96rpx;height:96rpx;border-radius:20rpx;background:#b5362d;color:#fff;display:flex;align-items:center;justify-content:center;font-family:serif;font-size:32rpx;margin-bottom:40rpx}.title{display:block;font-size:40rpx;font-weight:600}.note{display:block;font-size:27rpx;line-height:1.9;color:#7d6c5d;margin:26rpx 0}.notice{padding:26rpx;background:#f0e7d8;border-radius:16rpx;color:#76614b;font-size:25rpx;line-height:1.8}.confirm{margin-top:50rpx;background:#b5362d;color:white}.confirm-page button{margin-bottom:24rpx;font-size:29rpx;border-radius:16rpx}.error{display:block;color:#b5362d;font-size:25rpx;margin-top:24rpx}
</style>
