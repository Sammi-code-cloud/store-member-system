<template>
  <view class="page">
    <view style="text-align:right;color:#a73129;font-size:26rpx;margin-bottom:20rpx" @tap="viewRecords">查看消费记录 ›</view>
    <!-- 会员信息 -->
    <view class="member card">
      <view class="avatar">{{ (member.name || '会').charAt(0) }}</view>
      <view class="info">
        <text class="nm">{{ member.name }} <text class="lv">{{ levelText }}</text></text>
        <text class="bl">{{ member.phone || '未绑定手机号' }}</text>
        <text class="bl">会员卡余额 <text class="b mono">¥{{ fmt(member.balance) }}</text></text>
      </view>
    </view>

    <view class="modes"><button :disabled="submitting || !!pending" @tap="mode='manual'">手动金额</button><button :disabled="submitting || !!pending" @tap="mode='items'">消费明细</button></view>
    <view v-if="mode==='manual'" class="amount card">
      <text>实际扣减金额（元）</text>
      <input :disabled="submitting || !!pending" v-model="amount" type="digit" class="manual-input" placeholder="请输入扣款金额" />
      <text class="bl">按输入金额扣减会员余额，不再叠加会员折扣</text>
    </view>
    <view v-else>
    <!-- 消费明细 -->
    <view class="row-head flex-between">
      <text class="h">消费明细</text>
      <view class="add" @tap="addItem">＋ 添加</view>
    </view>
    <view class="list card">
      <view v-for="(it, i) in items" :key="i" class="li">
        <input :disabled="submitting || !!pending" v-model="it.itemName" class="iname" placeholder="项目名称" />
        <input :disabled="submitting || !!pending" v-model.number="it.price" class="iprice mono" type="digit" placeholder="单价" />
        <text class="x">×</text>
        <input :disabled="submitting || !!pending" v-model.number="it.quantity" class="iqty mono" type="number" />
        <text class="del" @tap="removeItem(i)">✕</text>
      </view>
    </view>

    <!-- 快捷添加 -->
    <view class="quick">
      <text class="q" @tap="quickAdd('ROOM', '观山茶室 1小时', 188)">+茶室</text>
      <text class="q" @tap="quickAdd('PRODUCT', '大红袍 125g', 288)">+大红袍</text>
      <text class="q" @tap="quickAdd('PRODUCT', '金骏眉 100g', 439)">+金骏眉</text>
    </view>

    </view>
    <!-- 金额 -->
    <view class="amount card">
      <view v-if="mode==='items'" class="flex-between line1">
        <text>原价合计</text>
        <text class="mono">¥{{ fmt(origin) }}</text>
      </view>
      <view class="flex-between">
        <text class="big-l">应扣金额{{ mode==='items' ? '（'+discount+' 折后）' : '' }}</text>
        <text class="big mono">¥{{ fmt(payEstimate) }}</text>
      </view>
    </view>

    <button class="btn-primary confirm" :disabled="submitting || completed || !member.memberId" @tap="confirm">
      {{ submitting ? '扣款中...' : (pending ? '重试同一笔扣款' : '确认扣款 · 扣会员卡') }}
    </button>
    <text v-if="pending && !completed" class="bl">如遇网络异常，请核对消费记录；重试使用同一业务号，避免重复扣款。</text>
  </view>
</template>

<script>
import api from '@/common/api.js'

export default {
  data() {
    return {
      member: {},
      items: [{ itemType: 'PRODUCT', itemName: '', price: 0, quantity: 1 }],
      mode:'manual', amount:'', completed:false, pending:null,
      submitting: false
    }
  },
  onLoad() {
    this.member = uni.getStorageSync('chargeMember') || {}
    const saved=uni.getStorageSync('chargePending')
    if(saved){this.member=saved.member;this.pending=saved.payload;this.mode=saved.payload.amount!=null?'manual':'items';this.amount=String(saved.payload.amount || '');this.items=saved.payload.items || []}
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
      return this.mode==='manual' ? Number(this.amount || 0) : Math.round(this.origin * (this.member.discount || 100)) / 100
    },
    levelText() {
      const map = { NORMAL: '普通', GOLD: '金卡', BLACK_GOLD: '黑金' }
      return map[this.member.level] || ''
    }
  },
  methods: {
    viewRecords(){if(!this.submitting)uni.navigateTo({url:'/pages/staff/records?type=CONSUME'})},
    fmt(n) { return Number(n || 0).toFixed(2) },
    addItem() {
      if(this.submitting || this.pending)return
      this.items.push({ itemType: 'PRODUCT', itemName: '', price: 0, quantity: 1 })
    },
    quickAdd(type, name, price) {
      if(this.submitting || this.pending)return
      this.items.push({ itemType: type, itemName: name, price, quantity: 1 })
    },
    removeItem(i) {
      if(this.submitting || this.pending)return
      this.items.splice(i, 1)
    },
    async confirm() {
      if(this.submitting || this.completed || !this.member.memberId)return
      let payload=this.pending
      if(!payload){
        const amount=String(this.amount || '').trim()
        if(this.mode==='manual' && (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount)<=0 || Number(amount)>99999999.99)){uni.showToast({title:'请输入有效金额，最多两位小数',icon:'none'});return}
        const valid=this.items.map(it=>({...it,price:Number(it.price),quantity:Number(it.quantity)}))
        if(this.mode==='items' && (!valid.length || valid.some(it=>!it.itemName.trim() || !/^\d+(\.\d{1,2})?$/.test(String(it.price)) || it.price<=0 || !Number.isInteger(it.quantity) || it.quantity<1))){uni.showToast({title:'请填写有效消费明细',icon:'none'});return}
        payload={memberId:this.member.memberId,bizNo:'MP'+Date.now().toString(36)+Math.random().toString(36).slice(2,14),remark:this.member.lookupPhone?'门店手机号收款':'门店扫码消费'}
        if(this.member.lookupPhone)payload.expectedPhone=this.member.lookupPhone
        if(this.mode==='manual')payload.amount=Number(amount)
        else payload.items=valid
      }
      this.submitting=true
      try {
        if(!this.pending && this.member.lookupPhone){
          const current=await api.chargeMemberByPhone(this.member.lookupPhone)
          if(current.memberId!==this.member.memberId)throw new Error('会员资料已变化，请返回重新查询')
          this.member={...current,lookupPhone:this.member.lookupPhone}
        }
        const estimate=payload.amount ?? Number((payload.items.reduce((sum,it)=>sum+it.price*it.quantity,0)*(this.member.discount ?? 100)/100).toFixed(2))
        const decision=await new Promise(resolve=>uni.showModal({title:'确认会员扣款',content:`会员：${this.member.name || '会员'}\n手机号：${this.member.phone || '未绑定'}\n扣款金额：¥${this.fmt(estimate)}`,success:resolve,fail:()=>resolve({confirm:false})}))
        if(!decision.confirm)return
        this.pending=payload
        uni.setStorageSync('chargePending',{member:this.member,payload})
        const res = await api.chargeConfirm(payload)
        this.completed=true
        uni.removeStorageSync('chargePending')
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
        if([400,401,403,1003,1005].includes(e.code)){this.pending=null;uni.removeStorageSync('chargePending')}
        if(e.code===1007){this.completed=true;uni.removeStorageSync('chargePending')}
        uni.showToast({title:e.message || '扣款未完成，请核对记录',icon:'none'})
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.modes{display:flex;gap:20rpx;margin-top:24rpx}.modes button{flex:1;font-size:28rpx}.manual-input{height:100rpx;font-size:40rpx;color:$brand}.bl{display:block;font-size:24rpx;color:$muted;line-height:1.6}
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
