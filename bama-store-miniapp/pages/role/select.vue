<template><view><text>正在进入茶生活…</text></view></template>
<script>
// Preserve old launch links while sending customers directly to the storefront.
export default {onLoad(){uni.reLaunch({url:'/pages/customer/home'})}}
</script>
