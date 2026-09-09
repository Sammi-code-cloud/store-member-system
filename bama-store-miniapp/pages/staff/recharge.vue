<template>
  <view class="page">
    <view style="text-align:right;color:#a73129;font-size:26rpx;margin-bottom:20rpx" @tap="viewRecords">查看充值记录 ›</view>
    <view class="form card">
      <view class="field">
        <text class="label">会员手机号</text>
        <view class="search-row"><input v-model="phone" class="input mono" type="number" maxlength="11" :disabled="submitting" placeholder="输入会员的11位手机号" @input="clearMember" @confirm="searchMember" /><button class="search-button" :loading="searching" :disabled="searching || submitting" @tap="searchMember">查询</button></view>
      </view>
      <view v-if="member" class="member-result"><text class="member-name">{{ member.name || '会员' }}</text><text>{{ member.phone }}</text><text class="member-balance">当前余额 ¥{{ Number(member.balance || 0).toFixed(2) }}</text></view>
      <text v-else class="search-note">{{ searchMessage || '请先查询并核对会员，再填写储值金额' }}</text>
      <view class="field"><text class="label">充值金额（元）</text><input v-model="form.amount" class="input mono" type="digit" :disabled="submitting" placeholder="如 3000" /></view>
      <view class="field"><text class="label">赠送金额（元）</text><input v-model="form.giftAmount" class="input mono" type="digit" :disabled="submitting" placeholder="选填，如 500" /></view>
      <view class="field"><text class="label">备注</text><input v-model="form.remark" class="input" :disabled="submitting" placeholder="选填" /></view>
      <button class="btn-primary submit" :loading="submitting" :disabled="submitting || searching || !member" @tap="submit">{{ submitting ? '处理中…' : '确认储值' }}</button>
      <text class="note">请核对会员姓名、手机号及金额，确认后记入会员账户。</text>
    </view>
  </view>
</template>
<script>
import api from '@/common/api.js'
export default {
  data() { return {phone:'',member:null,searchMessage:'',searching:false,searchRequest:0,form:{amount:'',giftAmount:'',remark:''},submitting:false,completed:false} },
  onHide() { this.searchRequest++; this.searching=false },
  methods: {
    viewRecords(){if(!this.submitting)uni.navigateTo({url:'/pages/staff/records?type=RECHARGE'})},
    clearMember() { this.searchRequest++; this.member=null; this.searchMessage=''; this.searching=false },
    async searchMember() {
      if(this.submitting || this.searching)return
      const phone=String(this.phone || '').trim(), request=++this.searchRequest
      this.member=null;this.searchMessage=''
      if(!/^1[3-9]\d{9}$/.test(phone)){this.searchMessage='请输入正确的11位手机号';return}
      this.searching=true
      try {
        const member=await api.memberByPhone(phone)
        if(request!==this.searchRequest || phone!==String(this.phone).trim())return
        this.member=member
      } catch(e) {if(request===this.searchRequest)this.searchMessage=e.message || '查询失败，请重试'}
      finally {if(request===this.searchRequest)this.searching=false}
    },
    async submit() {
      if(this.submitting || this.searching || this.completed)return
      const member=this.member,phone=String(this.phone || '').trim()
      if(!member || member.phone!==phone){uni.showToast({title:'请先按手机号查询会员',icon:'none'});return}
      const amount=String(this.form.amount ?? '').trim(),gift=String(this.form.giftAmount ?? '').trim() || '0'
      if(!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount)<=0 || !/^\d+(\.\d{1,2})?$/.test(gift) || !Number.isFinite(Number(amount)+Number(gift))){uni.showToast({title:'请输入正确金额，最多两位小数',icon:'none'});return}
      this.submitting=true
      const payload={memberId:member.id,amount:Number(amount),giftAmount:Number(gift),remark:this.form.remark}
      try {
        const current=await api.memberByPhone(phone)
        if(current.id!==member.id){this.member=null;throw new Error('会员资料已变化，请重新查询')}
        const confirmation=await new Promise(resolve=>uni.showModal({title:'确认会员储值',content:`会员：${current.name || '会员'}\n手机号：${phone}\n本金 ¥${Number(amount).toFixed(2)}，赠送 ¥${Number(gift).toFixed(2)}`,success:resolve,fail:()=>resolve({confirm:false})}))
        if(!confirmation.confirm)return
        if(this.member?.id!==member.id || String(this.phone).trim()!==phone)throw new Error('手机号已变化，请重新查询')
        await api.recharge(payload)
        this.completed=true;this.member=null;this.form={amount:'',giftAmount:'',remark:''}
        uni.showModal({title:'储值成功',content:`到账 ¥${(payload.amount+payload.giftAmount).toFixed(2)}（本金 ¥${payload.amount.toFixed(2)} + 赠送 ¥${payload.giftAmount.toFixed(2)}）`,showCancel:false,success:()=>uni.navigateBack()})
      } catch(e) {uni.showToast({title:e.message || '储值未完成，请核对记录后重试',icon:'none'})}
      finally {this.submitting=false}
    }
  }
}
</script>
<style lang="scss" scoped>
.page{padding:32rpx}.form{padding:40rpx 36rpx}.field{margin-bottom:28rpx}.label{font-size:24rpx;color:$muted}.input{margin-top:14rpx;height:88rpx;background:$porcelain;border-radius:16rpx;padding:0 24rpx;font-size:30rpx}.search-row{display:flex;align-items:center;gap:16rpx}.search-row .input{flex:1;min-width:0}.search-button{width:132rpx;flex-shrink:0;margin:14rpx 0 0;padding:0;font-size:26rpx;line-height:88rpx;background:#b5362d;color:#fff9ea;border-radius:16rpx}.member-result{background:#fff3e6;border:1rpx solid #ead5bd;border-radius:18rpx;padding:24rpx;margin-bottom:30rpx;display:flex;flex-direction:column;gap:10rpx;font-size:26rpx;color:#705346}.member-name{font-size:32rpx;font-weight:600;color:#422b24}.member-balance{color:#b5362d}.search-note{display:block;color:$muted;font-size:24rpx;line-height:1.6;margin-bottom:28rpx}.submit{padding:12rpx;font-size:30rpx;font-weight:600}.submit[disabled]{opacity:.55}.note{display:block;text-align:center;color:$muted;font-size:22rpx;margin-top:20rpx;line-height:1.6}
</style>
