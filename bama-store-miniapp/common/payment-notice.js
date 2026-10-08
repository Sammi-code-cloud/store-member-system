import { hasPrivacyConsent, privacyLogin } from './privacy-consent.mjs'
import request from '@/common/request.js'

export default {
  data(){return {paymentNoticeIds:[],paymentNoticeOpening:false}},
  onShow(){this.loadPaymentNoticeSettings()},
  watch:{loggedIn(value){if(value)this.loadPaymentNoticeSettings()}},
  methods:{
    loadPaymentNoticeSettings(){
      this.paymentNoticeIds=[]
      const token=uni.getStorageSync('customer_token')
      if(!token || !hasPrivacyConsent(uni, true))return
      request({url:'/api/customer/wallet-notices',silent:true}).then(config=>{
        if(config.enabled && uni.getStorageSync('customer_token')===token)this.paymentNoticeIds=config.templateIds||[]
      }).catch(()=>{})
    },
    openMemberPaycode(){
      if(this.paymentNoticeOpening)return
      if(!uni.getStorageSync('customer_token') || !hasPrivacyConsent(uni, true)){uni.navigateTo({url:'/pages/customer/login'});return}
      this.paymentNoticeOpening=true
      let finished=false
      const proceed=()=>{
        if(finished)return
        finished=true
        uni.navigateTo({url:'/pages/customer/paycode',complete:()=>{this.paymentNoticeOpening=false}})
      }
      if(!this.paymentNoticeIds.length || typeof uni.requestSubscribeMessage!=='function'){proceed();return}
      const ids=[...this.paymentNoticeIds]
      // Must remain synchronous in the tap handler to preserve WeChat's user gesture.
      try{uni.requestSubscribeMessage({
        tmplIds:ids,
        success:async result=>{
          if(!ids.some(id=>result[id]==='accept')){proceed();return}
          try{
            const login=await privacyLogin(uni, true)
            if(!login.code)throw new Error('Missing login code')
            await request({url:'/api/customer/wallet-notices',method:'POST',data:{code:login.code},silent:true})
          }catch{uni.showToast({title:'提醒未开通，仍可正常付款',icon:'none'})}
          finally{proceed()}
        },
        fail:proceed
      })}catch{proceed()}
    }
  }
}
