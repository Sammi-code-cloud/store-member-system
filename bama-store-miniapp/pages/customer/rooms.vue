<template>
  <view class="page">
    <!-- 选择日期 -->
    <view class="row-head">选择日期</view>
    <scroll-view class="datechips" scroll-x>
      <view
        v-for="d in dates"
        :key="d.date"
        class="datechip"
        :class="{ sel: selDate === d.date }"
        @tap="selectDate(d.date)"
      >
        <text class="lbl">{{ d.label }}</text>
        <text class="md">{{ d.md }}</text>
      </view>
    </scroll-view>

    <!-- 茶室 + 时段 -->
    <view v-if="loading" class="tip">加载中...</view>
    <view v-for="(r, i) in rooms" :key="r.id" class="room card">
      <view class="rimg" :class="{ g2: i % 3 === 1, g3: i % 3 === 2 }">
        <text class="pill">{{ r.capacity }} · {{ r.roomType }}</text>
      </view>
      <view class="rmeta">
        <view class="flex-between">
          <text class="rn">{{ r.name }}</text>
          <text class="price">¥{{ Number(r.priceHour).toFixed(0) }}<text class="unit">/时</text></text>
        </view>
        <text class="rslot-title">选择时段（2小时）</text>
        <view class="slots">
          <view
            v-for="s in (slotMap[r.id] || [])"
            :key="s.time"
            class="slot"
            :class="{ off: !s.available, sel: pick[r.id] === s.time }"
            @tap="s.available && pickSlot(r.id, s.time)"
          >{{ s.time }}</view>
        </view>
        <view class="btn-primary book" :class="{ disabled: !pick[r.id] }" @tap="reserve(r)">
          {{ pick[r.id] ? ('确认预定 ' + pick[r.id]) : '请选择时段' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import api from '@/common/api.js'

// 演示：固定会员ID=1（正式环境用登录态）
const MEMBER_ID = 1

export default {
  data() {
    return {
      dates: [],
      selDate: '',
      rooms: [],
      slotMap: {}, // roomId -> [{time, available}]
      pick: {},    // roomId -> time
      loading: false
    }
  },
  onLoad() {
    this.buildDates()
    this.loadRooms()
  },
  methods: {
    fmt(d) {
      const p = (n) => (n < 10 ? '0' + n : '' + n)
      return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
    },
    buildDates() {
      const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const today = new Date()
      const arr = []
      for (let i = 0; i < 5; i++) {
        const d = new Date(today)
        d.setDate(today.getDate() + i)
        arr.push({
          date: this.fmt(d),
          label: i === 0 ? '今天' : i === 1 ? '明天' : week[d.getDay()],
          md: d.getMonth() + 1 + '/' + d.getDate()
        })
      }
      this.dates = arr
      this.selDate = arr[0].date
    },
    async loadRooms() {
      this.loading = true
      try {
        this.rooms = await api.customerRooms()
        await this.loadSlots()
      } catch (e) {
      } finally {
        this.loading = false
      }
    },
    async loadSlots() {
      const map = {}
      for (const r of this.rooms) {
        try {
          map[r.id] = await api.customerSlots(r.id, this.selDate)
        } catch (e) {
          map[r.id] = []
        }
      }
      this.slotMap = map
    },
    selectDate(date) {
      this.selDate = date
      this.pick = {}
      this.loadSlots()
    },
    pickSlot(roomId, time) {
      this.pick = { ...this.pick, [roomId]: time }
    },
    async reserve(r) {
      const time = this.pick[r.id]
      if (!time) {
        uni.showToast({ title: '请选择时段', icon: 'none' })
        return
      }
      try {
        await api.customerReserve({
          memberId: MEMBER_ID,
          roomId: r.id,
          reserveDate: this.selDate,
          startTime: time,
          hours: 2
        })
        uni.showToast({ title: '预定成功', icon: 'success' })
        delete this.pick[r.id]
        this.loadSlots()
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 32rpx; }
.row-head { font-size: 30rpx; font-weight: 600; margin: 4rpx 2rpx 16rpx; }
.datechips { white-space: nowrap; margin-bottom: 20rpx; }
.datechip {
  display: inline-block; min-width: 120rpx; padding: 16rpx 12rpx; margin-right: 14rpx;
  border: 2rpx solid $line; border-radius: 16rpx; background: #fff; text-align: center;
}
.datechip.sel { border-color: $brand; background: #FBF2F1; }
.datechip .lbl { font-size: 22rpx; color: $muted; display: block; }
.datechip.sel .lbl { color: $brand; }
.datechip .md { font-size: 30rpx; font-weight: 600; display: block; margin-top: 4rpx; }
.datechip.sel .md { color: $brand; }
.tip { text-align: center; color: $muted; font-size: 24rpx; padding: 30rpx; }

.room { overflow: hidden; margin-bottom: 24rpx; }
.rimg { height: 180rpx; background: linear-gradient(120deg, #8a6f4e, #b79968); position: relative; }
.rimg.g2 { background: linear-gradient(120deg, #586b4f, #7d9268); }
.rimg.g3 { background: linear-gradient(120deg, #6b5a8a, #9E2B25); }
.rimg .pill {
  position: absolute; left: 18rpx; top: 18rpx; background: rgba(0,0,0,0.42);
  color: #fff; font-size: 20rpx; padding: 6rpx 16rpx; border-radius: 999rpx;
}
.rmeta { padding: 22rpx; }
.rn { font-size: 32rpx; font-weight: 600; }
.price { color: $brand; font-weight: 700; font-size: 32rpx; }
.price .unit { font-size: 20rpx; font-weight: 400; color: $muted; }
.rslot-title { display: block; font-size: 24rpx; color: $muted; margin: 18rpx 0 10rpx; }
.slots { display: flex; flex-wrap: wrap; gap: 16rpx; }
.slot {
  padding: 14rpx 28rpx; border: 2rpx solid $line; border-radius: 12rpx;
  font-size: 26rpx; background: #fff; color: $ink;
}
.slot.sel { border-color: $brand; background: #FBF2F1; color: $brand; font-weight: 600; }
.slot.off { opacity: 0.4; text-decoration: line-through; }
.book { margin-top: 22rpx; padding: 22rpx; font-size: 28rpx; font-weight: 600; text-align: center; }
.book.disabled { opacity: 0.45; }
</style>
