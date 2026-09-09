<template>
  <view class="page">
    <view class="logo">
      <view class="seal">馬</view>
      <text class="title">八马员工端</text>
      <text class="sub">扫码收款 · 核销 · 储值</text>
    </view>

    <button class="wechat-login" :loading="loading" :disabled="loading" @tap="wechat">微信登录员工端</button>
    <view v-if="bindTicket" class="binding-note">首次微信登录，请填写已有员工账号密码完成绑定</view>
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
        {{ loading ? '处理中…' : bindTicket ? '验证并绑定微信' : '登 录' }}
      </view>
    </view>

    <text class="staff-note">使用门店管理员分配的员工账号登录</text>
    <button class="customer-return" @tap="backToCustomer">返回顾客首页</button>
  </view>
</template>

<script>
import api from '@/common/api.js'
import { wechatLogin, bindWechat } from '@/common/wechat.js'
import { auth } from '@/common/store.js'
import { parseStaffScene } from '@/common/staff-bind.mjs'

export default {
  data() {
    return {
      form: { phone: '', password: '' },
      bindTicket: '', loading: false, bindingRedirect: false
    }
  },
  onLoad(options) { this.openBinding(options?.scene) },
  onShow() { this.openBinding(uni.getStorageSync('pending_staff_scene')?.scene) },
  methods: {
    openBinding(scene) {
      if(this.bindingRedirect || !parseStaffScene(scene))return
      this.bindingRedirect=true
      uni.redirectTo({url:'/pages/staff/bind-wechat?scene='+encodeURIComponent(scene),fail:()=>{this.bindingRedirect=false}})
    },
    backToCustomer(){auth.switchToCustomer()},
    async wechat() { if(this.loading)return;this.loading=true;try{const result=await wechatLogin('STAFF');if(result.bindRequired){this.bindTicket=result.bindTicket;this.form={phone:'',password:''};return}auth.setLogin(result.account);uni.reLaunch({url:'/pages/staff/workbench'})}catch(e){uni.showToast({title:e.message||'微信登录失败',icon:'none'})}finally{this.loading=false}},
    fill(phone, password) {
      this.form.phone = phone
      this.form.password = password
    },
    async submit() {
      if(this.loading)return
      if (!this.form.phone || !this.form.password) {
        uni.showToast({ title: '请输入账号和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const res = this.bindTicket ? (await bindWechat(this.bindTicket,this.form.phone,this.form.password)).account : await api.login(this.form)
        auth.setLogin(res)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/staff/workbench' }), 600)
      } catch (e) {
        this.bindTicket=''
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
.wechat-login{background:#368352;color:white;font-size:28rpx;line-height:96rpx;border-radius:16rpx;margin-bottom:28rpx}.binding-note{font-size:24rpx;color:#a75130;line-height:1.7;margin-bottom:22rpx}</style>

<style scoped>.staff-note{display:block;text-align:center;color:#998778;font-size:23rpx;margin-top:28rpx}.customer-return{background:transparent;color:#9a7461;font-size:24rpx;margin-top:28rpx}</style>
