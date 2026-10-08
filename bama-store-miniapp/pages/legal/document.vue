<template>
  <view class="legal-page"><text class="title">{{document.title}}</text><text class="date">更新及生效日期：{{document.updated}} · 版本 {{document.version}}</text><view v-for="section in document.sections" :key="section.title" class="section"><text class="heading">{{section.title}}</text><text v-for="paragraph in section.paragraphs" :key="paragraph" class="paragraph" selectable>{{paragraph}}</text></view><text class="footer">可通过登录页和“我的—隐私与授权”随时查看本文件。</text></view>
</template>
<script>
import { legalDocument } from '@/common/legal-content.mjs'
export default {data(){return{document:legalDocument('privacy')}},onLoad(options){this.document=legalDocument(options?.kind==='service'?'service':'privacy');uni.setNavigationBarTitle({title:this.document.title})}}
</script>
<style scoped>.legal-page{padding:40rpx 36rpx 80rpx;background:#fffdfa;color:#423528;min-height:100vh}.title{display:block;font-size:40rpx;font-weight:600}.date{display:block;color:#8d7c6c;font-size:23rpx;line-height:1.8;margin:20rpx 0 40rpx}.section{margin:32rpx 0}.heading{display:block;font-size:29rpx;font-weight:600;margin-bottom:16rpx;line-height:1.8}.paragraph{display:block;font-size:26rpx;line-height:2;margin:14rpx 0;white-space:pre-wrap;overflow-wrap:anywhere}.footer{display:block;font-size:23rpx;color:#8d7c6c;margin-top:40rpx;line-height:1.9}</style>
