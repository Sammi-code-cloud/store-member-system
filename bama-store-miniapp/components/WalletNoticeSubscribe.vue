<template>
  <view v-if="enabled" class="wallet-notice">
    <button :disabled="busy" :loading="busy" @tap="subscribe">订阅充值 / 扣减提醒</button>
    <text>允许后，充值或扣减余额时可收到微信提醒。授权次数用完后请再次订阅，不订阅也可正常付款。</text>
  </view>
</template>
<script>
import request from '@/common/request.js'
export default {
  data(){return {enabled:false,busy:false,templateIds:[]}},
  async mounted(){
    if(typeof uni.requestSubscribeMessage!=='function')return
    try{
      const config=await request({url:'/api/customer/wallet-notices',silent:true})
      this.templateIds=config.templateIds||[]
      this.enabled=config.enabled===true && this.templateIds.length>0
    }catch{}
  },
  methods:{
    subscribe(){
      if(this.busy || !this.enabled)return
      this.busy=true
      // Keep this call directly in the user's tap handler; no asynchronous work before the consent prompt.
      uni.requestSubscribeMessage({
        tmplIds:this.templateIds,
        success:async result=>{
          if(!this.templateIds.some(id=>result[id]==='accept')){
            this.busy=false
            uni.showToast({title:'未订阅，仍可正常充值和消费',icon:'none'})
            return
          }
          try{
            const login=await new Promise((resolve,reject)=>uni.login({provider:'weixin',success:resolve,fail:reject}))
            if(!login.code)throw new Error('微信授权失败')
            await request({url:'/api/customer/wallet-notices',method:'POST',data:{code:login.code}})
            uni.showToast({title:'交易提醒订阅已登记',icon:'none'})
          }catch{uni.showToast({title:'提醒登记未完成，请重试',icon:'none'})}
          finally{this.busy=false}
        },
        fail:()=>{this.busy=false;uni.showToast({title:'未能订阅，请检查微信通知设置',icon:'none'})}
      })
    }
  }
}
</script>
<style scoped>
.wallet-notice{width:100%;margin:24rpx 0;padding:24rpx;box-sizing:border-box;border-radius:16rpx;background:#fff7ed}.wallet-notice button{background:#f3dfca;color:#813b2d;font-size:26rpx;border-radius:12rpx}.wallet-notice text{display:block;font-size:22rpx;line-height:1.7;color:#79604e;margin-top:12rpx}
</style>
