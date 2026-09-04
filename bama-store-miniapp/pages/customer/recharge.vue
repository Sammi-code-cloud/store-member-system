<template>
  <view class="page">
    <!-- 余额卡 -->
    <view class="bal-card">
      <text class="k">当前余额（元）</text>
      <text class="amt mono">{{ member.balance }}</text>
      <text class="lv">◆ {{ member.level }} · 享 {{ member.discountText }}</text>
    </view>

    <view class="row-head">选择储值套餐</view>
    <view class="pkgs">
      <view
        v-for="(p, i) in packages"
        :key="i"
        class="pkg"
        :class="{ sel: selected === i }"
        @tap="selected = i"
      >
        <text v-if="p.hot" class="badge">热销</text>
        <text v-if="p.vip" class="badge">尊享</text>
        <text class="p mono">¥{{ p.amount }}</text>
        <text class="g">赠 ¥{{ p.gift }}</text>
      </view>
    </view>

    <view class="btn-primary cta" @tap="submit">
      立即储值 ¥{{ cur.amount }}（到账 ¥{{ cur.amount + cur.gift }}）
    </view>
    <text class="fine">储值金额可用于茶室预定、店内消费，本人会员卡专用，不可提现</text>
    <text class="demo-note">演示：正式环境通过微信支付完成充值</text>
  </view>
</template>

<script>
import { demoMember, demoPackages } from '@/common/mock.js'
export default {
  data() {
    return { member: demoMember, packages: demoPackages, selected: 1 }
  },
  computed: {
    cur() { return this.packages[this.selected] }
  },
  methods: {
    submit() {
      uni.showModal({
        title: '储值确认',
        content: `储值 ¥${this.cur.amount}，赠 ¥${this.cur.gift}，将拉起微信支付`,
        confirmText: '去支付',
        success: (r) => {
          if (r.confirm) uni.showToast({ title: '演示环境，暂未接入支付', icon: 'none' })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.bal-card {
  background: linear-gradient(135deg, #2b2320, #4a3a2f); border-radius: 28rpx;
  padding: 36rpx; color: #f6ecdd; display: flex; flex-direction: column;
}
.bal-card .k { font-size: 24rpx; opacity: 0.75; }
.bal-card .amt { font-size: 64rpx; font-weight: 600; margin-top: 10rpx; }
.bal-card .lv {
  margin-top: 20rpx; align-self: flex-start; font-size: 22rpx; color: $gold-soft;
  background: rgba(200,150,80,0.25); border: 2rpx solid rgba(200,150,80,0.5);
  padding: 8rpx 20rpx; border-radius: 999rpx;
}
.row-head { font-size: 30rpx; font-weight: 600; margin: 36rpx 0 20rpx; }
.pkgs { display: flex; flex-wrap: wrap; gap: 20rpx; }
.pkg {
  width: calc(50% - 10rpx); background: #fff; border: 3rpx solid $line;
  border-radius: 22rpx; padding: 30rpx; display: flex; flex-direction: column;
  align-items: center; position: relative;
}
.pkg.sel { border-color: $brand; background: #FBF2F1; }
.pkg .p { font-size: 44rpx; font-weight: 600; color: $ink; }
.pkg .g { font-size: 24rpx; color: $brand; margin-top: 8rpx; }
.pkg .badge {
  position: absolute; top: -14rpx; right: 20rpx; background: $gold; color: #fff;
  font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 999rpx;
}
.cta { margin-top: 40rpx; padding: 30rpx; font-size: 30rpx; font-weight: 600; }
.fine { display: block; text-align: center; color: $muted; font-size: 22rpx; margin-top: 20rpx; }
.demo-note { display: block; text-align: center; color: $muted; font-size: 22rpx; margin-top: 16rpx; }
</style>
