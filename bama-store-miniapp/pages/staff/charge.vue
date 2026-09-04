<template>
  <view class="page">
    <!-- 会员信息 -->
    <view class="member card">
      <view class="avatar">{{ (member.name || '会').charAt(0) }}</view>
      <view class="info">
        <text class="nm">{{ member.name }} <text class="lv">{{ levelText }}</text></text>
        <text class="bl">会员卡余额 <text class="b mono">¥{{ fmt(member.balance) }}</text></text>
      </view>
    </view>

    <!-- 消费明细 -->
    <view class="row-head flex-between">
      <text class="h">消费明细</text>
      <view class="add" @tap="addItem">＋ 添加</view>
    </view>
    <view class="list card">
      <view v-for="(it, i) in items" :key="i" class="li">
        <input v-model="it.itemName" class="iname" placeholder="项目名称" />
        <input v-model.number="it.price" class="iprice mono" type="digit" placeholder="单价" />
        <text class="x">×</text>
        <input v-model.number="it.quantity" class="iqty mono" type="number" />
        <text class="del" @tap="removeItem(i)">✕</text>
      </view>
    </view>

    <!-- 快捷添加 -->
    <view class="quick">
      <text class="q" @tap="quickAdd('ROOM', '观山茶室 1小时', 188)">+茶室</text>
      <text class="q" @tap="quickAdd('PRODUCT', '大红袍 125g', 288)">+大红袍</text>
      <text class="q" @tap="quickAdd('PRODUCT', '金骏眉 100g', 439)">+金骏眉</text>
    </view>

    <!-- 金额 -->
    <view class="amount card">
      <view class="flex-between line1">
        <text>原价合计</text>
        <text class="mono">¥{{ fmt(origin) }}</text>
      </view>
      <view class="flex-between">
        <text class="big-l">应扣金额（{{ discount }} 折后）</text>
        <text class="big mono">¥{{ fmt(payEstimate) }}</text>
      </view>
    </view>

    <view class="btn-primary confirm" :class="{ disabled: submitting }" @tap="confirm">
      {{ submitting ? '扣款中...' : '确认扣款 · 扣会员卡' }}
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'

export default {
  data() {
    return {
      member: {},
      items: [{ itemType: 'PRODUCT', itemName: '', price: 0, quantity: 1 }],
      submitting: false
    }
  },
  onLoad() {
    this.member = uni.getStorageSync('chargeMember') || {}
  },
  computed: {
    discount() {
      const d = this.member.discount || 100
      return (d / 10).toFixed(d % 10 === 0 ? 0 : 1)
    },
    origin() {
      return this.items.reduce((s, it) => s + (Number(it.price) || 0) * (Number(it.quantity) || 0), 0)
    },
    payEstimate() {
      return (this.origin * (this.member.discount || 100)) / 100
    },
    levelText() {
      const map = { NORMAL: '普通', GOLD: '金卡', BLACK_GOLD: '黑金' }
      return map[this.member.level] || ''
    }
  },
  methods: {
    fmt(n) { return Number(n || 0).toFixed(2) },
    addItem() {
      this.items.push({ itemType: 'PRODUCT', itemName: '', price: 0, quantity: 1 })
    },
    quickAdd(type, name, price) {
      this.items.push({ itemType: type, itemName: name, price, quantity: 1 })
    },
    removeItem(i) {
      this.items.splice(i, 1)
    },
    async confirm() {
      const valid = this.items.filter((it) => it.itemName && it.price > 0)
      if (valid.length === 0) {
        uni.showToast({ title: '请填写消费明细', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        const res = await api.chargeConfirm({
          memberId: this.member.memberId,
          items: valid,
          remark: '门店扫码消费'
        })
        uni.showModal({
          title: '扣款成功',
          content: `扣款 ¥${this.fmt(res.payAmount)}\n卡内余额 ¥${this.fmt(res.balanceAfter)}\n单号 ${res.orderNo}`,
          showCancel: false,
          confirmText: '完成',
          success: () => {
            uni.removeStorageSync('chargeMember')
            uni.reLaunch({ url: '/pages/staff/workbench' })
          }
        })
      } catch (e) {
        // 余额不足等错误已统一提示
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.member { display: flex; align-items: center; padding: 28rpx; }
.avatar {
  width: 92rpx; height: 92rpx; border-radius: 24rpx; background: linear-gradient(135deg, $gold, $brand);
  color: #fff; font-family: serif; font-size: 40rpx; display: flex; align-items: center; justify-content: center; margin-right: 22rpx;
}
.info .nm { font-size: 34rpx; font-weight: 600; }
.info .lv {
  font-size: 20rpx; color: $brand; background: #FBF2F1; border: 2rpx solid #E9C9C6;
  padding: 2rpx 14rpx; border-radius: 999rpx; margin-left: 10rpx;
}
.info .bl { display: block; font-size: 24rpx; color: $muted; margin-top: 8rpx; }
.info .b { color: $green; font-weight: 600; }

.row-head { margin: 32rpx 0 18rpx; }
.row-head .h { font-size: 30rpx; font-weight: 600; }
.row-head .add { color: $brand; font-size: 26rpx; }
.list { overflow: hidden; }
.li { display: flex; align-items: center; padding: 20rpx 24rpx; border-bottom: 2rpx solid $line; }
.li:last-child { border-bottom: none; }
.iname { flex: 1; font-size: 26rpx; }
.iprice { width: 120rpx; text-align: right; font-size: 26rpx; color: $brand; }
.x { margin: 0 12rpx; color: $muted; }
.iqty { width: 70rpx; text-align: center; font-size: 26rpx; }
.del { margin-left: 16rpx; color: $muted; font-size: 26rpx; }

.quick { display: flex; gap: 16rpx; margin-top: 20rpx; }
.quick .q {
  font-size: 24rpx; color: $ink; background: #fff; border: 2rpx solid $line;
  padding: 12rpx 26rpx; border-radius: 999rpx;
}

.amount { padding: 26rpx 28rpx; margin-top: 26rpx; }
.amount .line1 { color: $muted; font-size: 26rpx; margin-bottom: 16rpx; }
.amount .big-l { font-size: 26rpx; }
.amount .big { color: $brand; font-size: 44rpx; font-weight: 600; }

.confirm { margin-top: 40rpx; padding: 30rpx; font-size: 32rpx; font-weight: 600; }
.confirm.disabled { opacity: 0.6; }
</style>
