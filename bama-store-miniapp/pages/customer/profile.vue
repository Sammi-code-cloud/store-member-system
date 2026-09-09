<template>
  <view class="page">
    <view v-if="!loggedIn" class="guest-profile">
      <view class="guest-intro"><text class="guest-eyebrow">八马茶业 · 会员中心</text><text class="guest-title">好茶相伴，悦享日常</text><text class="guest-subtitle">一盏茶的闲暇，从这里开始</text></view>
      <view class="guest-membership">
        <view class="membership-header"><text class="membership-brand">八马</text><text class="membership-caption">我的会员账户</text></view>
        <view class="membership-copy"><text class="membership-title">欢迎成为八马会员</text><text class="membership-description">登录后查看余额、预约与每一笔消费记录</text></view>
        <button class="guest-login" @tap="login">微信登录 / 注册 <text>→</text></button>
        <text class="membership-footnote">首次登录自动注册会员</text>
        <view class="membership-ring ring-one"/><view class="membership-ring ring-two"/>
      </view>
      <view class="guest-section-heading"><text>我的服务</text><text class="guest-section-note">随时查看，安心到店</text></view>
      <view class="guest-services">
        <button class="guest-service" @tap="login"><view class="service-icon wallet-icon"><view/></view><text class="service-title">会员账户</text><text class="service-note">余额与资金记录</text></button>
        <button class="guest-service" @tap="login"><view class="service-icon calendar-icon"><view/></view><text class="service-title">我的预约</text><text class="service-note">查看到店安排</text></button>
        <button class="guest-service" @tap="login"><view class="service-icon receipt-icon"><view/></view><text class="service-title">付款凭证</text><text class="service-note">到店出示付款码</text></button>
      </view>
      <button class="guest-explore" @tap="browseRooms"><view><text class="explore-title">留一段时间，来喝茶</text><text class="explore-note">发现一间喜欢的茶室</text></view><text class="explore-arrow">↗</text></button>
      <view class="guest-signoff"><view/><text>一杯好茶 · 一份自在</text><view/></view>
    </view>
    <template v-else>
      <view class="card"><text class="name">{{member.name || '加载中…'}}</text><text class="muted">{{member.phone || '未绑定手机号'}} · {{member.levelText}}</text><view class="stats"><view><text class="stat-number">{{member.balance ?? '—'}}</text><text>账户余额</text></view></view></view>
      <view class="heading">我的预约 <text>最近20条</text></view><view class="card"><view v-if="!reservations.length" class="muted">暂无预约记录</view><view v-for="r in reservations" :key="r.id" class="record"><text>{{r.roomName}} · {{r.statusText}}</text><text class="branch-note">{{r.storeName}}</text><text class="muted">{{r.reserveDate}} {{r.startTime}} · {{r.hours}} 小时</text><text v-if="r.contactName" class="muted">联系人：{{r.contactName}} · {{r.contactPhone}}</text><text v-if="r.remark" class="muted">备注：{{r.remark}}</text><text v-if="r.status==='PENDING'" class="muted">申请已提交，等待店员确认，请勿视为预约成功。</text><text v-if="r.cancelReason" class="muted">原因：{{r.cancelReason}}</text><button v-if="['PENDING','WAITING'].includes(r.status)" size="mini" @tap="cancel(r)">取消预约</button></view></view>
      <view class="heading">资金记录 <text>最近10条</text></view><view class="card"><view v-if="!records.length" class="muted">暂无充值或消费记录</view><view v-for="r in records" :key="r.id" class="record"><text>{{types[r.type] || r.type}} · {{r.type==='CONSUME'?'-':'+'}}¥{{r.amount}}</text><text class="muted">{{(r.createTime||'').replace('T',' ')}} · 余额 ¥{{r.balanceAfter}}</text></view></view>
      <button @tap="load">刷新资料</button><button @tap="logout" class="logout">退出登录</button>
    </template>
    <CustomerNav active="profile" />
  </view>
</template>
<script>
import api from '@/common/api.js'
import CustomerNav from '@/components/CustomerNav.vue'
export default {
 components:{CustomerNav},
 data(){return{loggedIn:false,member:{},reservations:[],records:[],types:{RECHARGE:'充值本金',GIFT:'储值赠送',CONSUME:'消费扣款',REFUND:'退款'}}},
 onShow(){this.loggedIn=!!uni.getStorageSync('customer_token');if(this.loggedIn)this.load()},
 methods:{
  login(){uni.navigateTo({url:'/pages/customer/login'})},
  browseRooms(){uni.navigateTo({url:'/pages/customer/rooms'})},
  async load(){try{const [member,reservations,records]=await Promise.all([api.customerHome(),api.customerReservations(),api.customerRecords()]);this.member=member;this.reservations=reservations;this.records=records}catch{}},
  cancel(r){uni.showModal({title:'取消预约',content:'确认取消该预约？取消后释放房间时段。',success:async result=>{if(!result.confirm)return;try{await api.customerCancel(r.id,'顾客自行取消');uni.showToast({title:'已取消'});await this.load()}catch{}}})},
  logout(){uni.removeStorageSync('customer_token');uni.removeStorageSync('customer_user');this.member={};this.records=[];this.reservations=[];this.loggedIn=false}
 }
}
</script>
<style scoped>.page{padding:32rpx;background:#f5f2e9;min-height:100vh}.card{background:white;padding:30rpx;border-radius:22rpx;margin-bottom:24rpx}.name{font-size:38rpx;font-weight:600;display:block;margin-bottom:12rpx}.muted{font-size:23rpx;color:#9e9184;display:block;line-height:1.8}.stats{display:flex;justify-content:space-between;margin-top:30rpx}.stats view{flex:1;text-align:center}.stats b{font-size:34rpx;color:#605346}.stats text{display:block;font-size:23rpx;color:#9e9184;margin-top:12rpx}.heading{font-size:30rpx;margin:32rpx 0 18rpx}.heading text{font-size:22rpx;color:#9e9184;margin-left:16rpx}.record{padding:18rpx 0;border-bottom:1px solid #eee;font-size:27rpx}.record:last-child{border:none}.record button{margin-top:12rpx}.page>button{font-size:26rpx;margin-top:20rpx}.logout{color:#8c1f28}.page{padding:32rpx 32rpx 170rpx;background:#fff7ef}.page>.card:first-child{background:#b5362d;color:#fff9ea;padding:38rpx 30rpx}.page>.card:first-child .muted{color:#ffe1cc}.card{border:1rpx solid #eee1d4;border-radius:24rpx;background:#fffdfa}.stats{padding-top:24rpx;border-top:1rpx solid #ffffff26}.stats .stat-number{display:block;font-size:34rpx;color:#ffe0a9}.stats text{color:#ffe1cc;font-size:21rpx}.heading{font-family:serif;font-size:34rpx;font-weight:600;display:flex;justify-content:space-between;align-items:center}.heading text{font-family:sans-serif;font-weight:400;color:#86796c}.record{padding:24rpx 0;line-height:1.7}.branch-note{display:block;font-size:23rpx;color:#5f5245}.record button{border:1rpx solid #eaddd0;background:#fff5eb;color:#6f6255;border-radius:10rpx;font-size:22rpx}.page>button{background:#fffdfa;border:1rpx solid #eee1d4;border-radius:16rpx;color:#64574a}.page>.logout{background:transparent;border:0;color:#8a6461}.card button{font-size:26rpx;border-radius:14rpx}.name{font-family:serif;font-size:42rpx}.muted{color:#817467}

.guest-profile{padding:16rpx 0 0;color:#422b24}.guest-intro{padding:4rpx 4rpx 36rpx}.guest-eyebrow{display:block;font-size:22rpx;color:#a17459;letter-spacing:3rpx}.guest-title{display:block;margin-top:18rpx;font-size:42rpx;font-family:serif;font-weight:600;letter-spacing:2rpx}.guest-subtitle{display:block;margin-top:14rpx;color:#8a7a6d;font-size:25rpx}
.guest-membership{position:relative;overflow:hidden;padding:34rpx;border-radius:28rpx;background:linear-gradient(125deg,#9f2f28,#bd4939);color:#fff6e7;box-shadow:0 14rpx 30rpx #8f30251a}.membership-header,.membership-copy,.guest-login,.membership-footnote{position:relative;z-index:1}.membership-header{display:flex;align-items:center;justify-content:space-between}.membership-brand{font-size:35rpx;letter-spacing:8rpx;font-family:serif}.membership-caption{font-size:21rpx;letter-spacing:2rpx;color:#f0cbbb}.membership-copy{padding:40rpx 0 30rpx}.membership-title{display:block;font-size:34rpx;font-weight:600}.membership-description{display:block;font-size:23rpx;line-height:1.8;margin-top:12rpx;color:#f4d7c8}.guest-login{display:flex;align-items:center;justify-content:center;gap:24rpx;margin:0;background:#fff4df;color:#873027;font-size:28rpx;font-weight:600;line-height:88rpx;border-radius:16rpx}.guest-login::after,.guest-service::after,.guest-explore::after{border:0}.membership-footnote{display:block;text-align:center;margin-top:18rpx;font-size:21rpx;color:#f1cebc}.membership-ring{position:absolute;width:340rpx;height:340rpx;border:1rpx solid #fff0d322;border-radius:50%;right:-150rpx;top:-140rpx}.ring-two{width:430rpx;height:430rpx;right:-195rpx;top:-185rpx}
.guest-section-heading{display:flex;align-items:center;justify-content:space-between;margin:44rpx 4rpx 22rpx;font-size:30rpx;font-weight:600}.guest-section-note{font-size:21rpx;font-weight:400;color:#978679}.guest-services{display:flex;border:1rpx solid #eee2d6;border-radius:24rpx;background:#fffdfa;padding:30rpx 8rpx}.guest-service{flex:1;display:flex;flex-direction:column;align-items:center;padding:0;margin:0;background:transparent;line-height:1.5;border-radius:0}.guest-service+.guest-service{border-left:1rpx solid #eee2d6}.service-title{font-size:25rpx;color:#493b30;margin-top:22rpx}.service-note{font-size:19rpx;color:#938172;margin-top:8rpx}.service-icon{position:relative;box-sizing:border-box;width:42rpx;height:36rpx;color:#a65942;border:2rpx solid currentColor;border-radius:6rpx;margin:8rpx 0}.wallet-icon view{position:absolute;right:-3rpx;top:10rpx;width:17rpx;height:12rpx;background:#fffdfa;border:2rpx solid currentColor;border-radius:4rpx}.calendar-icon{border-top-width:8rpx}.calendar-icon view{width:6rpx;height:6rpx;background:currentColor;position:absolute;left:8rpx;top:9rpx;box-shadow:14rpx 0 currentColor}.receipt-icon{width:30rpx;height:39rpx;border-radius:2rpx}.receipt-icon view{position:absolute;left:6rpx;top:9rpx;width:14rpx;border-top:2rpx solid currentColor;box-shadow:0 8rpx currentColor}
.guest-explore{display:flex;align-items:center;justify-content:space-between;padding:28rpx 30rpx;margin:24rpx 0 0;border:1rpx solid #e7dfcf;border-radius:22rpx;background:#f0ecdf;text-align:left;line-height:1.6}.explore-title{display:block;font-family:serif;font-size:28rpx;color:#5c5943}.explore-note{display:block;font-size:22rpx;margin-top:5rpx;color:#86816b}.explore-arrow{font-size:36rpx;color:#777356}.guest-signoff{display:flex;align-items:center;justify-content:center;gap:20rpx;padding:42rpx 0 12rpx;color:#ac9986;font-size:20rpx;letter-spacing:3rpx}.guest-signoff view{width:40rpx;height:1rpx;background:#e1d4c6}
</style>
