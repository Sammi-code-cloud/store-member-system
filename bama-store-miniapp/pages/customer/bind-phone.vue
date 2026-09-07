<template>
  <view class="bind-page">
    <view class="seal">茶</view>
    <text class="title">首次绑定手机号</text>
    <text class="intro">验证后关联你的会员账户，同一微信下次可直接登录，无需再次接收短信。</text>
    <view class="card">
      <text v-if="!ticket || expired" class="notice">微信验证已过期，请重新验证微信身份。</text>
      <text v-else-if="!smsEnabled" class="notice">门店短信服务尚未开通，暂时无法绑定。你可以使用已有账号登录，或联系门店。</text>
      <view class="field"><text>手机号</text><input v-model="phone" type="number" maxlength="11" :disabled="sending || saving" placeholder="请输入本人手机号" @input="phoneChanged" /></view>
      <view class="field"><text>短信验证码</text><view class="code-row"><input v-model="code" type="number" maxlength="6" :disabled="saving" placeholder="6位验证码" /><button class="send" :disabled="!canSend" :loading="sending" @tap="send">{{ remaining > 0 ? remaining + '秒后重发' : '获取验证码' }}</button></view></view>
      <text class="privacy">点击获取验证码即表示同意将该手机号用于账户绑定、会员身份验证及本次短信验证。不会仅凭填写手机号关联账户。</text>
      <text v-if="error" class="error">{{ error }}</text>
      <button class="primary" :disabled="!canBind" :loading="saving" @tap="bind">验证并登录</button>
      <button v-if="!ticket || expired || !smsEnabled" class="secondary" :disabled="sending || saving || refreshing" :loading="refreshing" @tap="refresh">重新验证微信</button>
    </view>
    <button class="secondary" :disabled="saving || sending" @tap="account">使用账号登录</button>
    <text class="footer">已有会员将关联原账户，新顾客验证后创建账户，各分店共用。</text>
  </view>
</template>
<script>
import request from '@/common/request.js'
import { wechatLogin } from '@/common/wechat.js'
import { auth } from '@/common/store.js'
import { pendingCustomerBinding, clearCustomerBinding, completeCustomerWechat } from '@/common/customer-wechat.js'
export default {
  data() { return { ticket: '', smsEnabled: false, expiresAt: 0, now: Date.now(), retryAt: 0, phone: '', code: '', challenge: '', sentPhone: '', sending: false, saving: false, refreshing: false, error: '', timer: null } },
  computed: {
    expired() { return this.now >= this.expiresAt },
    remaining() { return Math.max(0, Math.ceil((this.retryAt - this.now) / 1000)) },
    canSend() { return this.ticket && !this.expired && this.smsEnabled && /^1[3-9]\d{9}$/.test(this.phone) && !this.remaining && !this.sending && !this.saving },
    canBind() { return this.ticket && !this.expired && this.challenge && this.sentPhone === this.phone && /^\d{6}$/.test(this.code) && !this.sending && !this.saving }
  },
  onLoad() { this.loadPending() },
  onShow() { this.now = Date.now(); clearInterval(this.timer); this.timer = setInterval(() => { this.now = Date.now() }, 1000) },
  onHide() { clearInterval(this.timer) },
  onUnload() { clearInterval(this.timer); clearCustomerBinding(); this.ticket = ''; this.code = ''; this.challenge = '' },
  methods: {
    loadPending() { const p = pendingCustomerBinding(); this.ticket = p?.ticket || ''; this.smsEnabled = !!p?.smsEnabled; this.expiresAt = p?.expiresAt || 0; this.now = Date.now() },
    phoneChanged() { this.challenge = ''; this.code = ''; this.error = '' },
    async send() {
      if (!this.canSend) return
      this.sending = true; this.error = ''; this.challenge = ''; this.code = ''
      const phone = this.phone
      try {
        const res = await request({ url: '/api/wechat/customer/sms', method: 'POST', data: { ticket: this.ticket, phone } })
        this.sentPhone = phone; this.challenge = res.challenge; this.retryAt = Date.now() + res.retryAfter * 1000; this.now = Date.now()
        uni.showToast({ title: '验证码已发送，请查收', icon: 'none' })
      } catch (e) { this.error = e.message || '发送失败，请稍后重试' }
      finally { this.sending = false }
    },
    async bind() {
      if (!this.canBind) return
      this.saving = true; this.error = ''
      try {
        const res = await request({ url: '/api/wechat/customer/bind', method: 'POST', data: { ticket: this.ticket, phone: this.phone, challenge: this.challenge, code: this.code } })
        auth.setCustomerLogin(res.account); clearCustomerBinding(); uni.reLaunch({ url: '/pages/customer/home' })
      } catch (e) { this.error = e.message || '绑定失败，请重试'; this.code = '' }
      finally { this.saving = false }
    },
    async refresh() {
      if (this.refreshing) return
      this.refreshing = true; this.error = ''; this.challenge = ''; this.code = ''
      try {
        const res = await wechatLogin('CUSTOMER')
        if (!res.bindRequired) { if (completeCustomerWechat(res)) uni.reLaunch({ url: '/pages/customer/home' }); return }
        // Update this page in place; no extra navigation or persistent ticket.
        this.ticket = res.bindTicket; this.smsEnabled = res.smsEnabled === true; this.expiresAt = Date.now() + 300000; this.now = Date.now()
      } catch (e) { this.error = e.message || '微信验证失败，请重试' }
      finally { this.refreshing = false }
    },
    account() { clearCustomerBinding(); uni.redirectTo({ url: '/pages/customer/login' }) }
  }
}
</script>
<style scoped>
.bind-page{min-height:100vh;background:#fff7ef;padding:64rpx 36rpx;color:#423528}.seal{background:#b5362d;color:#fff;width:84rpx;height:84rpx;border-radius:18rpx;display:flex;align-items:center;justify-content:center;font-size:44rpx;margin-bottom:32rpx}.title{display:block;font-size:42rpx;font-weight:600}.intro,.footer{display:block;font-size:25rpx;line-height:1.8;color:#807366;margin:22rpx 0 34rpx}.card{background:#fffdfa;border:1rpx solid #ecdfd2;border-radius:26rpx;padding:32rpx}.field{margin:16rpx 0 28rpx}.field>text{font-size:26rpx}.field input{height:90rpx;background:#fff5eb;border:1rpx solid #ecdfd2;border-radius:12rpx;padding:0 18rpx;margin-top:14rpx;font-size:27rpx}.code-row{display:flex;align-items:center;gap:12rpx}.code-row input{flex:1;min-width:0;width:0}.send{width:220rpx;flex-shrink:0;background:#f3e6d8;color:#8c332b;font-size:24rpx;line-height:88rpx;padding:0 8rpx;margin:14rpx 0 0}.privacy{display:block;font-size:22rpx;color:#8a7b6d;line-height:1.8}.primary{background:#368352;color:#fff;line-height:94rpx;font-size:29rpx;margin-top:28rpx;border-radius:14rpx}.secondary{background:transparent;color:#80644c;line-height:86rpx;font-size:26rpx;margin-top:20rpx}.notice{display:block;background:#fff0d9;padding:20rpx;border-radius:12rpx;font-size:25rpx;line-height:1.8}.error{display:block;color:#b5362d;font-size:24rpx;line-height:1.7;margin-top:20rpx}.footer{text-align:center;font-size:23rpx}button[disabled]{opacity:.55}
</style>
