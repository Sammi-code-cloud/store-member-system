<template>
  <view class="login-page">
    <view class="seal">馬</view>
    <text class="title">{{bindingEntry?'绑定会员微信':'微信登录'}}</text>
    <text class="intro">{{bindingEntry?'填写门店登记的会员手机号，将当前微信绑定到该会员档案。':(phoneRequired?'首次登录请填写手机号，完成会员注册。':'已绑定会员可直接微信登录，无需重复填写手机号。')}}</text>
    <view class="form-card">
      <view v-if="bindingEntry || phoneRequired" class="field"><text>手机号</text><input v-model="phone" type="number" maxlength="11" :disabled="loading" placeholder="请手动输入11位手机号" @confirm="submit"/></view>
      <view v-if="!bindingEntry && phoneRequired" class="field"><text>称呼（选填）</text><input v-model="name" maxlength="32" :disabled="loading" placeholder="首次注册时，填写你的称呼"/></view>
      <button class="wechat-login" :loading="loading" :disabled="loading" @tap="submit">{{loading?'正在处理…':(bindingEntry?'确认绑定当前微信':(phoneRequired?'注册并进入':'微信一键登录'))}}</button>
      <text class="note">{{bindingEntry?'请填写门店登记的手机号。':(phoneRequired?'手机号用于会员联系资料，不会自动合并其他会员账户。':'使用已绑定的微信，即可查看原会员账户和消费记录。')}}</text>
    </view>
    <button class="staff-entry" :disabled="loading" @tap="staffLogin">员工账号登录 ›</button>
  </view>
</template>
<script>
import {auth} from '@/common/store.js'
import {loginWechatWithPhone,parseMemberScene,bindMemberWechat} from '@/common/wechat.js'
import {completeCustomerWechat} from '@/common/customer-wechat.js'
export default {
  data(){return{phone:'',name:'',loading:false,phoneRequired:false,bindingEntry:false,memberTicket:null}},
  onLoad(options){this.phoneRequired=options?.phoneRequired==='1';if(options?.scene)this.acceptScene(options.scene)},
  onShow(){const pending=uni.getStorageSync('pending_member_scene');if(pending){uni.removeStorageSync('pending_member_scene');this.acceptScene(pending.scene)}},
  methods:{
    acceptScene(scene){this.bindingEntry=true;this.memberTicket=parseMemberScene(scene);this.phone='';this.name=''},
    staffLogin(){if(!this.loading)auth.switchToStaff()},
    async submit(){
      if(this.loading)return
      const phone=String(this.phone || '').trim()
      if((this.bindingEntry || this.phoneRequired) && !/^1[3-9]\d{9}$/.test(phone)){uni.showToast({title:'请输入正确的11位手机号',icon:'none'});return}
      this.loading=true
      try{const result=this.bindingEntry?await bindMemberWechat(this.memberTicket,phone):await loginWechatWithPhone(this.phoneRequired?phone:'',this.name);if(result?.manualPhoneRequired){this.phoneRequired=true;return}if(completeCustomerWechat(result))uni.reLaunch({url:'/pages/customer/home'})}
      catch(e){uni.showToast({title:e.message || '登录失败，请重试',icon:'none'})}
      finally{this.loading=false}
    }
  }
}
</script>
<style scoped>
.login-page{padding:90rpx 40rpx;min-height:100vh;box-sizing:border-box;background:#fff7ef}.seal{width:90rpx;height:90rpx;border-radius:20rpx;background:#b5362d;color:#fff;display:flex;align-items:center;justify-content:center;font-size:48rpx;font-family:serif;margin-bottom:40rpx}.title{font-size:46rpx;display:block;font-weight:600;color:#423528;letter-spacing:3rpx}.intro{display:block;font-size:26rpx;color:#807366;margin:22rpx 0 40rpx;line-height:1.8}.form-card{background:#fffdfa;border:1rpx solid #ecdfd2;border-radius:28rpx;padding:38rpx 32rpx}.field{margin-bottom:28rpx}.field text{font-size:26rpx;color:#6a5d50}.field input{height:98rpx;background:#fff5eb;border:1rpx solid #f1e4d7;border-radius:14rpx;padding:0 22rpx;margin-top:14rpx;font-size:28rpx}.wechat-login{background:#368352;color:white;font-size:30rpx;line-height:96rpx;border-radius:16rpx;margin-top:38rpx}.wechat-login[disabled]{opacity:.6}.note{display:block;color:#94877a;font-size:23rpx;line-height:1.8;margin-top:24rpx}.staff-entry{background:transparent;color:#9b6d55;font-size:25rpx;margin-top:28rpx}
</style>
