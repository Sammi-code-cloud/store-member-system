<template>
  <view class="page">
    <view class="masthead" :style="{paddingTop:(statusBarHeight+16)+'px'}"><view class="wordmark"><text class="seal">馬</text><view><text class="brand-name">八马茶业</text><text class="brand-note">一盏好茶 · 一刻闲适</text></view></view></view>
    <view class="content">
      <picker :range="stores" range-key="name" :value="storeIndex" @change="changeStore"><view class="store-card"><view class="store-icon">店</view><view class="store-info"><text class="eyebrow">当前分店</text><text class="store-name">{{ stores[storeIndex]?.name || '暂无营业分店' }}</text></view><text class="store-change">切换 ›</text></view></picker>
      <view v-if="banners.length" class="banner" @touchstart="bannerTouchStart" @touchend="bannerTouchEnd" @touchcancel="startBannerTimer">
        <view v-for="(banner,index) in banners" :key="banner.id" class="banner-slide" :class="{'is-active':index===bannerIndex}" :aria-hidden="index!==bannerIndex">
          <image class="banner-photo" :src="bannerSrc(banner.imageUrl)" :alt="banner.title" mode="aspectFill" @error="bannerFailed(banner.id)" />
          <view class="banner-shade" @tap="openBanner"><text class="banner-title">{{banner.title}}</text><text v-if="banner.target==='rooms'" class="banner-action">预订茶室 ↗</text></view>
        </view>
        <view v-if="banners.length>1" class="banner-dots"><button v-for="(banner,index) in banners" :key="banner.id" class="banner-dot" :class="{'is-active':index===bannerIndex}" :aria-label="'切换到第'+(index+1)+'张 Banner'" :aria-pressed="index===bannerIndex" @tap.stop="selectBanner(index)"><view /></button></view>
        <view v-if="banners.length>1" class="banner-controls"><button class="banner-arrow" aria-label="上一张 Banner" @tap.stop="moveBanner(-1)">‹</button><text>{{bannerIndex+1}} / {{banners.length}}</text><button class="banner-arrow" aria-label="下一张 Banner" @tap.stop="moveBanner(1)">›</button></view>
      </view>
      <view v-if="!banners.length" class="hero" @tap="go('/pages/customer/rooms')"><view class="hero-copy"><text class="hero-kicker">留一段时间，给自己</text><text class="hero-title"><text>好茶相伴</text><text>自在小坐</text></text><text class="hero-sub">寻一处静室，与知己共饮</text><view class="hero-action">预订茶室 <text>↗</text></view></view><view class="tea-art"><view class="halo"/><view class="tea-leaf leaf-one"/><view class="tea-leaf leaf-two"/><view class="saucer"/><view class="cup"><view class="tea"/></view><text class="art-note">闲 · 适</text></view></view>
      <view class="member-panel"><view class="member-top"><text>{{ loggedIn ? '我的会员账户' : '与好茶，初次相逢' }}</text><text class="member-level">{{ loggedIn ? member.levelText : '欢迎加入' }}</text></view><view class="member-bottom"><view v-if="loggedIn"><text class="balance-value">¥ {{ fmt(member.balance) }}</text><text class="balance-note">可用余额 · 各分店共享</text></view><view v-else @tap="go('/pages/customer/login')"><text class="login-label">登录 / 注册 ›</text><text class="balance-note">预订茶室，查看专属账户</text></view><view class="pay-entry" @tap="go(loggedIn ? '/pages/customer/paycode' : '/pages/customer/login')">付款凭证 ›</view></view></view>
      <view class="section-head"><view><text class="section-title">店内好茶</text><text class="section-note">慢慢选，细细品</text></view><text class="section-side">到店选购</text></view>
      <view v-if="!products.length" class="empty-state"><text class="empty-symbol">茶</text><text>好茶正在准备中</text><text class="empty-note">也可以先选一间喜欢的茶室</text></view>
      <view class="product-grid"><view v-for="(p,i) in products" :key="p.id" class="product-card"><view class="product-art" :class="'tone-'+i%3"><image v-if="p.image && !failedImages[p.id]" :src="p.image" mode="aspectFill" class="product-photo" @error="failedImages[p.id] = true"/><view v-else class="tea-tin"><text>八马</text><text class="tin-name">{{(p.category || '茗茶').slice(0,4)}}</text></view><text class="product-badge">店内精选</text></view><view class="product-info"><text class="product-name">{{p.name}}</text><text class="product-spec">{{p.spec || '到店品鉴'}}</text><view class="product-prices"><text class="product-price">¥{{Number(p.memberPrice).toFixed(0)}}</text><text class="retail">¥{{Number(p.retailPrice).toFixed(0)}}</text></view></view></view></view>
      <text class="end-note">一杯茶的时间，刚刚好</text>
    </view><CustomerNav active="home" />
    <view v-if="showWelcome" class="welcome-mask" @touchmove.stop.prevent>
      <view class="welcome-card" role="dialog" aria-modal="true" aria-label="欢迎登录八马茶业">
        <view class="welcome-seal">茶</view>
        <text class="welcome-kicker">八马茶业 · 欢迎到店</text>
        <text class="welcome-title">好茶相逢，从这里开始</text>
        <text class="welcome-description">登录后，即可预订茶室、查看订单，以及各分店共享的会员账户</text>
        <view class="welcome-info"><text class="welcome-info-title">登录信息使用说明</text><text>我们将使用微信身份信息识别你的账户，首次登录会自动建立顾客档案。</text></view>
        <button class="welcome-primary" :loading="welcomeLoading" :disabled="welcomeLoading" @tap="welcomeWechat">微信一键登录 / 注册</button>
        <view class="welcome-links"><button :disabled="welcomeLoading" @tap="welcomeAccount(false)">账号登录</button><text>·</text><button :disabled="welcomeLoading" @tap="welcomeAccount(true)">注册新账号</button></view>
        <button class="welcome-skip" :disabled="welcomeLoading" @tap="showWelcome=false">先逛逛</button>
      </view>
    </view>
  </view>
</template>

<script>
import { auth } from '@/common/store.js'
import api from '@/common/api.js'
import CustomerNav from '@/components/CustomerNav.vue'
import { BASE_URL } from '@/common/request.js'
import { wechatLogin } from '@/common/wechat.js'
import { completeCustomerWechat } from '@/common/customer-wechat.js'
// Only remind once during this app launch, including when returning from login.
let welcomeShown = false
export default {
  components: { CustomerNav },
  data() {
    return {
      statusBarHeight: 20,
      loggedIn: false,
      banners: [], bannerIndex: 0, bannerTimer: null, bannerActive: false, bannerTouchX: 0, bannerTouchY: 0, bannerSkipTapUntil: 0, bannerRequest: 0,
      showWelcome: false, welcomeLoading: false,
      member: { balance: 0, levelText: '会员', discount: 100 },
      stores: [], storeIndex: 0,
      failedImages: {}, products: [],
      reservations: [],
      rooms: [],
      prodGrad: [
        'linear-gradient(120deg,#7E211C,#9E2B25)',
        'linear-gradient(120deg,#675a4d,#877a6d)',
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
  },
  onShow() {
    if(auth.preferStaff()){uni.reLaunch({url:'/pages/staff/workbench'});return}
    this.bannerActive = true
    if (!uni.getStorageSync('customer_token') && !welcomeShown) {
      welcomeShown = true
      this.showWelcome = true
    }
    if (uni.getStorageSync('customer_token')) this.showWelcome = false
    this.loadAll()
  },
  onHide() { this.stopBanners() },
  onUnload() { this.stopBanners() },
  methods: {
    bannerSrc(url) { return typeof window === 'undefined' ? BASE_URL + url : url },
    stopBanners() { this.bannerActive=false; this.bannerRequest++; clearInterval(this.bannerTimer) },
    startBannerTimer() { clearInterval(this.bannerTimer); if(this.bannerActive && this.banners.length>1)this.bannerTimer=setInterval(()=>{if(!this.showWelcome)this.bannerIndex=(this.bannerIndex+1)%this.banners.length},5000) },
    selectBanner(index) { this.bannerIndex=index; this.startBannerTimer() },
    moveBanner(step) { if(this.banners.length)this.selectBanner((this.bannerIndex+step+this.banners.length)%this.banners.length) },
    bannerTouchStart(e) { clearInterval(this.bannerTimer); this.bannerTouchX=e.changedTouches?.[0]?.clientX || 0; this.bannerTouchY=e.changedTouches?.[0]?.clientY || 0 },
    bannerTouchEnd(e) { const dx=(e.changedTouches?.[0]?.clientX || 0)-this.bannerTouchX; const dy=(e.changedTouches?.[0]?.clientY || 0)-this.bannerTouchY; if(Math.abs(dx)>40 && Math.abs(dx)>Math.abs(dy)){this.bannerSkipTapUntil=Date.now()+500;this.moveBanner(dx<0?1:-1)}else this.startBannerTimer() },
    openBanner() { if(Date.now()<this.bannerSkipTapUntil)return; if(this.banners[this.bannerIndex]?.target==='rooms')this.go('/pages/customer/rooms') },
    bannerFailed(id) { this.banners=this.banners.filter(item=>item.id!==id); this.bannerIndex=Math.min(this.bannerIndex,Math.max(0,this.banners.length-1));this.startBannerTimer() },
    welcomeAccount(register) {
      this.showWelcome = false
      this.go('/pages/customer/login' + (register ? '?mode=register' : ''))
    },
    async welcomeWechat() {
      if (this.welcomeLoading) return
      this.welcomeLoading = true
      try {
        const result = await wechatLogin('CUSTOMER')
        if (!completeCustomerWechat(result)) { this.showWelcome = false; return }
        this.showWelcome = false
        await this.loadAll()
      } catch (e) {
        uni.showToast({ title: e.message || '微信登录未完成，请重试', icon: 'none' })
      } finally { this.welcomeLoading = false }
    },
    async changeStore(e) {
      this.storeIndex=Number(e.detail.value)
      const id=this.stores[this.storeIndex].id
      uni.setStorageSync('customer_store_id',id)
      this.products=[]
      this.banners=[]; this.bannerIndex=0; clearInterval(this.bannerTimer)
      const request=++this.bannerRequest
      api.customerBanners(id).then(rows=>{
        if(!this.bannerActive || request!==this.bannerRequest || id!==this.stores[this.storeIndex]?.id)return
        this.banners=rows
        this.startBannerTimer()
      }).catch(()=>{})
      try { const products=await api.customerProducts(id); if(id===this.stores[this.storeIndex]?.id)this.products=products } catch(e){}
    },
    fmt(n) { return Number(n || 0).toFixed(2) },
    go(url) { uni.navigateTo({ url }) },
    async loadAll() {
      this.loggedIn = !!uni.getStorageSync('customer_token')
      if (this.loggedIn) try { this.member = await api.customerHome() } catch (e) { this.loggedIn = false }
      try {
        this.stores=await api.customerStores()
        const saved=Number(uni.getStorageSync('customer_store_id'))
        this.storeIndex=Math.max(0,this.stores.findIndex(s=>s.id===saved))
        if(this.stores.length)await this.changeStore({detail:{value:this.storeIndex}})
        else this.products=[]
      } catch(e) {}
    }
  }
}
</script>

<style scoped>
.page{padding-bottom:160rpx}.masthead{padding:0 36rpx 24rpx}.wordmark{display:flex;align-items:center;gap:18rpx}.seal{width:64rpx;height:64rpx;background:#c33c2f;color:#fff9ea;border-radius:14rpx;display:flex;align-items:center;justify-content:center;font-family:serif;font-size:40rpx}.brand-name{font-family:serif;font-size:36rpx;letter-spacing:4rpx;font-weight:600}.brand-note{display:block;font-size:20rpx;color:#837669;margin-top:6rpx;letter-spacing:3rpx}.content{padding:0 32rpx}.store-card{display:flex;align-items:center;padding:22rpx 24rpx;background:#fffdfa;border:1rpx solid #f0ded0;border-radius:22rpx;margin-bottom:24rpx}.store-icon{width:58rpx;height:58rpx;display:flex;align-items:center;justify-content:center;background:#ffead7;border-radius:16rpx;font-family:serif;font-size:30rpx;margin-right:18rpx}.store-info{flex:1;min-width:0}.eyebrow{font-size:20rpx;color:#837669;display:block;margin-bottom:6rpx}.store-name{font-size:27rpx;font-weight:600;display:block;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.store-change{font-size:23rpx;color:#716457;margin-left:16rpx}.hero{min-height:390rpx;border-radius:28rpx;background:#ffe3c3;position:relative;overflow:hidden;padding:40rpx 32rpx}.hero-copy{position:relative;z-index:2}.hero-kicker{font-size:20rpx;color:#76695c;letter-spacing:3rpx}.hero-title{white-space:pre-line;display:block;font-family:serif;font-size:54rpx;letter-spacing:6rpx;line-height:1.4;margin:14rpx 0}.hero-sub{font-size:22rpx;color:#726558}.hero-action{box-shadow:0 8rpx 20rpx #b5362d18;margin-top:28rpx;display:flex;align-items:center;justify-content:space-between;background:#b5362d;color:#fffdfa;border-radius:12rpx;width:220rpx;padding:20rpx 24rpx;font-size:25rpx}.tea-art{position:absolute;right:-22rpx;bottom:0;width:320rpx;height:360rpx}.halo{position:absolute;inset:20rpx -30rpx 0 0;border:1rpx solid #edbf91;border-radius:50%}.saucer{position:absolute;bottom:65rpx;right:20rpx;width:270rpx;height:125rpx;border-radius:50%;background:#f3c493;box-shadow:0 20rpx 30rpx #e5a66d}.cup{position:absolute;bottom:95rpx;right:50rpx;width:208rpx;height:125rpx;border-radius:15% 15% 48% 48%;background:linear-gradient(90deg,#fdfcf5,#f3ddc4);box-shadow:12rpx 12rpx 20rpx #d89860}.tea{position:absolute;top:-26rpx;left:0;width:208rpx;height:70rpx;border-radius:50%;background:#c87929;border:12rpx solid #faf9ef;box-shadow:inset 0 8rpx 10rpx #a85620}.tea-leaf{position:absolute;width:55rpx;height:130rpx;border-radius:0 100% 0 100%;background:#dc8242;transform:rotate(20deg);right:38rpx;top:0}.leaf-two{right:108rpx;top:28rpx;transform:rotate(-45deg);background:#efae68}.art-note{position:absolute;bottom:22rpx;right:84rpx;letter-spacing:10rpx;font-size:20rpx;color:#7f7265}.member-panel{background:linear-gradient(115deg,#b93229,#d65432);color:#fff9e9;border-radius:24rpx;padding:28rpx;margin-top:24rpx}.member-top,.member-bottom{display:flex;align-items:center;justify-content:space-between;gap:16rpx}.member-top{font-size:23rpx;color:#ffe7d4;margin-bottom:22rpx}.member-level{font-size:19rpx;border:1rpx solid #db8061;border-radius:20rpx;padding:5rpx 14rpx;color:#ffe1ba}.balance-value{font-size:44rpx;font-weight:500;font-variant-numeric:tabular-nums}.balance-note{display:block;font-size:20rpx;color:#ffdcc3;margin-top:9rpx}.login-label{font-size:33rpx}.pay-entry{font-size:23rpx;background:#fff0da;color:#b5362d;padding:18rpx 20rpx;border-radius:12rpx}.section-head{display:flex;justify-content:space-between;align-items:center;margin:40rpx 0 24rpx}.section-title{font-size:33rpx;font-family:serif;font-weight:600;letter-spacing:3rpx}.section-note{display:block;font-size:21rpx;color:#877a6d;margin-top:7rpx}.section-side{font-size:22rpx;color:#877a6d}.product-grid{display:flex;flex-wrap:wrap;gap:20rpx}.product-card{width:calc(50% - 10rpx);background:#fffdfa;border-radius:22rpx;overflow:hidden;border:1rpx solid #f1e4d7}.product-art{height:250rpx;background:#f9e9d5;position:relative;display:flex;justify-content:center;align-items:center}.tone-1{background:#ffe3ce}.tone-2{background:#ffe7dd}.tea-tin{width:130rpx;height:165rpx;background:#af352d;border-radius:12rpx 12rpx 4rpx 4rpx;box-shadow:16rpx 16rpx 25rpx #0002;border-top:10rpx solid #8d2924;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#ecdec3;font-size:21rpx;letter-spacing:4rpx}.tone-1 .tea-tin{background:#d36a32;border-color:#aa4929}.tone-2 .tea-tin{background:#be7c35;border-color:#6d5338}.tin-name{font-family:serif;font-size:27rpx;margin-top:14rpx}.product-badge{position:absolute;top:14rpx;left:14rpx;font-size:18rpx;color:#6a5d50;background:#fff9;padding:6rpx 10rpx;border-radius:6rpx}.product-info{padding:22rpx}.product-name{font-size:27rpx;font-weight:600;display:block;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.product-spec{font-size:21rpx;color:#887b6e;display:block;margin-top:10rpx;min-height:30rpx}.product-prices{display:flex;align-items:baseline;gap:14rpx;margin-top:16rpx}.product-price{font-size:32rpx;color:#c33c2f}.retail{font-size:21rpx;color:#9c8f82;text-decoration:line-through}.empty-state{padding:50rpx;text-align:center;background:#fffdfa;border-radius:22rpx;display:flex;flex-direction:column;gap:14rpx}.empty-symbol{font-size:48rpx;font-family:serif;color:#908376}.empty-note,.end-note{font-size:22rpx;color:#887b6e}.end-note{display:block;text-align:center;letter-spacing:4rpx;margin:40rpx 0}
.hero-title text{display:block}.hero-title{max-width:330rpx}.product-photo{width:100%;height:100%}</style>

<style scoped>
.welcome-mask{position:fixed;inset:0;z-index:80;background:rgba(49,27,21,.48);display:flex;align-items:center;justify-content:center;padding:40rpx;overflow-y:auto}
.welcome-card{width:100%;max-width:640rpx;max-height:90vh;overflow-y:auto;background:linear-gradient(160deg,#fff0dd,#fffdfa 45%);border:1rpx solid #ffe8d0;border-radius:36rpx;padding:42rpx 36rpx 22rpx;text-align:center;box-shadow:0 28rpx 90rpx #391c2426}
.welcome-seal{width:92rpx;height:92rpx;margin:0 auto 24rpx;border-radius:26rpx;background:linear-gradient(135deg,#c13b2e,#e6793e);color:#fff7e9;font-family:serif;font-size:52rpx;display:flex;align-items:center;justify-content:center;box-shadow:0 12rpx 24rpx #c33c2f20}
.welcome-kicker{display:block;font-size:21rpx;letter-spacing:4rpx;color:#ac7355}
.welcome-title{display:block;margin:18rpx 0;font-family:serif;font-size:36rpx;font-weight:600;line-height:1.5;color:#482d24}
.welcome-description{display:block;white-space:pre-line;color:#8b7466;font-size:24rpx;line-height:1.9}
.welcome-info{text-align:left;background:#fff4e9;border:1rpx solid #f6e4d3;border-radius:18rpx;padding:22rpx 24rpx;margin:30rpx 0;color:#907766;font-size:22rpx;line-height:1.8}
.welcome-info-title{display:block;color:#604537;font-size:24rpx;font-weight:500;margin-bottom:6rpx}
.welcome-primary{background:linear-gradient(110deg,#bf392e,#df6737);color:#fff;font-size:28rpx;line-height:92rpx;border-radius:16rpx;margin:0;box-shadow:0 10rpx 24rpx #c33c2f18}
.welcome-links{display:flex;align-items:center;justify-content:center;gap:12rpx;color:#c8ac96;margin-top:14rpx}
.welcome-links button{background:transparent;color:#a44c33;font-size:24rpx;padding:10rpx 12rpx;margin:0;width:auto;line-height:2}
.welcome-skip{background:transparent;color:#9a8577;font-size:23rpx;width:auto;padding:10rpx 28rpx;line-height:2;margin:0 auto}
</style>

<style scoped>
.banner{height:344rpx;position:relative;overflow:hidden;border-radius:28rpx;background:#8b5c39}
.banner-photo{width:100%;height:100%;display:block}
.banner-shade{position:absolute;inset:0;padding:36rpx 30rpx;background:linear-gradient(90deg,#301a1366,transparent 85%);color:#fff8ea;display:flex;flex-direction:column;align-items:flex-start}
.banner-kicker{font-size:19rpx;letter-spacing:3rpx;color:#f2d6ba}
.banner-title{font-family:serif;font-size:38rpx;line-height:1.5;max-width:390rpx;max-height:120rpx;overflow:hidden;margin-top:16rpx;letter-spacing:2rpx;text-shadow:0 2rpx 8rpx #3b201c70}
.banner-action{margin-top:auto;font-size:23rpx;padding:10rpx 18rpx;border:1rpx solid #ffedd580;border-radius:8rpx;background:#51281350}
.banner-controls{position:absolute;bottom:20rpx;right:18rpx;display:flex;align-items:center;gap:10rpx;color:#fff;font-size:20rpx;background:#35241c66;border-radius:30rpx;padding:0 6rpx}
.banner-arrow{background:transparent;color:white;margin:0;width:44rpx;height:48rpx;line-height:44rpx;font-size:34rpx;padding:0}
</style>

<style scoped>
.banner-slide{position:absolute;inset:0;opacity:0;pointer-events:none;transition:opacity .4s ease}
.banner-slide.is-active{opacity:1;pointer-events:auto}
.banner-action{margin-bottom:32rpx}
.banner-dots{position:absolute;bottom:12rpx;left:24rpx;display:flex;align-items:center;gap:0;z-index:2}
.banner-dot{display:flex;align-items:center;justify-content:center;width:36rpx;height:36rpx;margin:0;padding:0;background:transparent;border:0;border-radius:0;line-height:1}
.banner-dot view{width:10rpx;height:10rpx;border-radius:8rpx;background:#ffffff70;transition:width .25s,background .25s}
.banner-dot.is-active view{width:26rpx;background:#fff3dc}
@media(prefers-reduced-motion:reduce){.banner-slide,.banner-dot view{transition:none}}
</style>
