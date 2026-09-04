<template>
  <view class="page">
    <!-- 自定义头部 + 余额 -->
    <view class="header brand-bg">
      <view class="status" :style="{ height: statusBarHeight + 'px' }"></view>
      <view class="brandbar">
        <view class="logo"><view class="seal">馬</view><text class="bname">八马茶业</text></view>
        <text class="loc">📍 五缘湾旗舰店 ▾</text>
      </view>
      <view class="balance">
        <text class="k">会员卡余额</text>
        <text class="amt mono">¥ {{ fmt(member.balance) }}</text>
        <text class="lv">{{ member.levelText }} · 享 {{ discount }} 折</text>
      </view>
    </view>

    <view class="wrap">
      <!-- 醒目付款码入口 -->
      <view class="paycode-entry" @tap="go('/pages/customer/paycode')">
        <view class="pe-left">
          <view class="pe-ic">📷</view>
          <view><text class="pe-t">会员付款码</text><text class="pe-s">向店员出示，扫码即扣会员卡</text></view>
        </view>
        <text class="pe-arrow">出示 ›</text>
      </view>

      <!-- 活动精选商品 -->
      <view class="row-head"><text class="h">活动精选</text><text class="m">会员专享价</text></view>
      <scroll-view class="prods" scroll-x>
        <view v-for="(p, i) in products" :key="p.id" class="prod">
          <view class="pimg" :style="{ background: prodGrad[i % prodGrad.length] }">
            <text class="bd">活动</text><text class="pn">{{ p.name }}</text>
          </view>
          <view class="pm">
            <view><text class="pp">¥{{ Number(p.memberPrice).toFixed(0) }}</text><text class="po">¥{{ Number(p.retailPrice).toFixed(0) }}</text></view>
            <text class="spec">{{ p.spec }}</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'

const MEMBER_ID = 1

export default {
  data() {
    return {
      statusBarHeight: 20,
      member: { balance: 0, levelText: '会员', discount: 100 },
      products: [],
      reservations: [],
      rooms: [],
      prodGrad: [
        'linear-gradient(120deg,#7E211C,#9E2B25)',
        'linear-gradient(120deg,#586b4f,#7d9268)',
        'linear-gradient(120deg,#8a6f4e,#b79968)',
        'linear-gradient(120deg,#6b5a8a,#9E2B25)'
      ]
    }
  },
  computed: {
    discount() {
      return (this.member.discount || 100) / 10
    }
  },
  onLoad() {
    this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
    this.loadAll()
  },
  methods: {
    fmt(n) { return Number(n || 0).toFixed(2) },
    go(url) { uni.navigateTo({ url }) },
    async loadAll() {
      try { this.member = await api.customerHome(MEMBER_ID) } catch (e) {}
      try { this.products = await api.customerProducts() } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding-bottom: 60rpx; }
.header { padding: 0 32rpx 40rpx; border-radius: 0 0 36rpx 36rpx; }
.brandbar { display: flex; align-items: center; justify-content: space-between; padding: 12rpx 0 20rpx; }
.logo { display: flex; align-items: center; }
.seal { width: 52rpx; height: 52rpx; border-radius: 14rpx; background: rgba(255,255,255,0.16); color: #fff;
  font-family: serif; display: flex; align-items: center; justify-content: center; margin-right: 14rpx; font-size: 30rpx; }
.bname { color: #fff; font-size: 32rpx; font-weight: 600; }
.loc { color: rgba(255,255,255,0.75); font-size: 22rpx; }
.balance .k { color: rgba(255,255,255,0.8); font-size: 24rpx; }
.balance .amt { display: block; color: #fff; font-size: 60rpx; font-weight: 600; margin-top: 10rpx; }
.balance .lv { color: rgba(255,255,255,0.8); font-size: 22rpx; }
.wrap { padding: 32rpx; }

.paycode-entry {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #9E2B25, #7E211C); color: #fff;
  border-radius: 20rpx; padding: 26rpx 28rpx; margin: -70rpx 0 28rpx;
  box-shadow: 0 20rpx 40rpx -22rpx rgba(126,33,28,0.8);
}
.pe-left { display: flex; align-items: center; }
.pe-ic { width: 76rpx; height: 76rpx; border-radius: 20rpx; background: rgba(255,255,255,0.18);
  display: flex; align-items: center; justify-content: center; font-size: 38rpx; margin-right: 20rpx; }
.pe-t { font-size: 32rpx; font-weight: 600; }
.pe-s { display: block; font-size: 22rpx; opacity: 0.85; margin-top: 4rpx; }
.pe-arrow { background: rgba(255,255,255,0.2); border-radius: 999rpx; padding: 12rpx 26rpx; font-size: 26rpx; font-weight: 600; }

.row-head { display: flex; align-items: center; justify-content: space-between; margin: 8rpx 2rpx 16rpx; }
.row-head .h { font-size: 32rpx; font-weight: 600; }
.row-head .m { font-size: 24rpx; color: $muted; }

.prods { white-space: nowrap; margin-bottom: 16rpx; }
.prod { display: inline-block; width: 300rpx; margin-right: 20rpx; background: #fff; border: 1rpx solid $line;
  border-radius: 18rpx; overflow: hidden; vertical-align: top; }
.pimg { height: 220rpx; position: relative; display: flex; align-items: flex-end; padding: 18rpx; }
.pimg .pn { color: #fff; font-size: 26rpx; font-weight: 600; text-shadow: 0 2rpx 8rpx rgba(0,0,0,0.45); white-space: normal; line-height: 1.3; }
.pimg .bd { position: absolute; top: 16rpx; left: 16rpx; background: $gold; color: #fff; font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 999rpx; }
.pm { padding: 16rpx 18rpx; }
.pm .pp { color: $brand; font-weight: 700; font-size: 30rpx; }
.pm .po { color: $muted; font-size: 20rpx; text-decoration: line-through; margin-left: 8rpx; }
.pm .spec { display: block; font-size: 20rpx; color: $muted; margin-top: 6rpx; }

.empty { color: $muted; font-size: 24rpx; padding: 6rpx 2rpx 20rpx; }
.res { display: flex; align-items: center; background: #fff; border: 1rpx solid $line; border-radius: 18rpx; padding: 22rpx 24rpx; margin-bottom: 16rpx; }
.res .rd { width: 96rpx; text-align: center; }
.res .rd .rt { color: $brand; font-weight: 700; font-size: 30rpx; }
.res .rd .rday { display: block; font-size: 20rpx; color: $muted; margin-top: 4rpx; }
.res .rinfo { flex: 1; padding-left: 12rpx; }
.res .rinfo .n { font-size: 28rpx; font-weight: 600; }
.res .rinfo .s { display: block; font-size: 22rpx; color: $muted; margin-top: 4rpx; }
.res .rstatus { font-size: 20rpx; padding: 6rpx 16rpx; border-radius: 999rpx; font-weight: 600; }
.res .rstatus.WAITING { background: #FDF3E3; color: #C98A2E; }
.res .rstatus.VERIFIED { background: #EDF3EA; color: $green; }
.res .rstatus.USING { background: #FBF2F1; color: $brand; }
.res .rstatus.CANCELLED { background: #eee; color: $muted; }

.room { overflow: hidden; margin-bottom: 20rpx; }
.rimg { height: 180rpx; background: linear-gradient(120deg, #8a6f4e, #b79968); position: relative; }
.rimg.g2 { background: linear-gradient(120deg, #586b4f, #7d9268); }
.rimg.g3 { background: linear-gradient(120deg, #6b5a8a, #9E2B25); }
.rimg .pill { position: absolute; left: 18rpx; top: 18rpx; background: rgba(0,0,0,0.42); color: #fff;
  font-size: 20rpx; padding: 6rpx 16rpx; border-radius: 999rpx; }
.rmeta { padding: 20rpx 22rpx; }
.rn { font-size: 30rpx; font-weight: 600; }
.price { color: $brand; font-weight: 700; font-size: 32rpx; }
.price .unit { font-size: 20rpx; font-weight: 400; color: $muted; }
</style>
