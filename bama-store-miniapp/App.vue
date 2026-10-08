<script>
import { hasPrivacyConsent, clearPrivateSessions } from './common/privacy-consent.mjs'
import { rememberScan } from './common/scan-entry.mjs'
import { parseStaffScene } from './common/staff-bind.mjs'
export default {
  onShow(options) {
    rememberScan(options, uni)
    if(options?.path==='pages/staff/login' && options.query?.scene){let scene='';try{scene=decodeURIComponent(options.query.scene)}catch{};if(/^l=[A-Za-z0-9_-]{22}$/.test(scene))uni.setStorageSync('pending_desktop_scene',scene)}
    if(options?.path==='pages/customer/login' && options.query?.scene)uni.setStorageSync('pending_member_scene',{scene:options.query.scene})
    if (['pages/staff/login','pages/staff/bind-wechat'].includes(options?.path) && parseStaffScene(options.query?.scene)) uni.setStorageSync('pending_staff_scene', {scene: options.query.scene})
  },
  onLaunch() {
    // Older builds had no consent record; never reuse those sessions silently.
    if (!hasPrivacyConsent(uni)) clearPrivateSessions(uni)
    else if (!hasPrivacyConsent(uni, true)) { uni.removeStorageSync('customer_token'); uni.removeStorageSync('customer_user') }
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #fff7ef;
  color: #422b24;
  font-size: 28rpx;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
}

view, text {
  box-sizing: border-box;
}

/* 通用工具类 */
.share-entry{margin:24rpx 0;padding:20rpx 24rpx;background:#fff3e4;color:#9b3d2e;border:1rpx solid #eed5bd;border-radius:18rpx;font-size:25rpx;line-height:1.6;text-align:center}
.flex { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.brand-bg { background: linear-gradient(135deg, #b5362d, #8e2825); }
.card {
  background: #fff;
  border-radius: 24rpx;
  border: 1rpx solid #f0ded0;
}
.btn-primary {
  background: #c33c2f;
  color: #fff;
  border-radius: 20rpx;
  text-align: center;
}
.mono { font-variant-numeric: tabular-nums; letter-spacing: 1rpx; }
button::after{border:0}button{font-family:inherit}button[disabled]{opacity:.5} .btn-primary{min-height:88rpx;display:flex;align-items:center;justify-content:center} image{display:block} </style>
