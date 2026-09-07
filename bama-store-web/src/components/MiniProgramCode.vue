<template>
  <el-button @click="open">小程序码</el-button>
  <el-dialog v-model="visible" title="微信扫码到店" width="420px" align-center>
    <div class="mini-code" v-loading="loading">
      <h3>{{ storeName }}</h3>
      <img v-if="image" :src="image" alt="用微信扫一扫打开本店小程序" />
      <el-empty v-else-if="error" :description="error" :image-size="64" />
      <p>顾客使用微信扫一扫，打开小程序后即可微信登录、预订茶室。</p>
      <a v-if="image" :href="image" :download="storeName + '-小程序码.' + (image.startsWith('data:image/png') ? 'png' : 'jpg')">下载小程序码</a>
      <el-button v-if="error" :loading="loading" @click="open">重新生成</el-button>
    </div>
  </el-dialog>
</template>
<script setup>
import { ref } from 'vue'
import api from '@/api'
const props = defineProps({ storeId: { type: Number, required: true }, storeName: { type: String, default: '' } })
const visible = ref(false), loading = ref(false), image = ref(''), error = ref('')
async function open() {
  if (loading.value) return
  visible.value = true; loading.value = true; image.value = ''; error.value = ''
  try { image.value = (await api.storeMiniCode(props.storeId)).image }
  catch(e) { error.value = e.message || '小程序码生成失败，请重试' }
  finally { loading.value = false }
}
</script>
<style scoped>
.mini-code{text-align:center;min-height:260px}.mini-code h3{font-size:18px;margin-top:0}.mini-code img{display:block;width:258px;height:258px;max-width:100%;object-fit:contain;margin:0 auto}.mini-code p{color:#7a8089;font-size:13px;line-height:1.8}.mini-code a{display:inline-block;margin-top:10px;color:var(--el-color-primary)}
</style>
