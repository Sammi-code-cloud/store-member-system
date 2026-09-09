<template>
  <view class="page">
    <view class="filters card">
      <view class="scope"><button :class="{active:scope==='mine'}" @tap="setScope('mine')">我经办的</button><button v-if="canViewStore" :class="{active:scope==='store'}" @tap="setScope('store')">本店全部</button></view>
      <view class="search"><input v-model="keyword" placeholder="会员手机号、姓名或单号" confirm-type="search" @confirm="reload"/><button @tap="reload">查询</button></view>
      <view class="types"><text v-for="t in types" :key="t.value" :class="{active:type===t.value}" @tap="setType(t.value)">{{t.label}}</text></view>
      <view class="dates"><picker mode="date" :value="startDate" @change="startDate=$event.detail.value;reload()"><text>{{startDate || '开始日期'}}</text></picker><text>至</text><picker mode="date" :value="endDate" @change="endDate=$event.detail.value;reload()"><text>{{endDate || '结束日期'}}</text></picker><text @tap="clearDates">清除</text></view>
    </view>
    <text class="note">共 {{total}} 条流水 · 充值本金与赠送金额分别记账</text>
    <text v-if="error" class="empty">{{error}}</text>
    <text v-else-if="!rows.length" class="empty">{{loading?'正在加载…':'暂无记录'}}</text>
    <view v-for="r in rows" :key="r.id" class="record card">
      <view class="line"><text class="title">{{typeText(r.type)}}</text><text class="money" :class="{expense:r.type==='CONSUME'}">{{r.type==='CONSUME'?'-':'+'}}¥{{money(r.amount)}}</text></view>
      <text class="member">{{r.memberName || '会员'}} · {{r.memberPhone || '未绑定手机号'}}</text>
      <text>余额：¥{{money(r.balanceBefore)}} → ¥{{money(r.balanceAfter)}}</text>
      <text>经办员工：{{r.staffName || '—'}}<text v-if="r.staffId">（#{{r.staffId}}）</text></text>
      <text>门店：{{r.storeName || ('#'+r.storeId)}}</text>
      <text>时间：{{String(r.createTime || '').replace('T',' ')}}</text>
      <text class="number" selectable>流水号：{{r.bizNo}}</text>
      <text v-if="r.refOrderNo" class="number" selectable>消费单号：{{r.refOrderNo}}</text>
      <text v-if="r.remark">备注：{{r.remark}}</text>
    </view>
    <button v-if="rows.length<total && !error" class="more" :disabled="loading" @tap="loadMore">{{loading?'加载中…':'加载更多'}}</button>
  </view>
</template>
<script>
import api from '@/common/api.js'
import {auth} from '@/common/store.js'
export default {
  data(){return{scope:'mine',type:'',keyword:'',startDate:'',endDate:'',rows:[],total:0,page:0,loading:false,error:'',requestId:0,canViewStore:false,types:[{label:'全部',value:''},{label:'充值本金',value:'RECHARGE'},{label:'赠送',value:'GIFT'},{label:'消费',value:'CONSUME'},{label:'退款',value:'REFUND'}]}},
  onLoad(options){if(this.types.some(t=>t.value===options?.type))this.type=options.type},
  onShow(){if(!auth.isLogin()){uni.reLaunch({url:'/pages/staff/login'});return}this.canViewStore=auth.can('account:view');if(!this.canViewStore)this.scope='mine';this.reload()},
  onHide(){this.requestId++;this.loading=false},
  onUnload(){this.requestId++},
  onPullDownRefresh(){this.reload().finally(()=>uni.stopPullDownRefresh())},
  onReachBottom(){this.loadMore()},
  methods:{
    money(value){return Number(value || 0).toFixed(2)},
    typeText(value){return this.types.find(t=>t.value===value)?.label || value},
    setScope(value){if(value==='store'&&!this.canViewStore)return;this.scope=value;this.reload()},
    setType(value){this.type=value;this.reload()},
    clearDates(){this.startDate='';this.endDate='';this.reload()},
    reload(){this.requestId++;this.loading=false;this.rows=[];this.total=0;this.page=0;return this.loadPage(1)},
    loadMore(){if(this.loading || this.error || this.rows.length>=this.total)return;return this.loadPage(this.page+1)},
    async loadPage(page){
      if(this.loading)return
      this.error=''
      if(this.startDate&&this.endDate&&this.startDate>this.endDate){this.error='开始日期不能晚于结束日期';return}
      const id=++this.requestId
      this.loading=true
      try{
        const result=await api.staffTransactions({scope:this.scope,type:this.type,keyword:this.keyword.trim(),startDate:this.startDate || undefined,endDate:this.endDate || undefined,pageNum:page,pageSize:20})
        if(id!==this.requestId)return
        this.rows=page===1?(result.records || []):this.rows.concat(result.records || [])
        this.total=Number(result.total || 0);this.page=page
      }catch(e){if(id===this.requestId)this.error=e.message || '加载失败，请重新查询'}
      finally{if(id===this.requestId)this.loading=false}
    }
  }
}
</script>
<style lang="scss" scoped>
.page{padding:28rpx;background:$porcelain;min-height:100vh}.filters{padding:24rpx}.scope,.search,.dates,.line{display:flex;align-items:center;gap:18rpx}.scope button{flex:1;font-size:26rpx;background:#f6f0ea;color:#76594a;line-height:72rpx}.scope .active{background:$brand;color:white}.search{margin-top:24rpx}.search input{flex:1;min-width:0;height:76rpx;background:#f7f3ef;border-radius:12rpx;padding:0 18rpx;font-size:25rpx}.search button{font-size:24rpx;background:$brand;color:white;line-height:76rpx;padding:0 22rpx}.types{display:flex;flex-wrap:wrap;gap:12rpx;margin:24rpx 0}.types text{padding:10rpx 16rpx;border-radius:10rpx;font-size:24rpx;color:#795d50;background:#f6f0ea}.types .active{color:$brand;background:#fce8e3}.dates{justify-content:space-between;font-size:23rpx;color:#8a6857}.note{display:block;font-size:23rpx;color:$muted;margin:24rpx 0}.record{padding:26rpx;margin-bottom:20rpx}.record>text{display:block;font-size:24rpx;color:#8a6a57;line-height:1.8}.line{justify-content:space-between;margin-bottom:12rpx}.title{font-size:29rpx;font-weight:600;color:#51382b}.money{font-size:34rpx;font-weight:600;color:#34816a}.money.expense{color:$brand}.record .member{font-size:27rpx;color:#51382b;margin-bottom:8rpx}.number{word-break:break-all}.empty{display:block;text-align:center;padding:60rpx 0;color:$muted;font-size:26rpx}.more{font-size:26rpx;color:$brand;background:white;margin:20rpx 0}
</style>
