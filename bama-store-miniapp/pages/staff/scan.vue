<template>
  <view class="page">
    <view class="scanbox brand-bg">
      <view class="finder">
        <view class="corner tl"></view><view class="corner tr"></view>
        <view class="corner bl"></view><view class="corner br"></view>
        <view class="line"></view>
      </view>
      <text class="tip">对准会员付款码 / 二维码</text>
      <view class="scanbtn" @tap="scanQr">📷 调用扫一扫</view>
    </view>

    <view class="manual card">
      <text class="label">或手动输入付款码</text>
      <input v-model="payCode" class="input" placeholder="粘贴 / 输入会员付款码" />
      <view class="btn-primary submit" @tap="resolve">识别会员</view>
      <text class="note">提示：可先在顾客端「付款码」页生成付款码</text>
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'

export default {
  data() {
    return { payCode: '' }
  },
  methods: {
    scanQr() {
      // 微信小程序 / App 支持扫码；H5 不支持时提示手动输入
      // #ifdef MP-WEIXIN || APP-PLUS
      uni.scanCode({
        success: (res) => {
          this.payCode = res.result
          this.resolve()
        },
        fail: () => uni.showToast({ title: '扫码取消', icon: 'none' })
      })
      // #endif
      // #ifdef H5
      uni.showToast({ title: 'H5 环境请手动输入付款码', icon: 'none' })
      // #endif
    },
    async resolve() {
      if (!this.payCode) {
        uni.showToast({ title: '请输入付款码', icon: 'none' })
        return
      }
      try {
        const vo = await api.chargeResolve(this.payCode.trim())
        // 暂存会员信息，进入收款确认页
        uni.setStorageSync('chargeMember', vo)
        uni.navigateTo({ url: '/pages/staff/charge' })
      } catch (e) {
        // 错误已统一提示
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; }
.scanbox {
  padding: 80rpx 48rpx 60rpx; display: flex; flex-direction: column; align-items: center;
}
.finder { width: 380rpx; height: 380rpx; position: relative; margin-bottom: 40rpx; }
.corner { position: absolute; width: 60rpx; height: 60rpx; border: 6rpx solid $gold; }
.corner.tl { top: 0; left: 0; border-right: none; border-bottom: none; }
.corner.tr { top: 0; right: 0; border-left: none; border-bottom: none; }
.corner.bl { bottom: 0; left: 0; border-right: none; border-top: none; }
.corner.br { bottom: 0; right: 0; border-left: none; border-top: none; }
.line {
  position: absolute; left: 10rpx; right: 10rpx; top: 20rpx; height: 4rpx;
  background: $gold; box-shadow: 0 0 16rpx $gold;
}
.tip { color: rgba(255,255,255,0.85); font-size: 26rpx; }
.scanbtn {
  margin-top: 40rpx; color: #fff; font-size: 28rpx;
  border: 2rpx solid rgba(255,255,255,0.5); border-radius: 999rpx; padding: 20rpx 50rpx;
}
.manual { margin: 40rpx 32rpx; padding: 36rpx; }
.label { font-size: 24rpx; color: $muted; }
.input {
  margin: 16rpx 0 24rpx; height: 88rpx; background: $porcelain; border-radius: 16rpx;
  padding: 0 24rpx; font-size: 28rpx;
}
.submit { padding: 26rpx; font-size: 30rpx; font-weight: 600; }
.note { display: block; text-align: center; color: $muted; font-size: 22rpx; margin-top: 20rpx; }
</style>
