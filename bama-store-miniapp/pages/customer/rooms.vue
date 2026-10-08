<template>
 <view class="page">
  <view class="page-heading"><text class="page-title">预订茶室</text><text class="page-subtitle">选一间喜欢的，坐一会儿。</text></view>
  <picker :range="stores" range-key="name" :value="storeIndex" @change="changeStore" :disabled="loading || submitting"><view class="branch-card"><view class="store-mark">店</view><view class="branch-copy"><text class="branch-name">{{stores[storeIndex]?.name || '暂无营业分店'}}</text><text class="branch-address">{{stores[storeIndex]?.address || '选择您要前往的分店'}}</text></view><text class="branch-switch">切换 ›</text></view></picker>
  <view v-if="bookingNoticeIds.length && bookingNoticeSupported" class="booking-notice">
   <view class="notice-main">
    <view class="notice-icon" aria-hidden="true"><view class="notice-bell"/></view>
    <view class="notice-copy"><text class="notice-title">预约成功提醒</text><text class="notice-description">店员确认后，微信及时提醒</text></view>
    <button class="notice-subscribe" :disabled="bookingNoticeBusy || submitting" :loading="bookingNoticeBusy" @tap="subscribeBookingNotice">{{bookingNoticeBusy ? '订阅中' : '订阅提醒'}}</button>
   </view>
   <text class="notice-hint">自愿订阅，不影响预约 · 次数用完可再次订阅</text>
   <view v-if="bookingNoticeFailure" class="notice-failure"><text>{{bookingNoticeFailure.message}}</text><button v-if="bookingNoticeFailure.settings" class="notice-settings" @tap="openBookingNoticeSettings">打开通知设置 ›</button></view>
  </view>
  <view class="section-title"><text>到店日期</text><text class="section-hint">可预约未来 5 天</text></view>
  <scroll-view class="datechips" scroll-x><view v-for="d in dates" :key="d.date" class="datechip" :class="{sel:selDate===d.date}" @tap="selectDate(d.date)"><text class="date-label">{{d.label}}</text><text class="date-number">{{d.md}}</text><view class="date-dot"/></view></scroll-view>
  <view class="section-title"><text>选择包间</text><text class="section-hint">{{rooms.length}} 间包间</text></view>
  <view v-if="loading" class="empty">正在查询可订时段…</view>
  <view v-else-if="!rooms.length" class="empty">该分店暂无可预订包间</view>
  <view v-for="(r,i) in rooms" :key="r.id" class="room" :class="{expanded:activeRoom===r.id}">
   <view class="room-head" @tap="openRoom(r)"><view class="thumbnail"><image v-if="r.image" :src="r.image" mode="aspectFill"/><view v-else class="room-placeholder"><text class="room-number">0{{i+1}}</text><text class="room-type">{{r.roomType || '茶室'}}</text></view></view><view class="room-info"><text class="room-name">{{r.name}}</text><text class="room-detail">{{r.capacity || '独立空间'}} · {{Number(r.minHours || 1)}} 小时起订</text><view class="room-bottom"><text class="price">¥{{Number(r.priceHour).toFixed(0)}}<text class="unit"> / 小时</text></text><text class="select-room">{{activeRoom===r.id?'收起 −':'选时间 ＋'}}</text></view></view></view>
   <view class="occupancy">
    <view class="occupancy-heading"><text>当天占用</text><text class="occupancy-date">{{selDate}}</text></view>
    <text v-if="slotsLoading" class="occupancy-note">正在查询占用时段…</text>
    <view v-else-if="!occupied[r.id]" class="occupancy-error" @tap="loadSlots"><text>时段查询失败，点击重试</text></view>
    <view v-else-if="(occupied[r.id] || []).length" class="occupancy-list">
     <view v-for="(slot,index) in occupiedTimes(r)" :key="slot.start+'-'+slot.end+'-'+index" class="occupancy-chip" :class="{closed:slot.type==='CLOSED'}"><text class="occupancy-dot"/><text>{{slot.start}}–{{slot.end}}</text><text class="occupancy-kind">{{slot.type==='CLOSED'?'临时关闭':slot.type==='PENDING'?'待确认占用':'已占用'}}</text></view>
    </view>
    <text v-else class="occupancy-note">暂无预约占用或临时关闭，请选择可订时间</text>
   </view>
   <view v-if="activeRoom===r.id" class="booking-panel">
    <text v-if="r.facilities" class="facilities">{{r.facilities}}</text>
    <text v-if="slotsLoading" class="empty">正在更新可订时段…</text>
    <template v-else><view v-if="starts(r).length" class="time-row"><picker class="time-control" :range="starts(r)" range-key="time" :value="startIndex(r)" :disabled="submitting" @change="chooseStart(r,$event)"><view class="time-box"><text class="time-label">开始时间</text><text class="time-value">{{pick[r.id] || '请选择'}}<text class="chevron">⌄</text></text></view></picker><text class="time-separator">—</text><picker class="time-control" :range="options(r)" range-key="label" :value="endIndex(r)" :disabled="submitting || !pick[r.id]" @change="chooseEnd(r,$event)"><view class="time-box"><text class="time-label">结束时间</text><text class="time-value">{{ends[r.id] || '请选择'}}<text class="chevron">⌄</text></text></view></picker></view><view v-else class="empty">当天暂无可订时段，请换个日期</view>
    <text class="time-note">按半小时延长 · 已避开占用时段</text>
<text class="approval-note">提交申请后，需店员确认才算预约成功。</text><view class="checkout"><view><text class="total-label">{{selected(r) ? '共 '+selected(r).hours+' 小时' : '请选择起止时间'}}</text><text class="total-price">¥{{amount(r)}}</text></view><button class="book" :disabled="!selected(r) || submitting" :loading="submitting" @tap="reserve(r)">立即预约</button></view></template>
   </view>
  </view>
  <text class="footnote">提交后需店员确认才算预约成功，请在「我的预约」查看结果</text><CustomerNav active="rooms"/>

  <view v-if="booking" class="reservation-modal" @keydown.esc="closeBooking">
    <view class="reservation-mask" @tap="closeBooking" @touchmove.stop.prevent />
    <view class="reservation-sheet" role="dialog" aria-modal="true" aria-label="填写并确认预约">
      <view class="sheet-header"><view><text class="sheet-title">填写并确认预约</text><text class="sheet-subtitle">核对本次行程，留下联系方式</text></view><button class="sheet-close" :disabled="submitting" aria-label="关闭预约弹窗" @tap="closeBooking">×</button></view>
      <scroll-view scroll-y class="sheet-body">
        <view class="booking-summary"><text class="summary-branch">{{booking.storeName}}</text><text class="summary-room">{{booking.roomName}}</text><view class="summary-time"><text>{{booking.date}}</text><text>{{booking.time}}–{{booking.end}} · {{booking.hours}} 小时</text></view><view class="summary-price"><text>预计金额</text><text>¥{{booking.amount}}</text></view><button class="edit-time" :disabled="submitting" @tap="closeBooking">修改包间 / 时间 ›</button></view>
    <view class="contact-form">
      <text class="contact-title">预约信息</text>
      <text class="contact-hint">{{contactLoading?'正在读取账号信息…':'已保存的姓名、手机号会自动带入，可修改为本次联系人。'}}</text>
      <view class="contact-field"><text>联系人姓名 <text class="required">*</text></text><input v-model="contact.name" :disabled="submitting" maxlength="64" placeholder="请输入联系人姓名" aria-label="联系人姓名" @input="contactTouched.name=true" /></view>
      <view class="contact-field"><text>手机号 <text class="required">*</text></text><input v-model="contact.phone" :disabled="submitting" type="number" maxlength="11" placeholder="请输入11位手机号，方便店员联系" aria-label="预约手机号" @input="contactTouched.phone=true" /></view>
      <view class="contact-field guest-field"><view><text>到店人数 <text class="required">*</text></text></view><view class="guest-stepper"><button :disabled="submitting || contact.guests<=1" aria-label="减少到店人数" @tap="contact.guests=Math.max(1,Number(contact.guests||1)-1)">−</button><input v-model.number="contact.guests" type="number" :disabled="submitting" maxlength="3" aria-label="到店人数" /><text>人</text><button :disabled="submitting || contact.guests>=100" aria-label="增加到店人数" @tap="contact.guests=Math.min(100,Number(contact.guests||0)+1)">＋</button></view></view>
      <view class="contact-field"><text>预约备注 <text class="optional">选填</text></text><textarea v-model="contact.remark" :disabled="submitting" maxlength="500" placeholder="如茶具、布置或其他需求" aria-label="预约备注" /><text class="remark-count">{{contact.remark.length}} / 500</text></view>
    </view>
        <view class="booking-consent-row"><checkbox-group @change="bookingPrivacyAccepted = $event.detail.value.includes('booking')"><label class="booking-privacy"><checkbox value="booking" :checked="bookingPrivacyAccepted" color="#b5362d" /><text>同意预约资料用于门店处理通知</text></label></checkbox-group><text class="booking-detail-toggle" @tap="bookingDisclosureOpen = !bookingDisclosureOpen">{{bookingDisclosureOpen?'收起':'说明'}}</text></view>
        <view v-if="bookingDisclosureOpen" class="booking-disclosure"><text>本次预约的订单号、门店、茶室、时间、人数、联系人、电话和备注，会通过WxPusher预约通知服务提供给负责处理的门店员工，用于排期和联系。拒绝时可联系门店电话预约。</text><text class="booking-policy" @tap="openBookingPrivacy">查看隐私政策及服务商说明 ›</text></view>
        <text class="approval-note">提交后需店员确认才算预约成功，结果可在「我的预约」查看。{{bookingNoticeIds.length?'允许微信订阅通知后，预约通过时会收到提醒。':''}}</text>
      </scroll-view>
      <text v-if="bookingError" class="booking-error" role="alert">{{bookingError}}</text>
      <view class="sheet-footer"><button class="sheet-cancel" :disabled="submitting" @tap="closeBooking">再想想</button><button class="sheet-submit" :disabled="submitting || contactLoading" :loading="submitting" @tap="submitBooking">{{submitting?'提交中…':'确认信息并提交'}}</button></view>
    </view>
  </view>
  <view v-if="bookingResult" class="reservation-modal">
    <view class="reservation-mask" />
    <view class="reservation-sheet result-sheet" role="dialog" aria-modal="true" aria-label="预约申请已提交">
      <view class="sheet-header"><view><text class="sheet-title">预约申请已提交</text><text class="sheet-subtitle">待店员确认</text></view></view>
      <view class="result-body"><text>{{bookingResult.storeName}} · {{bookingResult.roomName}}</text><text>{{bookingResult.date}} {{bookingResult.time}}–{{bookingResult.end}}</text><text>我们已收到您的预约申请。{{bookingResult.noticeRegistered?'已登记微信通知，店员确认后将发送预约成功提醒。':'请在「我的预约」查看处理结果。'}}</text></view>
      <view class="sheet-footer"><button class="sheet-cancel" @tap="bookingResult=null">我知道了</button><button class="sheet-submit" @tap="viewReservations">查看我的预约</button></view>
    </view>
  </view>
 </view>
</template>
<script>
import api from '@/common/api.js'
import CustomerNav from '@/components/CustomerNav.vue'
import { endOptions } from '@/common/booking-times.mjs'
import { requestBookingNotice } from '@/common/booking-notice.mjs'
import { customerShare, receiveSharedStore } from '@/common/customer-share.mjs'



export default {
 components:{CustomerNav},
  data() {
    return {
      stores: [], storeIndex: 0, requestVersion: 0, activeRoom: null,
      dates: [],
      selDate: '',
      rooms: [],
      slotMap: {}, // roomId -> [{time, available}]
      ends: {}, occupied: {}, slotsLoading: false,
      pick: {},    // roomId -> time
      loading: false,
      bookingDisclosureOpen:false, bookingPrivacyAccepted:false, submitting: false, booking:null, bookingError:'', bookingResult:null, contact:{name:'',phone:'',remark:'',guests:1},contactTouched:{},contactLoading:false,contactOwner:null
      ,bookingNoticeIds:[],bookingNoticeBusy:false,bookingNoticeFailure:null,bookingNoticeSupported:typeof uni!=='undefined' && typeof uni.requestSubscribeMessage==='function'
    }
  },
  onShareAppMessage() { const {query, ...share}=customerShare('rooms',this.stores[this.storeIndex]); return share },
  onShareTimeline() { const {path, ...share}=customerShare('rooms',this.stores[this.storeIndex]); return share },
  onLoad(options) {
    receiveSharedStore(options, uni)
    this.buildDates()
  },
  onShow() { if(this.booking || this.submitting || this.bookingNoticeBusy)return; this.loadBookingNoticeSettings(); this.loadContact(); this.initStores() },
  methods: {
    openBookingPrivacy(){uni.navigateTo({url:'/pages/legal/document?kind=privacy'})},
    async subscribeBookingNotice() {
      if(this.bookingNoticeBusy || this.submitting || !this.bookingNoticeIds.length)return
      this.bookingNoticeBusy=true
      this.bookingNoticeFailure=null
      try {
        // Invoke directly on tap so WeChat can display the subscription prompt.
        const registered=await requestBookingNotice(api,this.bookingNoticeIds,uni,failure=>{this.bookingNoticeFailure=failure})
        uni.showToast({title:registered?'预约提醒订阅已登记':'未完成订阅，请查看按钮下方原因',icon:'none'})
      } finally {this.bookingNoticeBusy=false}
    },
    openBookingNoticeSettings() {
      if(typeof uni.openSetting!=='function'){uni.showToast({title:'请从右上角菜单进入设置，允许接收通知',icon:'none'});return}
      uni.openSetting({withSubscriptions:true,fail:()=>uni.showToast({title:'请从右上角菜单进入设置，允许接收通知',icon:'none'})})
    },
    async loadBookingNoticeSettings() {
      this.bookingNoticeIds=[]
      const token=uni.getStorageSync('customer_token')
      if(!token)return
      try {
        const config=await api.customerBookingNoticeSettings()
        if(token===uni.getStorageSync('customer_token') && config.enabled)this.bookingNoticeIds=config.templateIds||[]
      }catch{}
    },
    async loadContact() {
      const token=uni.getStorageSync('customer_token')
      const owner=uni.getStorageSync('customer_user')?.memberId
      if(!token || owner!==this.contactOwner){this.contact={name:'',phone:'',remark:'',guests:1};this.contactTouched={};this.contactOwner=owner}
      if(!token)return
      this.contactLoading=true
      try {
        const details=await api.customerBookingContact()
        if(token!==uni.getStorageSync('customer_token'))return
        if(!this.contactTouched.name)this.contact.name=details.name||''
        if(!this.contactTouched.phone)this.contact.phone=details.phone||''
      } catch(e) {} finally {this.contactLoading=false}
    },
    occupiedTimes(r) { return [...(this.occupied[r.id] || [])].sort((a,b) => a.start.localeCompare(b.start) || a.end.localeCompare(b.end)) },
    starts(r) { return (this.slotMap[r.id] || []).filter(s => s.available) },
    startIndex(r) { return Math.max(0,this.starts(r).findIndex(s => s.time === this.pick[r.id])) },
    endIndex(r) { return Math.max(0,this.options(r).findIndex(s => s.time === this.ends[r.id])) },
    chooseStart(r,e) { const start=this.starts(r)[Number(e.detail.value)]; if(start)this.pickSlot(r.id,start.time) },
    openRoom(r) {
      if(this.submitting || this.slotsLoading)return
      this.activeRoom=this.activeRoom===r.id?null:r.id
      if(this.activeRoom && !this.pick[r.id] && this.starts(r).length)this.pickSlot(r.id,this.starts(r)[0].time)
    },
    async initStores() {
      const version=++this.requestVersion
      this.stores=[]; this.rooms=[]; this.activeRoom=null; this.booking=null; this.pick={}; this.ends={}; this.occupied={}; this.slotMap={}
      try {
        const stores=await api.customerStores()
        if(version!==this.requestVersion)return
        this.stores=stores
        const saved=Number(uni.getStorageSync('customer_store_id'))
        this.storeIndex=Math.max(0,this.stores.findIndex(s=>s.id===saved))
        if(this.stores.length) { uni.setStorageSync('customer_store_id',this.stores[this.storeIndex].id); await this.loadRooms() }
        else uni.removeStorageSync('customer_store_id')
      } catch(e) { if(version===this.requestVersion)uni.removeStorageSync('customer_store_id') }
    },
    async changeStore(e) {
      this.storeIndex=Number(e.detail.value)
      if(!this.stores[this.storeIndex])return
      uni.setStorageSync('customer_store_id',this.stores[this.storeIndex].id)
      this.activeRoom=null; this.requestVersion++; this.pick={}; this.ends={}; this.occupied={}; this.slotMap={}; this.rooms=[]
      await this.loadRooms()
    },
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
      const storeId=this.stores[this.storeIndex]?.id, version=this.requestVersion
      if(!storeId){this.rooms=[];return}
      this.loading = true
      try {
        const rooms=await api.customerRooms(storeId)
        if(version!==this.requestVersion || storeId!==this.stores[this.storeIndex]?.id)return
        this.rooms = rooms
        await this.loadSlots()
      } catch (e) {
      } finally {
        this.loading = false
      }
    },
    async loadSlots() {
      const version=++this.requestVersion, date=this.selDate
      this.slotsLoading = true
      this.slotMap = {}
      const map = {}, occupied = {}
      for (const r of this.rooms) {
        try {
          const [slots, reserved] = await Promise.all([api.customerSlots(r.id, date, Number(r.minHours || 1)), api.customerReserved(r.id, date)])
          map[r.id] = slots; occupied[r.id] = reserved
        } catch (e) {
          map[r.id] = []
        }
      }
      if(version===this.requestVersion) {
        this.slotMap = map; this.occupied = occupied; this.slotsLoading = false
        for(const r of this.rooms) if(!(map[r.id] || []).some(s => s.available && s.time === this.pick[r.id])) { delete this.pick[r.id]; delete this.ends[r.id] }
      }
    },
    selectDate(date) {
      if(this.submitting)return
      this.activeRoom = null
      this.selDate = date
      this.pick = {}; this.ends = {}
      this.loadSlots()
    },
    options(r) { return endOptions(r, this.stores[this.storeIndex], this.pick[r.id], this.occupied[r.id] || []) },
    selected(r) { return this.options(r).find(o => o.time === this.ends[r.id]) },
    amount(r) { return (Number(r.priceHour) * (this.selected(r)?.hours || 0)).toFixed(2) },
    chooseEnd(r, event) { this.ends = {...this.ends, [r.id]:this.options(r)[Number(event.detail.value)]?.time} },
    pickSlot(roomId, time) {
      this.pick = { ...this.pick, [roomId]: time }
      const room = this.rooms.find(r => r.id === roomId)
      this.ends = {...this.ends, [roomId]:this.options(room)[0]?.time}
    },
    viewReservations() { this.bookingResult=null; uni.navigateTo({url:'/pages/customer/profile'}) },
    bookingFailed(message) { this.bookingError=message },
    closeBooking() { if(!this.submitting)this.booking=null },
    reserve(r) {
      if (!uni.getStorageSync('customer_token')) { uni.navigateTo({url:'/pages/customer/login'}); return }
      if (this.submitting || this.slotsLoading) return
      const selection=this.selected(r),time=this.pick[r.id]
      if(!selection || !time)return uni.showToast({title:'请选择可订时段',icon:'none'})
      this.bookingError=''
      this.bookingPrivacyAccepted=false
      this.bookingDisclosureOpen=false
      this.booking={roomId:r.id,roomName:r.name,storeName:this.stores[this.storeIndex]?.name||'',date:this.selDate,time,end:this.ends[r.id],hours:selection.hours,amount:this.amount(r)}
    },
    async submitBooking() {
      if (!this.bookingPrivacyAccepted) { this.bookingError='请先同意本次预约通知的信息提供；也可联系门店电话预约'; return }
      if(!this.booking || this.submitting)return
      this.bookingError=''
      const booking=this.booking
      const name=String(this.contact.name ?? '').trim()
      const phone=String(this.contact.phone ?? '').trim()
      const remark=String(this.contact.remark ?? '').trim()

      if(!name || name.length>64)return this.bookingFailed('请填写联系人姓名（最多64字）')
      if(!/^1[3-9]\d{9}$/.test(phone))return this.bookingFailed('请填写有效的11位手机号')
      if(!Number.isInteger(this.contact.guests)||this.contact.guests<1||this.contact.guests>100)return this.bookingFailed('到店人数请填写1–100的整数')
      if(this.contact.remark.length>500)return this.bookingFailed('备注最多500字')
      this.submitting = true
      try {
        const noticeRegistered=await requestBookingNotice(api,this.bookingNoticeIds,typeof uni==='undefined'?undefined:uni)
        await api.customerReserve({
          roomId: booking.roomId,
          reserveDate: booking.date,
          startTime: booking.time,
          hours: booking.hours, guests:this.contact.guests,
          contactName:name, contactPhone:phone, remark:remark
        })
        this.bookingResult={...booking,noticeRegistered}
        this.contact.remark=''; this.contact.guests=1
        this.booking=null
        this.bookingPrivacyAccepted=false
        delete this.ends[booking.roomId]
        delete this.pick[booking.roomId]
        this.loadSlots()
      } catch (e) { this.bookingFailed(e?.message || '未能确认提交结果，请先到「我的预约」查看；若没有记录，再重试。') } finally { this.submitting = false }
    }
  }
}
</script>

<style scoped>
.page{min-height:100vh;background:#faf7f2;padding:32rpx 32rpx 170rpx;color:#352a24}.page-heading{margin:10rpx 0 30rpx}.page-title{display:block;font-size:44rpx;font-weight:650;letter-spacing:1rpx}.page-subtitle{display:block;color:#8d8176;font-size:24rpx;margin-top:12rpx}.branch-card{display:flex;align-items:center;gap:18rpx;background:#fff;padding:24rpx;border:1rpx solid #eee5dc;border-radius:22rpx}.store-mark{width:60rpx;height:60rpx;flex-shrink:0;display:flex;align-items:center;justify-content:center;background:#fff0e4;color:#b9442e;border-radius:16rpx;font-size:28rpx}.branch-copy{flex:1;min-width:0}.branch-name{font-size:27rpx;font-weight:600;display:block;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.branch-address{font-size:21rpx;color:#92867b;display:block;margin-top:8rpx;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.branch-switch{font-size:22rpx;color:#a55f45;flex-shrink:0}.section-title{display:flex;align-items:center;justify-content:space-between;margin:34rpx 0 20rpx;font-size:29rpx;font-weight:600}.section-hint{font-size:21rpx;color:#94877d;font-weight:400}.datechips{white-space:nowrap}.datechip{display:inline-flex;flex-direction:column;align-items:center;justify-content:center;min-width:116rpx;padding:18rpx 10rpx 12rpx;margin-right:14rpx;background:#fff;border:1rpx solid #eee5dc;border-radius:18rpx}.datechip:last-child{margin-right:0}.date-label{font-size:21rpx;color:#8b7f75}.date-number{font-size:30rpx;font-weight:600;margin-top:10rpx}.date-dot{width:6rpx;height:6rpx;border-radius:50%;background:transparent;margin-top:10rpx}.datechip.sel{background:#fff0e6;border-color:#dc714b;color:#ae3928}.sel .date-label{color:#b55a3f}.sel .date-dot{background:#c95332}.room{background:#fff;border:1rpx solid #ece5de;border-radius:24rpx;margin-bottom:20rpx;overflow:hidden}.room.expanded{border-color:#d98565;box-shadow:0 10rpx 34rpx #a8452210}.room-head{display:flex;gap:22rpx;padding:24rpx;align-items:center}.thumbnail{height:150rpx;width:142rpx;flex-shrink:0;border-radius:16rpx;overflow:hidden;background:#f7ebe0}.thumbnail image{width:100%;height:100%}.room-placeholder{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;background:linear-gradient(135deg,#f8ebd9,#f4d9c3)}.room-number{font-size:48rpx;color:#b78a67;font-family:Georgia,serif}.room-type{font-size:20rpx;color:#9c7255;letter-spacing:4rpx;margin-top:8rpx}.room-info{flex:1;min-width:0}.room-name{font-size:31rpx;font-weight:600;display:block}.room-detail{display:block;font-size:22rpx;color:#938679;margin-top:12rpx}.room-bottom{display:flex;justify-content:space-between;align-items:baseline;gap:10rpx;margin-top:20rpx}.price{color:#b73d2d;font-size:33rpx;font-weight:600}.unit{font-size:19rpx;color:#9a8b7d;font-weight:400}.select-room{font-size:21rpx;color:#b45235;white-space:nowrap}.booking-panel{padding:0 24rpx 26rpx}.facilities{display:block;font-size:21rpx;color:#948478;padding-bottom:16rpx}.time-row{display:flex;align-items:center;gap:16rpx;padding-top:24rpx;border-top:1rpx solid #f0e8df}.time-control{flex:1;min-width:0}.time-box{background:#faf7f3;border:1rpx solid #eae1d7;border-radius:16rpx;padding:20rpx}.time-label{display:block;font-size:20rpx;color:#8a7c70}.time-value{font-size:38rpx;font-weight:500;display:flex;align-items:center;justify-content:space-between;margin-top:10rpx;font-variant-numeric:tabular-nums}.chevron{font-size:24rpx;color:#b17f62}.time-separator{color:#c8b7a6}.time-note{display:block;font-size:20rpx;color:#9b8a7b;margin:18rpx 0 24rpx}.checkout{display:flex;align-items:center;justify-content:space-between;gap:20rpx}.total-label{display:block;font-size:21rpx;color:#92806f}.total-price{display:block;font-size:40rpx;font-weight:600;color:#b5362d;margin-top:6rpx}.book{margin:0;width:260rpx;background:linear-gradient(105deg,#bb382a,#da6338);color:white;font-size:27rpx;border-radius:14rpx;line-height:88rpx;padding:0}.book[disabled]{background:#eee4dc;color:#a69586;opacity:1}.empty{padding:24rpx 0;color:#978676;text-align:center;font-size:24rpx}.footnote{display:block;text-align:center;color:#ab9b8c;font-size:21rpx;margin:32rpx 0}
.occupancy{margin:0 24rpx 22rpx;padding-top:18rpx;border-top:1rpx solid #f2eae2}.occupancy-heading{display:flex;justify-content:space-between;align-items:center;font-size:21rpx;color:#897669;margin-bottom:14rpx}.occupancy-date{font-size:19rpx;color:#ad9b8c}.occupancy-list{display:flex;flex-wrap:wrap;gap:12rpx}.occupancy-chip{display:flex;align-items:center;gap:10rpx;background:#fff0e8;color:#a6472e;border:1rpx solid #f0d6c6;padding:12rpx 16rpx;border-radius:10rpx;font-size:23rpx;font-variant-numeric:tabular-nums}.occupancy-kind{font-size:19rpx;color:#a97058}.occupancy-dot{width:8rpx;height:8rpx;display:block;border-radius:50%;background:#ce6946}.occupancy-chip.closed{background:#f1efed;border-color:#e1dad4;color:#807165}.closed .occupancy-dot{background:#9d8e83}.closed .occupancy-kind{color:#94877b}.occupancy-note{font-size:21rpx;color:#9b8879;line-height:1.7}.occupancy-error{font-size:21rpx;color:#b95138;padding:8rpx 0}</style>

<style scoped>.approval-note{display:block;color:#b15b3d;background:#fff2e7;padding:16rpx;border-radius:10rpx;font-size:22rpx;line-height:1.6;margin-bottom:22rpx}
.booking-notice{margin:20rpx 0 0;padding:24rpx;box-sizing:border-box;border:1rpx solid #ebe1d4;border-radius:20rpx;background:#f6f0e7}
.notice-main{display:flex;align-items:center;gap:16rpx}.notice-icon{display:flex;align-items:center;justify-content:center;flex-shrink:0;width:52rpx;height:60rpx;color:#a07850}.notice-bell{position:relative;width:26rpx;height:30rpx;border:2rpx solid currentColor;border-radius:16rpx 16rpx 5rpx 5rpx}.notice-bell::before{content:'';position:absolute;top:-7rpx;left:9rpx;width:4rpx;height:5rpx;border-radius:3rpx;background:currentColor}.notice-bell::after{content:'';position:absolute;bottom:-8rpx;left:8rpx;width:7rpx;height:4rpx;border-radius:0 0 6rpx 6rpx;background:currentColor}
.notice-copy{flex:1;min-width:0}.notice-title{display:block;font-size:26rpx;font-weight:600;line-height:1.5;color:#574432}.notice-description{display:block;margin-top:5rpx;font-size:21rpx;line-height:1.6;color:#887460}
.notice-subscribe{flex-shrink:0;margin:0;padding:0 20rpx;line-height:62rpx;border:1rpx solid #d9bea0;border-radius:32rpx;background:#fffaf3;color:#905331;font-size:23rpx;font-weight:500}.notice-subscribe::after,.notice-settings::after{border:0}.notice-subscribe[disabled]{background:#eee7dd;border-color:#dfd4c5;color:#a18c76}
.notice-hint{display:block;margin-top:18rpx;padding-top:14rpx;border-top:1rpx solid #e8ded0;font-size:20rpx;line-height:1.6;color:#918170}
.notice-failure{margin-top:16rpx;padding:16rpx 18rpx;background:#fff5ee;border:1rpx solid #edd5c5;border-radius:12rpx}.notice-failure text{display:block;font-size:22rpx;line-height:1.7;color:#a45136}.notice-settings{margin:10rpx 0 0;padding:8rpx 0;background:transparent;border:0;text-align:left;font-size:23rpx;line-height:1.6;color:#91442e}
</style>

<style scoped>
.contact-form{border-top:1rpx solid #eee2d7;padding-top:24rpx;margin:8rpx 0 24rpx}.contact-title{font-size:28rpx;font-weight:600;display:block;color:#584134}.contact-hint{font-size:21rpx;color:#9a8879;line-height:1.7;display:block;margin:10rpx 0 22rpx}.contact-field{margin-bottom:22rpx;font-size:24rpx;color:#735b49}.contact-field:last-child{margin-bottom:0}.required{color:#c04c34}.optional{color:#ac9b8c;font-size:21rpx;margin-left:10rpx}.contact-field input,.contact-field textarea{display:block;width:100%;box-sizing:border-box;background:#faf7f3;border:1rpx solid #eae1d7;border-radius:12rpx;padding:18rpx 20rpx;font-size:25rpx;color:#483628;margin-top:12rpx}.contact-field input{height:84rpx}.contact-field textarea{height:154rpx;line-height:1.7}.remark-count{font-size:19rpx;color:#ac9b8c;display:block;text-align:right;margin-top:8rpx}
</style>

<style scoped>
.reservation-modal{position:fixed;inset:0;z-index:90;display:flex;align-items:center;justify-content:center;padding:28rpx}
.reservation-mask{position:absolute;inset:0;background:#2e201b80}
.reservation-sheet{position:relative;width:100%;max-width:680rpx;max-height:92vh;background:#fffaf4;border-radius:30rpx;display:flex;flex-direction:column;overflow:hidden;box-shadow:0 24rpx 100rpx #26120f40}
.sheet-header{padding:28rpx 28rpx 22rpx;display:flex;justify-content:space-between;align-items:center;border-bottom:1rpx solid #eee1d4;flex-shrink:0}
.sheet-title{display:block;font-size:34rpx;font-family:serif;font-weight:600;color:#54392c}.sheet-subtitle{display:block;font-size:21rpx;color:#a18a79;margin-top:8rpx}
.sheet-close{width:60rpx;height:60rpx;line-height:60rpx;padding:0;margin:0;background:#f7ece1;color:#8c6d58;border-radius:50%;font-size:36rpx;flex-shrink:0}
.sheet-body{min-height:0;max-height:65vh;box-sizing:border-box;padding:24rpx 28rpx;overflow-y:auto;overscroll-behavior:contain}
.booking-summary{background:#fff0e1;border:1rpx solid #f0d8c1;border-radius:18rpx;padding:22rpx}.summary-branch{display:block;font-size:21rpx;color:#9d7355}.summary-room{display:block;font-size:29rpx;font-weight:600;margin:8rpx 0 14rpx;color:#673f29}.summary-time{display:flex;flex-wrap:wrap;gap:8rpx 20rpx;font-size:23rpx;color:#815d43;line-height:1.7}.summary-price{display:flex;justify-content:space-between;margin-top:16rpx;font-size:25rpx;color:#b6442f}.edit-time{width:auto;margin:12rpx 0 0;padding:0;background:transparent;text-align:left;color:#a97550;font-size:21rpx;line-height:1.8}
.sheet-footer{padding:22rpx 28rpx;padding-bottom:calc(22rpx + env(safe-area-inset-bottom));display:flex;gap:18rpx;border-top:1rpx solid #eee1d4;background:#fffaf4;flex-shrink:0}.sheet-footer button{line-height:84rpx;margin:0;border-radius:14rpx;font-size:25rpx}.sheet-cancel{width:150rpx;background:#f4e9dd;color:#8e7059;flex-shrink:0}.sheet-submit{flex:1;background:linear-gradient(110deg,#bd3b2c,#dc6639);color:white}
.reservation-sheet .contact-form{border-top:0;margin:0 0 20rpx}.reservation-sheet .contact-title{display:none}.reservation-sheet .contact-hint{margin-top:0;font-size:20rpx}.reservation-sheet .contact-field{margin-bottom:18rpx}.reservation-sheet .contact-field input{height:74rpx}.reservation-sheet .contact-field textarea{height:112rpx}.reservation-sheet .approval-note{margin-bottom:0}
</style>

<style scoped>
.guest-field{display:flex;align-items:center;justify-content:space-between;gap:16rpx}.guest-stepper{display:flex;align-items:center;gap:8rpx;border:1rpx solid #eadccc;background:#fff;border-radius:12rpx;padding:4rpx;flex-shrink:0}.guest-stepper button{width:54rpx;height:58rpx;line-height:58rpx;padding:0;margin:0;border-radius:8rpx;font-size:28rpx;background:#fff0e1;color:#b54b31}.reservation-sheet .guest-stepper input{width:58rpx;height:58rpx;padding:0;margin:0;border:0;background:transparent;border-radius:0;text-align:center;font-size:26rpx}.guest-stepper>text{font-size:21rpx;color:#947b67;margin-right:6rpx}
</style>

<style scoped>
.booking-error{display:block;flex-shrink:0;margin:0 28rpx;padding:18rpx 20rpx;background:#fff0e8;border:1rpx solid #edc4b5;border-radius:12rpx;color:#ad3828;font-size:23rpx;line-height:1.6}.result-body{padding:12rpx 32rpx 28rpx;display:flex;flex-direction:column;gap:20rpx;color:#735b49;font-size:25rpx;line-height:1.8}
</style>

<style scoped>.booking-privacy{display:flex;align-items:center;font-size:24rpx;line-height:1.8;color:#78695c;margin:0;min-height:62rpx}.booking-privacy checkbox{flex-shrink:0;width:40rpx;transform:scale(.65);transform-origin:center}.booking-privacy text{flex:1}.booking-policy{display:block;color:#9f352b;font-size:24rpx;padding:16rpx 0}</style>

<style scoped>.booking-consent-row{display:flex;align-items:center;gap:12rpx;margin:20rpx 0}.booking-consent-row checkbox-group{flex:1;min-width:0}.booking-detail-toggle{color:#ad8871;font-size:22rpx;padding:16rpx 0 16rpx 12rpx}.booking-disclosure{padding:20rpx 24rpx;background:#f8f1e8;border-radius:14rpx;margin-bottom:20rpx;color:#8b7967;font-size:23rpx;line-height:1.9}.booking-disclosure>text{display:block}.booking-disclosure .booking-policy{padding-bottom:0}</style>

<style scoped>.booking-privacy{font-size:12px;min-height:28px;color:#817972;line-height:1.6}.booking-privacy checkbox{width:18px;transform:scale(.6)}.booking-detail-toggle,.booking-policy{font-size:12px;color:#8d5048}.booking-detail-toggle{padding:6px 0 6px 8px}.booking-consent-row{margin:12px 0;gap:6px}.booking-disclosure{font-size:12px;line-height:1.8}</style>
