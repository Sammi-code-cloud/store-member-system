<template>
  <el-card>
    <template #header><div class="heading"><div><b>分店管理</b><p>每家分店独立设置包间、员工、营业时间和预约。</p></div><el-button type="primary" @click="open">新增分店</el-button></div></template>
    <el-table :data="user.stores">
      <el-table-column prop="name" label="分店名称" min-width="180" />
      <el-table-column prop="address" label="地址" min-width="220" />
      <el-table-column prop="phone" label="联系电话" width="160" />
      <el-table-column label="营业时间" width="150"><template #default="{row}">{{ row.openTime }}–{{ row.closeTime }}</template></el-table-column>
      <el-table-column label="状态" width="120"><template #default="{row}"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '营业中' : '暂停营业' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="210"><template #default="{row}"><el-button link type="primary" @click="enter(row, '/room')">管理包间</el-button><el-button link @click="enter(row, '/store')">门店设置</el-button></template></el-table-column>
    </el-table>
    <el-alert title="新增分店后，请先配置该店包间与员工；顾客账号和会员余额跨分店共享。" type="info" :closable="false" style="margin-top:20px" />
  </el-card>
  <el-dialog v-model="visible" title="新增分店" width="540px" :close-on-click-modal="false">
    <el-form label-width="100px" @submit.prevent="save">
      <el-form-item label="分店名称" required><el-input v-model="form.name" maxlength="64" placeholder="例如：八马茶业·湖里店" /></el-form-item>
      <el-form-item label="地址"><el-input v-model="form.address" maxlength="255" /></el-form-item>
      <el-form-item label="联系电话"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
      <el-form-item label="营业时间" required><el-time-select v-model="form.openTime" start="00:00" end="23:30" step="00:30" style="width:150px" /><span style="margin:0 10px">至</span><el-time-select v-model="form.closeTime" start="00:30" end="23:30" step="00:30" style="width:150px" /></el-form-item>
      <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="0">暂停营业</el-radio><el-radio :value="1">营业中</el-radio></el-radio-group></el-form-item>
      <el-form-item label="预约须知"><el-input v-model="form.reservationNotice" type="textarea" maxlength="1000" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存分店</el-button></template>
  </el-dialog>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import api from '@/api'
const user = useUserStore(), router = useRouter(), visible = ref(false), saving = ref(false), form = ref({})
function open() { form.value = { name:'', address:'', phone:'', openTime:'10:00', closeTime:'22:00', status:0, reservationNotice:'' }; visible.value=true }
async function save() {
  if (!form.value.name.trim()) return ElMessage.warning('请填写分店名称')
  if (!form.value.openTime || !form.value.closeTime || form.value.closeTime <= form.value.openTime) return ElMessage.warning('请设置有效营业时间')
  saving.value=true
  try { await api.storeCreate(form.value); await user.loadStores(); visible.value=false; ElMessage.success('分店已创建，可配置包间和员工') } finally { saving.value=false }
}
async function enter(row, path) { await router.push({path, query:{}}); user.selectStore(row.id) }
</script>
<style scoped>.heading{display:flex;align-items:center;justify-content:space-between}.heading p{font-size:13px;color:#7a8089;margin-bottom:0}</style>
