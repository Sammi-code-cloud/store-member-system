<template><view class="staff-nav"><view v-for="item in items" :key="item.key" :class="{active:active===item.key}" @tap="go(item)"><text>{{item.label}}</text></view></view></template>
<script>
export default {props:{active:String},data(){return{items:[{key:'workbench',label:'工作台'},{key:'profile',label:'我的'}]}},methods:{go(item){if(item.key!==this.active)uni.redirectTo({url:'/pages/staff/'+item.key})}}}
</script>
<style scoped>.staff-nav{position:fixed;bottom:0;left:0;right:0;z-index:20;display:flex;max-width:430px;margin:auto;background:#fffdfa;border-top:1rpx solid #f0e3d6;padding:24rpx 24rpx calc(24rpx + env(safe-area-inset-bottom))}.staff-nav view{flex:1;text-align:center;font-size:27rpx;line-height:48rpx;color:#958071}.staff-nav .active{color:#b5362d;font-weight:600}</style>
