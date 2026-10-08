<template>
  <view class="consent">
    <checkbox-group @change="agreed = $event.detail.value.includes('agree')">
      <view class="agreement-row">
        <label class="check-label"><checkbox value="agree" :checked="agreed" color="#b5362d" /><text>已阅读并同意</text></label>
        <view class="agreement-links"><text @tap.stop="open('service')">《用户服务协议》</text><text @tap.stop="open('privacy')">《隐私政策》</text></view>
      </view>
    </checkbox-group>
    <view v-if="member" class="financial-row">
      <checkbox-group @change="financial = $event.detail.value.includes('financial')">
        <label class="check-label"><checkbox value="financial" :checked="financial" color="#b5362d" /><text>单独同意处理会员余额与交易记录</text></label>
      </checkbox-group>
      <text class="detail-link" @tap="detailsOpen = !detailsOpen">{{detailsOpen?'收起':'说明'}}</text>
    </view>
    <view v-if="detailsOpen" class="disclosure">
      <text class="disclosure-title">信息使用说明</text>
      <text class="purpose">微信标识、你提交的手机号等用于身份验证和门店服务。</text>
      <text v-if="member" class="purpose">会员余额和交易记录属于敏感资金信息，用于会员账户核验、储值记账、消费扣款和查询对账。泄露或不当使用可能影响交易隐私及财产安全。不同意时，可继续浏览公开信息。</text>
      <text class="policy-link" @tap="open('privacy')">查看完整隐私政策 ›</text>
    </view>
    <view class="consent-footer"><text class="detail-link" @tap="detailsOpen = !detailsOpen">{{detailsOpen?'收起使用说明':'信息使用说明'}}</text><text class="browse" @tap="browse">暂不同意，继续浏览 ›</text></view>
    <view v-if="nativePending" class="privacy-mask">
      <view class="privacy-dialog"><text class="dialog-title">微信隐私授权</text><text class="purpose">请查看本小程序在微信平台登记的隐私保护指引，确认后继续。</text><text class="native-link" @tap="openNative">查看微信隐私保护指引</text><button id="privacy-agree" open-type="agreePrivacyAuthorization" @agreeprivacyauthorization="nativeAgree">同意并继续</button><button class="decline" @tap="nativeDecline">暂不同意</button></view>
    </view>
  </view>
</template>
<script>
import { hasPrivacyConsent, savePrivacyConsent } from '@/common/privacy-consent.mjs'
import { legalReady } from '@/common/legal-content.mjs'
export default {
  props: { member: { type: Boolean, default: false } },
  data() { return { agreed: hasPrivacyConsent(uni), financial: hasPrivacyConsent(uni, true), detailsOpen: false, nativePending: false, nativeResolve: null } },
  emits: ['change'],
  mounted() { this.emitAllowed() },
  watch: { agreed() { this.emitAllowed() }, financial() { this.emitAllowed() } },
  beforeUnmount() { this.nativeDecline() },
  methods: {
    emitAllowed() { this.$emit('change', this.agreed && (!this.member || this.financial)) },
    open(kind) { uni.navigateTo({ url: '/pages/legal/document?kind=' + kind }) },
    browse() { uni.reLaunch({ url: '/pages/customer/home' }) },
    openNative() { if (typeof wx !== 'undefined' && wx.openPrivacyContract) wx.openPrivacyContract({ fail: () => uni.showToast({title:'隐私指引暂不可用，请联系门店',icon:'none'}) }) },
    nativeAgree() { this.finishNative(true) },
    nativeDecline() { this.finishNative(false) },
    finishNative(allowed) { const resolve = this.nativeResolve; this.nativeResolve = null; this.nativePending = false; if (resolve) resolve(allowed) },
    async ensure() {
      if (!legalReady) { uni.showToast({title:'运营及隐私联系方式尚未配置',icon:'none'}); return false }
      if (!this.agreed || (this.member && !this.financial)) { uni.showToast({title:'请阅读协议并主动勾选同意',icon:'none'}); return false }
      // No login code, phone request or native data API is invoked before this succeeds.
      let nativeAuthorized = true
      if (typeof wx !== 'undefined') {
        if (typeof wx.getPrivacySetting !== 'function') { uni.showToast({title:'请更新微信后重试',icon:'none'}); return false }
        try {
          const settings = await new Promise((resolve, reject) => wx.getPrivacySetting({success:resolve,fail:reject}))
          if (settings.needAuthorization) {
            if (this.nativeResolve) return false
            nativeAuthorized = await new Promise(resolve => { this.nativeResolve = resolve; this.nativePending = true })
          }
        } catch { uni.showToast({title:'隐私授权检查失败，请重试',icon:'none'}); return false }
      }
      if (!nativeAuthorized || !this.agreed || (this.member && !this.financial)) return false
      try {
        if (!hasPrivacyConsent(uni, this.member)) savePrivacyConsent(uni, {agreed:this.agreed,member:this.member,financial:this.financial,nativeAuthorized})
        return true
      } catch { uni.showToast({title:'授权状态无法保存，请重试',icon:'none'}); return false }
    }
  }
}
</script>
<style scoped>
.consent{margin:4rpx 0 20rpx;color:#857568;font-size:24rpx;line-height:1.7}
.agreement-row,.financial-row{display:flex;align-items:center;flex-wrap:wrap;min-height:62rpx;gap:0 8rpx}
.check-label{display:flex;align-items:center;min-height:62rpx;gap:8rpx;color:#78695c}
.check-label checkbox{flex-shrink:0;transform:scale(.65);transform-origin:center;width:40rpx}
.agreement-links{display:flex;flex-wrap:wrap;color:#a75140}.agreement-links text{padding:10rpx 0}
.financial-row{margin-top:2rpx}.financial-row checkbox-group{flex:1;min-width:0}.financial-row .check-label text{flex:1}
.detail-link{color:#ad8871;font-size:22rpx;padding:14rpx 0}.financial-row>.detail-link{flex-shrink:0;padding-left:12rpx}
.consent-footer{display:flex;align-items:center;justify-content:space-between;gap:16rpx;margin-top:8rpx}
.browse{font-size:22rpx;color:#a39283;padding:14rpx 0}
.disclosure{margin:14rpx 0 4rpx;padding:22rpx 24rpx;background:#f8f3ed;border-radius:14rpx;color:#8b7b6d}
.disclosure-title{display:block;color:#766354;font-size:24rpx;font-weight:500;margin-bottom:10rpx}
.purpose{display:block;font-size:23rpx;line-height:1.9}.disclosure .purpose+.purpose{margin-top:12rpx}
.policy-link{display:block;padding-top:14rpx;color:#a75140;font-size:23rpx}
.privacy-mask{position:fixed;z-index:1000;inset:0;background:#0008;display:flex;align-items:center;justify-content:center;padding:40rpx}
.privacy-dialog{background:#fffdfa;padding:36rpx;border-radius:24rpx;width:100%;max-width:620rpx}.dialog-title{display:block;font-size:34rpx;color:#423528;margin-bottom:20rpx}
.native-link{display:block;color:#9f352b;padding:20rpx 0}.privacy-dialog button{background:#b5362d;color:white;font-size:28rpx;margin-top:16rpx}.privacy-dialog .decline{background:#f5eee5;color:#756457}
</style>
