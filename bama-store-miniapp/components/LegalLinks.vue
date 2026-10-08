<template><view class="legal-links"><text @tap="open('service')">用户服务协议</text><text @tap="open('privacy')">隐私政策</text><text v-if="showSettings" @tap="settings">隐私与授权</text></view></template>
<script>
export default {props:{showSettings:{type:Boolean,default:true}},methods:{open(kind){uni.navigateTo({url:'/pages/legal/document?kind='+kind})},settings(){uni.navigateTo({url:'/pages/legal/privacy-settings'})}}}
</script>
<style scoped>.legal-links{display:flex;justify-content:center;flex-wrap:wrap;gap:24rpx;padding:32rpx 0;font-size:23rpx;color:#95664e}.legal-links text{padding:8rpx 0}</style>
