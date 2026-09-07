<template>
  <view class="page">
    <view v-if="!loggedIn" class="card"><text>登录后查看个人账户和预约</text><button @tap="login">登录 / 注册</button></view>
    <template v-else>
      <view class="card"><text class="name">{{member.name || '加载中…'}}</text><text class="muted">{{member.phone || '未绑定手机号'}} · {{member.levelText}}</text><view class="stats"><view><text class="stat-number">{{member.balance ?? '—'}}</text><text>账户余额</text></view><view><text class="stat-number">{{member.totalConsume ?? '—'}}</text><text>累计消费</text></view><view><text class="stat-number">{{member.points ?? '—'}}</text><text>积分</text></view></view></view>
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
  async load(){try{const [member,reservations,records]=await Promise.all([api.customerHome(),api.customerReservations(),api.customerRecords()]);this.member=member;this.reservations=reservations;this.records=records}catch{}},
  cancel(r){uni.showModal({title:'取消预约',content:'确认取消该预约？取消后释放房间时段。',success:async result=>{if(!result.confirm)return;try{await api.customerCancel(r.id,'顾客自行取消');uni.showToast({title:'已取消'});await this.load()}catch{}}})},
  logout(){uni.removeStorageSync('customer_token');uni.removeStorageSync('customer_user');this.member={};this.records=[];this.reservations=[];this.loggedIn=false}
 }
}
</script>
<style scoped>.page{padding:32rpx;background:#f5f2e9;min-height:100vh}.card{background:white;padding:30rpx;border-radius:22rpx;margin-bottom:24rpx}.name{font-size:38rpx;font-weight:600;display:block;margin-bottom:12rpx}.muted{font-size:23rpx;color:#9e9184;display:block;line-height:1.8}.stats{display:flex;justify-content:space-between;margin-top:30rpx}.stats view{text-align:center}.stats b{font-size:34rpx;color:#605346}.stats text{display:block;font-size:23rpx;color:#9e9184;margin-top:12rpx}.heading{font-size:30rpx;margin:32rpx 0 18rpx}.heading text{font-size:22rpx;color:#9e9184;margin-left:16rpx}.record{padding:18rpx 0;border-bottom:1px solid #eee;font-size:27rpx}.record:last-child{border:none}.record button{margin-top:12rpx}.page>button{font-size:26rpx;margin-top:20rpx}.logout{color:#8c1f28}.page{padding:32rpx 32rpx 170rpx;background:#fff7ef}.page>.card:first-child{background:#b5362d;color:#fff9ea;padding:38rpx 30rpx}.page>.card:first-child .muted{color:#ffe1cc}.card{border:1rpx solid #eee1d4;border-radius:24rpx;background:#fffdfa}.stats{padding-top:24rpx;border-top:1rpx solid #ffffff26}.stats .stat-number{display:block;font-size:34rpx;color:#ffe0a9}.stats text{color:#ffe1cc;font-size:21rpx}.heading{font-family:serif;font-size:34rpx;font-weight:600;display:flex;justify-content:space-between;align-items:center}.heading text{font-family:sans-serif;font-weight:400;color:#86796c}.record{padding:24rpx 0;line-height:1.7}.branch-note{display:block;font-size:23rpx;color:#5f5245}.record button{border:1rpx solid #eaddd0;background:#fff5eb;color:#6f6255;border-radius:10rpx;font-size:22rpx}.page>button{background:#fffdfa;border:1rpx solid #eee1d4;border-radius:16rpx;color:#64574a}.page>.logout{background:transparent;border:0;color:#8a6461}.card button{font-size:26rpx;border-radius:14rpx}.name{font-family:serif;font-size:42rpx}.muted{color:#817467}
</style>
