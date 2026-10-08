<template>
  <view class="scan-page">
    <view class="seal">茶</view>
    <text class="title">欢迎到店</text>
    <view v-if="loading" class="hint">正在识别分店…</view>
    <view v-else-if="error" class="entry-card">
      <text class="hint">{{ error }}</text>
      <button @tap="loadStore">重试</button>
      <button class="secondary" @tap="home">查看营业分店</button>
    </view>
    <view v-else class="entry-card">
      <text class="store-name">{{ store.name }}</text>
      <text v-if="store.address" class="hint">{{ store.address }}</text>
      <text class="hint">登录后可预订茶室、查看订单和会员账户。</text>
      <text class="privacy">首次绑定需填写手机号并验证短信；已绑定的会员可直接登录。</text>

      <button class="wechat" :disabled="busy" :loading="busy" @tap="login">手机号快捷登录</button>
      <PrivacyConsent ref="privacyConsent" @change="privacyFormAllowed=$event" :member="true" />
      <text v-if="loginError" class="error">{{ loginError }}</text>
    </view>
  </view>
</template>
<script>
import PrivacyConsent from '@/components/PrivacyConsent.vue'
import api from '@/common/api.js'
import { auth } from '@/common/store.js'
import { wechatLogin } from '@/common/wechat.js'
import { completeCustomerWechat } from '@/common/customer-wechat.js'
import { parseStoreScene } from '@/common/scan-entry.mjs'
export default {
  components: { PrivacyConsent },
  data() { return{privacyFormAllowed:false, scene: '', store: {}, loading: true, busy: false, error: '', loginError: '', loadVersion: 0 } },
  onLoad(options) { uni.setStorageSync('pending_store_scene', { scene: options?.scene || '' }) },
  onShow() {
    const pending = uni.getStorageSync('pending_store_scene')
    if (pending && typeof pending === 'object') {
      this.scene = pending.scene || ''
      uni.removeStorageSync('pending_store_scene')
      this.loadStore()
    }
  },
  onUnload() { this.loadVersion++ },
  methods: {
    async loadStore() {
      const version = ++this.loadVersion
      this.loading = true; this.error = ''; this.loginError = ''
      uni.setStorageSync('app_identity', 'customer')
      try {
        const id = parseStoreScene(this.scene)
        if (!id) throw new Error('小程序码无效，请重新扫描门店提供的小程序码')
        const stores = await api.customerStores()
        if (version !== this.loadVersion) return
        const store = stores.find(s => Number(s.id) === id && s.status !== 0)
        if (!store) throw new Error('该分店暂未营业或已停用，请联系门店')
        this.store = store
        uni.setStorageSync('customer_store_id', id)
        if (uni.getStorageSync('customer_token') && uni.getStorageSync('customer_user')?.memberId) {
          await api.customerHome()
          if (version === this.loadVersion) this.home()
        }
      } catch(e) { if (version === this.loadVersion) this.error = e.message || '暂时无法连接门店，请稍后重试' }
      finally { if (version === this.loadVersion) this.loading = false }
    },
    async login() {
      if (this.busy || this.loading || this.error) return
      this.busy = true
      if (!await this.$refs.privacyConsent.ensure()) { this.busy=false; return }
       this.loginError = ''
      try {
        const result = await wechatLogin('CUSTOMER')
        if (completeCustomerWechat(result)) this.home()
      } catch(e) { this.loginError = e.message || '登录未完成，请重试' }
      finally { this.busy = false }
    },
    home() { uni.setStorageSync('app_identity', 'customer'); uni.reLaunch({ url: '/pages/customer/home' }) },
    account() { uni.navigateTo({ url: '/pages/customer/login' }) }
  }
}
</script>
<style scoped>
.scan-page{min-height:100vh;background:#fff7ef;padding:100rpx 40rpx;color:#422b24}.seal{width:100rpx;height:100rpx;border-radius:24rpx;background:#b5362d;color:#fff;font-size:52rpx;display:flex;align-items:center;justify-content:center;margin:0 auto 28rpx}.title{display:block;text-align:center;font-size:44rpx;font-weight:600}.entry-card{margin-top:40rpx;background:#fffdfa;border:1rpx solid #ecdfd2;border-radius:28rpx;padding:40rpx 32rpx}.store-name{display:block;font-size:34rpx;font-weight:600;margin-bottom:18rpx}.hint,.privacy,.error{display:block;font-size:26rpx;line-height:1.8;margin:20rpx 0;color:#807366}.privacy{font-size:23rpx;background:#f8f0e5;padding:20rpx;border-radius:14rpx}.error{color:#b5362d}.entry-card button{font-size:28rpx;line-height:92rpx;border-radius:16rpx;margin-top:24rpx}.wechat{background:#b5362d;color:#fff}.secondary{background:transparent;color:#80644c}
</style>
