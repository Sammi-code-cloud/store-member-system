<template>
  <div>
    <div class="banner-heading"><div><h2>首页 Banner</h2><p>管理当前分店的首页轮播图，保存后顾客重新进入首页即可看到。</p></div><el-button type="primary" @click="edit()" :disabled="items.length>=10">新增 Banner</el-button></div>
    <el-alert title="建议尺寸 1500 × 750（2:1），支持 JPG / PNG，单张不超过 2MB；最多 10 张。排序越小越靠前。" type="info" :closable="false" />
    <el-table :data="items" v-loading="loading" style="margin-top:20px">
      <el-table-column label="图片" width="230"><template #default="{row}"><img :src="imageUrl(row.imageUrl)" class="thumb" :alt="row.title" /></template></el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="点击跳转" width="120"><template #default="{row}">{{row.target==='rooms'?'预订茶室':'不跳转'}}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'">{{row.status===1?'已启用':'已停用'}}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="210"><template #default="{row}"><el-button link type="primary" @click="edit(row)">编辑 / 换图</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
      <template #empty>暂无 Banner，点击右上角上传第一张图片</template>
    </el-table>
    <el-dialog v-model="visible" :title="form.id?'编辑 Banner':'新增 Banner'" width="600px" :close-on-click-modal="false" :before-close="close">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="60" show-word-limit /></el-form-item>
        <el-form-item label="Banner 图片"><div class="upload-area"><img v-if="preview" :src="preview" class="preview" alt="Banner预览"/><input type="file" accept="image/jpeg,image/png" @change="choose" :disabled="saving" aria-label="上传 Banner 图片"/><span>选择新图片即可替换；不选择则保留原图。</span></div></el-form-item>
        <div class="form-row"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="999"/></el-form-item><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用"/></el-form-item></div>
        <el-form-item label="点击跳转"><el-select v-model="form.target"><el-option value="none" label="不跳转"/><el-option value="rooms" label="预订茶室"/></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false" :disabled="saving">取消</el-button><el-button type="primary" @click="save" :loading="saving" :disabled="reading">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import {ref,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import request from '@/utils/request'
const items=ref([]),loading=ref(false),visible=ref(false),saving=ref(false),reading=ref(false),form=ref({}),preview=ref('')
const imageUrl=url=>request.defaults.baseURL.replace(/\/$/,'')+url.replace(/^\/api/,'')
async function load(){loading.value=true;try{items.value=await request.get('/banners')}finally{loading.value=false}}
function edit(row){form.value=row?{id:row.id,title:row.title,sortOrder:row.sortOrder,status:row.status,target:row.target}:{title:'',sortOrder:items.value.length,status:1,target:'none'};preview.value=row?imageUrl(row.imageUrl):'';visible.value=true}
function close(done){if(!saving.value)done()}
async function choose(event){const file=event.target.files[0];if(!file)return;if(!['image/jpeg','image/png'].includes(file.type)||file.size>2*1024*1024){event.target.value='';return ElMessage.warning('请选择2MB以内的 JPG / PNG 图片')};reading.value=true;try{const data=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reject;reader.readAsDataURL(file)});form.value.image=data;preview.value=data}catch{ElMessage.error('图片读取失败，请重新选择')}finally{reading.value=false}}
async function save(){if(saving.value||reading.value)return;if(!form.value.title.trim()||(!form.value.id&&!form.value.image))return ElMessage.warning('请填写标题并上传图片');saving.value=true;try{await request.post('/banners',form.value);visible.value=false;ElMessage.success('Banner已保存');await load()}finally{saving.value=false}}
async function remove(row){try{await ElMessageBox.confirm('删除后将不再展示该图片，是否继续？','删除 Banner',{type:'warning'});}catch{return}await request.delete('/banners/'+row.id);ElMessage.success('已删除');await load()}
onMounted(load)
</script>
<style scoped>
.banner-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.banner-heading h2{margin:0 0 8px}.banner-heading p{margin:0;color:#8c8178}.thumb{width:200px;height:100px;object-fit:cover;border-radius:8px}.upload-area{display:flex;flex-direction:column;gap:12px;width:100%;color:#928479;font-size:12px}.preview{width:100%;aspect-ratio:2;object-fit:cover;border-radius:12px}.form-row{display:flex;gap:36px}
</style>
