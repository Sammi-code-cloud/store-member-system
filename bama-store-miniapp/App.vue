<script>
export default {
  onLaunch() {
    console.log('八马门店小程序启动')
  }
}
</script>

<style>
/* 全局样式 */
page {
  background-color: #F3EDE4;
  color: #241C19;
  font-size: 28rpx;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
}

view, text {
  box-sizing: border-box;
}

/* 通用工具类 */
.flex { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.brand-bg { background: linear-gradient(135deg, #9E2B25, #7E211C); }
.card {
  background: #fff;
  border-radius: 24rpx;
  border: 1rpx solid #E6DCCE;
}
.btn-primary {
  background: #9E2B25;
  color: #fff;
  border-radius: 20rpx;
  text-align: center;
}
.mono { font-variant-numeric: tabular-nums; letter-spacing: 1rpx; }
</style>
