<template>
  <view class="login-page">
    <view class="seal">茶</view><text class="title">{{register?'欢迎成为我们的顾客':'欢迎回来'}}</text><text class="intro">{{register?'注册后即可预订茶室、查看个人账户':'登录你的门店账户，开启一段茶叙'}}</text>
    <button class="wechat-login" :loading="wechatLoading" :disabled="wechatLoading || loading" @tap="wechat">微信快捷登录</button>
    <view class="login-divider">或使用账号登录</view>
    <view class="form-card"><view class="field"><text>登录账号</text><input v-model="form.username" placeholder="4–32位字母、数字或下划线" maxlength="32"/></view><view class="field" v-if="register"><text>你的称呼</text><input v-model="form.name" placeholder="请输入姓名或昵称" maxlength="32"/></view><view class="field"><text>密码</text><input v-model="form.password" password placeholder="8–64位密码" maxlength="64"/></view><view class="field" v-if="register"><text>确认密码</text><input v-model="confirmPassword" password placeholder="再次输入密码" maxlength="64"/></view><button :loading="loading" :disabled="loading" @tap="submit">{{register?'注册并登录':'登录'}}</button><view class="switch" @tap="register=!register;form.password='';confirmPassword=''">{{register?'已有账号？去登录':'还没有账号？立即注册'}}</view></view>
  </view>
</template>
<script>
import api from '@/common/api.js'
import { wechatLogin } from '@/common/wechat.js'
export default {
  data(){return{register:false,wechatLoading:false,loading:false,confirmPassword:'',form:{username:'',password:'',name:''}}},
  onLoad(options){this.register=options?.mode==='register'},
  methods:{
    async wechat(){if(this.wechatLoading || this.loading)return;this.wechatLoading=true;try{const result=await wechatLogin('CUSTOMER');const user=result.account;uni.setStorageSync('customer_token',user.token);uni.setStorageSync('customer_user',{memberId:user.memberId,name:user.name});uni.reLaunch({url:'/pages/customer/home'})}catch(e){uni.showToast({title:e.message||'微信登录失败',icon:'none'})}finally{this.wechatLoading=false}},
    async submit(){
    if(!/^[a-zA-Z0-9_]{4,32}$/.test(this.form.username.trim()))return uni.showToast({title:'账号需为4–32位字母、数字或下划线',icon:'none'})
    if(this.register&&(!this.form.name.trim()||this.form.password.length<8||this.form.password!==this.confirmPassword))return uni.showToast({title:'请填写称呼和至少8位密码，并确认两次密码一致',icon:'none'})
    this.loading=true
    try{const user=await(this.register?api.customerRegister(this.form):api.customerLogin(this.form));uni.setStorageSync('customer_token',user.token);uni.setStorageSync('customer_user',{memberId:user.memberId,name:user.name});uni.reLaunch({url:'/pages/customer/home'})}catch{}finally{this.loading=false}
  }}
}
</script>
<style scoped>.login-page{padding:80rpx 40rpx;min-height:100vh;background:#f5f2e9}.seal{width:90rpx;height:90rpx;border-radius:24rpx;background:#55483b;color:#fff;display:flex;align-items:center;justify-content:center;font-size:48rpx;font-family:serif;margin-bottom:40rpx}.title{font-size:44rpx;display:block;font-weight:600;color:#423528}.intro{display:block;font-size:25rpx;color:#94877a;margin:20rpx 0 48rpx}.form-card{background:white;border-radius:24rpx;padding:36rpx}.field{margin-bottom:28rpx}.field text{font-size:25rpx;color:#6a5d50}.field input{height:84rpx;border-bottom:1px solid #f1e4d7;font-size:27rpx}.form-card button{background:#8c1f28;color:white;font-size:30rpx;border-radius:14rpx;margin-top:42rpx}.switch{text-align:center;margin-top:32rpx;font-size:25rpx;color:#8c1f28}.login-page{background:#fff7ef;padding-top:80rpx}.seal{background:#c33c2f;border-radius:18rpx;box-shadow:0 12rpx 32rpx #c33c2f15}.title{font-family:serif;letter-spacing:3rpx;line-height:1.5}.intro{color:#807366;line-height:1.8}.form-card{background:#fffdfa;border:1rpx solid #ecdfd2;border-radius:28rpx;padding:38rpx 32rpx}.field input{height:98rpx;background:#fff5eb;border:1rpx solid #f1e4d7;border-radius:12rpx;padding:0 22rpx;margin-top:14rpx;font-size:25rpx}.form-card button{background:#b5362d;line-height:96rpx;border-radius:14rpx}.switch{padding:10rpx;color:#726558}
.wechat-login{background:#368352;color:#fff;font-size:28rpx;line-height:96rpx;border-radius:16rpx;margin-bottom:20rpx}.login-divider{text-align:center;color:#9b8978;font-size:22rpx;margin:24rpx 0}</style>
