<template>
  <view class="page">
    <view class="pay-nav"><button class="back-home" @tap="backHome" aria-label="返回首页">‹ 返回首页</button></view>
    <text class="title">会员付款码</text>
    <text class="tip">请向店员出示，由员工端扫一扫收款</text>
    <view class="card">
      <text class="card-title">门店会员余额付款</text>
      <view v-if="loading" class="placeholder">正在更新付款码…</view>
      <view v-else-if="payCode" class="qr" role="img" aria-label="会员付款二维码">
        <view v-for="(row,r) in qrRows" :key="r" class="qr-row"><view v-for="(dark,c) in row" :key="c" class="qr-cell" :class="{dark}" /></view>
      </view>
      <view v-else class="placeholder">生成失败，请点击下方刷新</view>
      <text v-if="payCode" class="code mono">{{ payCode }}</text>
      <text v-if="payCode" class="countdown">{{ countdown }} 秒后自动更新</text>
      <text class="note">仅用于本店会员余额扣款</text>
    </view>
    <view class="balance-bar"><text>会员卡可用余额</text><text class="balance mono">¥{{ Number(member.balance || 0).toFixed(2) }}</text></view>
    <button class="refresh" :disabled="loading" @tap="refresh">{{loading ? '更新中…' : '刷新付款码'}}</button>
  </view>
</template>
<script>
import api from '@/common/api.js'
import { paymentQrRows } from '@/common/payment-qr.js'
export default {
  data() { return {member:{},payCode:'',qrRows:[],loading:false,countdown:60,timer:null,active:false,requestId:0} },
  onShow() {
    if (!uni.getStorageSync('customer_token')) { uni.redirectTo({url:'/pages/customer/login'}); return }
    this.active=true
    this.refresh()
  },
  onHide() { this.stop() },
  onUnload() { this.stop() },
  methods: {
    backHome() { this.stop(); uni.reLaunch({url:'/pages/customer/home'}) },
    stop() {
      this.active=false
      this.requestId++
      clearInterval(this.timer)
      this.timer=null
      this.payCode=''
      this.qrRows=[]
      this.loading=false
    },
    async refresh() {
      if(this.loading || !this.active)return
      clearInterval(this.timer)
      this.timer=null
      this.loading=true
      this.payCode=''
      this.qrRows=[]
      const id=++this.requestId
      try {
        const member=await api.customerHome()
        if(!this.active || id!==this.requestId)return
        const requestedAt=Date.now()
        const res=await api.customerPaycode()
        if(!this.active || id!==this.requestId)return
        const expiresAt=requestedAt+60000
        if(Date.now()>=expiresAt)throw new Error('付款码已过期，请刷新')
        this.qrRows=paymentQrRows(res.payCode)
        this.member=member
        this.payCode=res.payCode
        this.countdown=Math.ceil((expiresAt-Date.now())/1000)
        this.timer=setInterval(()=>{
          this.countdown=Math.max(0,Math.ceil((expiresAt-Date.now())/1000))
          if(this.countdown===0)this.refresh()
        },500)
      } catch(e) {
        if(this.active && id===this.requestId){this.payCode='';this.qrRows=[]}
      } finally { if(id===this.requestId)this.loading=false }
    }
  }
}
</script>
<style scoped>
.page{min-height:100vh;background:#b5362d;padding:24rpx 36rpx 60rpx;padding-top:calc(24rpx + env(safe-area-inset-top));display:flex;flex-direction:column;align-items:center}
.pay-nav{width:100%;margin-bottom:24rpx;display:flex;justify-content:flex-start}
.back-home{width:auto;margin:0;padding:0 20rpx;line-height:76rpx;min-height:76rpx;font-size:26rpx;background:#ffffff18;color:#fff;border:1rpx solid #ffffff40;border-radius:14rpx}
.title{color:#fff;font-family:serif;font-size:44rpx;letter-spacing:4rpx;font-weight:600}
.tip{color:#ffe1cc;font-size:24rpx;margin-top:16rpx;line-height:1.8;text-align:center}
.card{background:#fffdfa;border-radius:28rpx;padding:36rpx 24rpx;width:100%;margin-top:40rpx;display:flex;flex-direction:column;align-items:center;box-shadow:0 20rpx 60rpx #6e251e33}
.card-title{color:#624739;font-size:26rpx;margin-bottom:22rpx}
.qr{padding:48rpx;background:#fff;flex-shrink:0}
.qr-row{display:flex;height:12rpx}
.qr-cell{width:12rpx;height:12rpx;flex-shrink:0;background:#fff}
.qr-cell.dark{background:#000}
.placeholder{height:348rpx;display:flex;align-items:center;color:#998576;font-size:25rpx;text-align:center}
.code{color:#614a40;font-size:24rpx;letter-spacing:2rpx;margin-top:18rpx;word-break:break-all;text-align:center}
.countdown{margin-top:18rpx;color:#b5362d;font-size:23rpx}
.note{margin-top:24rpx;color:#a18d7f;font-size:21rpx}
.balance-bar{margin-top:32rpx;width:100%;background:#ffffff0d;border:1rpx solid #ffffff30;border-radius:16rpx;padding:26rpx;display:flex;align-items:center;justify-content:space-between;color:#fff;font-size:25rpx}
.balance{font-size:34rpx;font-weight:600}
.refresh{margin-top:36rpx;background:transparent;color:#fff;font-size:25rpx;border:1rpx solid #ffffff80;border-radius:14rpx;padding:0 36rpx;line-height:84rpx}
</style>
