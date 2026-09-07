<script>
import { rememberScan } from './common/scan-entry.mjs'
export default {
  onShow(options) { rememberScan(options, uni) },
  onLaunch() {
    console.log('八马门店小程序启动')
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
