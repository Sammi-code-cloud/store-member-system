<template>
  <view class="settings-page"><text class="title">隐私与授权</text><view class="card"><text class="heading">{{accepted?'已同意本版协议':'尚未同意本版协议'}}</text><text class="note">协议版本：{{version}}。你可以不登录浏览公开的门店、商品和茶室信息。</text><LegalLinks :show-settings="false" /></view><view class="card"><text class="heading">管理个人信息</text><text class="note">运营者：{{operator.name}}</text><text class="note" selectable>隐私咨询、查阅、更正、删除及账号注销申请：{{operator.phone}}</text><text class="note" selectable>通信地址：{{operator.address}}</text><button @tap="call">联系门店</button><text class="note">注销前需核实本人身份并处理余额、未完成预约等事项；依法需要保存的记录按法定要求处理。</text></view><view class="card"><text class="heading">撤回本小程序授权</text><text class="note">撤回会清除本机的顾客和员工登录状态及同意记录，停止后续个人信息请求，可继续公开浏览。撤回不自动删除服务器记录；删除、注销或停止待处理服务请联系门店。</text><button class="revoke" :disabled="!accepted" @tap="revoke">撤回授权并退出登录</button><text class="note">微信通知订阅及平台隐私授权请在微信提供的设置入口中管理。</text><button @tap="nativePolicy">查看微信隐私保护指引</button></view></view>
</template>
<script>
import LegalLinks from '@/components/LegalLinks.vue'
import { legalOperator } from '@/common/legal-content.mjs'
import { PRIVACY_VERSION, hasPrivacyConsent, revokePrivacyConsent } from '@/common/privacy-consent.mjs'
import { clearCustomerBinding } from '@/common/customer-wechat.js'
export default {
 components:{LegalLinks},data(){return{operator:legalOperator,version:PRIVACY_VERSION,accepted:false}},
 onShow(){this.accepted=hasPrivacyConsent(uni)},
 methods:{
  call(){if(this.operator.phone)uni.makePhoneCall({phoneNumber:this.operator.phone})},
  nativePolicy(){if(typeof wx!=='undefined' && wx.openPrivacyContract)wx.openPrivacyContract({fail:()=>uni.showToast({title:'请在微信设置中查看隐私指引',icon:'none'})});else uni.showToast({title:'请在手机微信中查看',icon:'none'})},
  revoke(){uni.showModal({title:'撤回授权',content:'将退出本机全部顾客和员工账号，停止后续个人信息请求。服务器历史记录不会自动删除，删除或注销请联系门店。确认撤回？',success:result=>{if(!result.confirm)return;revokePrivacyConsent(uni);clearCustomerBinding();this.accepted=false;uni.reLaunch({url:'/pages/customer/home'})}})}
 }
}
</script>
<style scoped>.settings-page{padding:40rpx 32rpx 80rpx;background:#fff7ef;min-height:100vh;color:#423528}.title{display:block;font-size:40rpx;font-weight:600;margin-bottom:32rpx}.card{padding:30rpx;margin-bottom:26rpx}.heading{display:block;font-size:29rpx;font-weight:600}.note{display:block;font-size:24rpx;line-height:1.9;color:#827568;margin:18rpx 0}.card button{font-size:26rpx;background:#f5eee5;color:#80644c;margin-top:24rpx}.card button.revoke{color:#aa392e}.card button[disabled]{opacity:.5}</style>
