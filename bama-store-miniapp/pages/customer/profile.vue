<template>
  <view class="page">
    <view class="profile card">
      <view class="avatar">{{ member.name.charAt(0) }}</view>
      <view class="info">
        <text class="nm">{{ member.name }}</text>
        <text class="mo">{{ member.phone }} · {{ member.level }}</text>
      </view>
    </view>

    <view class="stats card">
      <view class="s">
        <text class="n mono">{{ member.balance }}</text>
        <text class="l">卡内余额</text>
      </view>
      <view class="s">
        <text class="n mono">{{ member.visits }}</text>
        <text class="l">累计到店</text>
      </view>
      <view class="s">
        <text class="n mono">{{ member.points }}</text>
        <text class="l">积分</text>
      </view>
    </view>

    <view class="row-head">消费记录</view>
    <view class="list card">
      <view v-for="(r, i) in records" :key="i" class="li">
        <view class="lic">{{ r.icon }}</view>
        <view class="txt">
          <text class="t1">{{ r.title }}</text>
          <text class="t2">{{ r.time }}</text>
        </view>
        <text class="amt mono" :class="r.type">{{ r.amount }}</text>
      </view>
    </view>
    <text class="demo-note">演示数据；接入顾客登录后展示真实流水</text>
  </view>
</template>

<script>
import { demoMember, demoRecords } from '@/common/mock.js'
export default {
  data() {
    return { member: demoMember, records: demoRecords }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.profile { display: flex; align-items: center; padding: 30rpx; }
.avatar {
  width: 104rpx; height: 104rpx; border-radius: 28rpx;
  background: linear-gradient(135deg, $gold, $brand); color: #fff; font-family: serif;
  font-size: 44rpx; display: flex; align-items: center; justify-content: center; margin-right: 24rpx;
}
.info .nm { font-size: 36rpx; font-weight: 600; }
.info .mo { display: block; font-size: 24rpx; color: $muted; margin-top: 8rpx; }

.stats { display: flex; margin-top: 24rpx; }
.stats .s {
  flex: 1; padding: 30rpx 0; display: flex; flex-direction: column; align-items: center;
  border-right: 2rpx solid $line;
}
.stats .s:last-child { border-right: none; }
.stats .n { font-size: 38rpx; font-weight: 600; color: $brand; }
.stats .l { font-size: 22rpx; color: $muted; margin-top: 8rpx; }

.row-head { font-size: 30rpx; font-weight: 600; margin: 36rpx 0 20rpx; }
.list { overflow: hidden; }
.li { display: flex; align-items: center; padding: 26rpx; border-bottom: 2rpx solid $line; }
.li:last-child { border-bottom: none; }
.lic {
  width: 68rpx; height: 68rpx; border-radius: 18rpx; background: #F6EEDF; color: $brand;
  font-size: 32rpx; display: flex; align-items: center; justify-content: center; margin-right: 22rpx;
}
.txt { flex: 1; }
.txt .t1 { font-size: 26rpx; }
.txt .t2 { display: block; font-size: 22rpx; color: $muted; margin-top: 4rpx; }
.amt { font-size: 30rpx; font-weight: 600; }
.amt.out { color: $ink; }
.amt.in { color: $green; }
.demo-note { display: block; text-align: center; color: $muted; font-size: 22rpx; margin-top: 16rpx; }
</style>
