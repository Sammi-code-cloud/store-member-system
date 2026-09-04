<template>
  <view class="page">
    <view class="form card">
      <view class="field">
        <text class="label">会员 ID</text>
        <input v-model.number="form.memberId" class="input mono" type="number" placeholder="输入会员ID（演示：1）" />
      </view>
      <view class="field">
        <text class="label">充值金额（元）</text>
        <input v-model.number="form.amount" class="input mono" type="digit" placeholder="如 3000" />
      </view>
      <view class="field">
        <text class="label">赠送金额（元）</text>
        <input v-model.number="form.giftAmount" class="input mono" type="digit" placeholder="如 500" />
      </view>
      <view class="field">
        <text class="label">备注</text>
        <input v-model="form.remark" class="input" placeholder="选填" />
      </view>

      <view class="btn-primary submit" :class="{ disabled: submitting }" @tap="submit">
        {{ submitting ? '提交中...' : '确认储值' }}
      </view>
      <text class="note">储值本金与赠送分别记入流水，余额乐观锁更新</text>
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'

export default {
  data() {
    return {
      form: { memberId: 1, amount: null, giftAmount: null, remark: '' },
      submitting: false
    }
  },
  methods: {
    async submit() {
      if (!this.form.memberId || !this.form.amount) {
        uni.showToast({ title: '请填写会员ID和金额', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        await api.recharge({
          memberId: this.form.memberId,
          amount: this.form.amount,
          giftAmount: this.form.giftAmount || 0,
          remark: this.form.remark
        })
        const total = Number(this.form.amount) + Number(this.form.giftAmount || 0)
        uni.showModal({
          title: '储值成功',
          content: `到账 ¥${total.toFixed(2)}（本金 ¥${Number(this.form.amount).toFixed(2)} + 赠送 ¥${Number(this.form.giftAmount || 0).toFixed(2)}）`,
          showCancel: false,
          success: () => uni.navigateBack()
        })
      } catch (e) {
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.form { padding: 40rpx 36rpx; }
.field { margin-bottom: 28rpx; }
.label { font-size: 24rpx; color: $muted; }
.input {
  margin-top: 14rpx; height: 88rpx; background: $porcelain; border-radius: 16rpx;
  padding: 0 24rpx; font-size: 30rpx;
}
.quick { display: flex; flex-wrap: wrap; gap: 16rpx; margin-bottom: 30rpx; }
.quick .q {
  font-size: 24rpx; color: $brand; background: #FBF2F1; border: 2rpx solid #E9C9C6;
  padding: 12rpx 24rpx; border-radius: 999rpx;
}
.submit { padding: 28rpx; font-size: 32rpx; font-weight: 600; }
.submit.disabled { opacity: 0.6; }
.note { display: block; text-align: center; color: $muted; font-size: 22rpx; margin-top: 20rpx; }
</style>
