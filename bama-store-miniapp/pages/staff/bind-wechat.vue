<template>
  <view class="binding-page">
    <view class="card binding-card">
      <text class="heading">绑定员工微信</text>
      <text class="note">输入管理员登记的本人手机号，确认绑定当前微信。绑定后可使用员工快捷登录，无需输入员工密码。</text>
      <text v-if="!ticket" class="error">绑定码无效，请向管理员获取二维码后重新扫码。</text>
      <template v-else>
        <PrivacyConsent ref="privacyConsent" @change="privacyFormAllowed=$event" />
        <input v-model="phone" :disabled="!privacyFormAllowed || loading" type="number" maxlength="11" placeholder="员工登录手机号" />
        <button class="btn-primary" :loading="loading" :disabled="loading" @tap="bind">确认绑定当前微信</button>
      </template>
    </view>
  </view>
</template>
<script>
import PrivacyConsent from '@/components/PrivacyConsent.vue'
import { parseStaffScene, bindStaffWechat } from '@/common/staff-bind.mjs'
import request from '@/common/request.js'
import { auth } from '@/common/store.js'
export default {
  components: { PrivacyConsent },
  data() { return{privacyFormAllowed:false,ticket: null, phone: '', loading: false} },
  onLoad(options) { this.ticket = parseStaffScene(options?.scene) },
  onShow() {
    const pending = uni.getStorageSync('pending_staff_scene')
    if (pending) {
      uni.removeStorageSync('pending_staff_scene')
      this.ticket = parseStaffScene(pending.scene)
      this.phone = ''
    }
  },
  methods: {
    async bind() {
      if (this.loading) return
      this.loading = true
      if (!await this.$refs.privacyConsent.ensure()) { this.loading=false; return }
      try {
        const result = await bindStaffWechat(this.ticket, this.phone.trim(), uni, request)
        this.ticket = null
        auth.setLogin(result.account)
        uni.showToast({title: '微信绑定成功', icon: 'success'})
        uni.reLaunch({url: '/pages/staff/workbench'})
      } catch (e) {
        uni.showToast({title: e.message || '绑定失败，请重试', icon: 'none'})
      } finally { this.loading = false }
    }
  }
}
</script>
<style scoped>
.binding-page{padding:80rpx 36rpx}.binding-card{padding:40rpx}.heading{display:block;font-size:38rpx;font-weight:600}.note{display:block;font-size:26rpx;line-height:1.8;color:#827568;margin:24rpx 0 40rpx}.error{color:#c33c2f;font-size:28rpx}input{height:96rpx;padding:0 24rpx;background:#fff7ef;border-radius:12rpx;margin-bottom:24rpx}.btn-primary{margin-top:32rpx;font-size:30rpx}
</style>
