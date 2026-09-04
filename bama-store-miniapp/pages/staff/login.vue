<template>
  <view class="page">
    <view class="logo">
      <view class="seal">馬</view>
      <text class="title">八马员工端</text>
      <text class="sub">扫码收款 · 核销 · 储值</text>
    </view>

    <view class="form card">
      <view class="field">
        <text class="label">手机号</text>
        <input v-model="form.phone" class="input" type="number" placeholder="请输入登录手机号" />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input v-model="form.password" class="input" password placeholder="请输入密码" />
      </view>
      <view class="btn-primary submit" :class="{ disabled: loading }" @tap="submit">
        {{ loading ? '登录中...' : '登 录' }}
      </view>
    </view>

    <view class="hint">
      <text>演示账号</text>
      <text class="acc" @tap="fill('13800000000', 'admin123')">店长 13800000000 / admin123</text>
      <text class="acc" @tap="fill('13800000001', '123456')">收银员 13800000001 / 123456</text>
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'
import { auth } from '@/common/store.js'

export default {
  data() {
    return {
      form: { phone: '13800000001', password: '123456' },
      loading: false
    }
  },
  methods: {
    fill(phone, password) {
      this.form.phone = phone
      this.form.password = password
    },
    async submit() {
      if (!this.form.phone || !this.form.password) {
        uni.showToast({ title: '请输入账号和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const res = await api.login(this.form)
        auth.setLogin(res)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/staff/workbench' }), 600)
      } catch (e) {
        // 错误已由 request 统一提示
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; padding: 100rpx 48rpx; }
.logo { display: flex; flex-direction: column; align-items: center; margin-bottom: 70rpx; }
.seal {
  width: 108rpx; height: 108rpx; border-radius: 26rpx; background: $brand; color: #fff;
  font-family: serif; font-size: 56rpx; display: flex; align-items: center; justify-content: center;
}
.title { font-size: 44rpx; font-weight: 600; margin-top: 24rpx; }
.sub { font-size: 24rpx; color: $muted; margin-top: 10rpx; }
.form { padding: 40rpx 36rpx; }
.field { margin-bottom: 30rpx; }
.label { font-size: 24rpx; color: $muted; }
.input {
  margin-top: 14rpx; height: 88rpx; background: $porcelain; border-radius: 16rpx;
  padding: 0 24rpx; font-size: 30rpx;
}
.submit { margin-top: 20rpx; padding: 28rpx; font-size: 32rpx; font-weight: 600; }
.submit.disabled { opacity: 0.6; }
.hint { margin-top: 50rpx; display: flex; flex-direction: column; align-items: center; }
.hint > text:first-child { font-size: 22rpx; color: $muted; margin-bottom: 16rpx; }
.acc {
  font-size: 24rpx; color: $brand; padding: 10rpx 0;
}
</style>
