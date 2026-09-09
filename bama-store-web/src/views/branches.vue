<template>
  <el-card>
    <template #header><div class="heading"><div><b>分店管理</b><p>每家分店独立设置包间、员工、营业时间和预约。</p></div><el-button type="primary" @click="open">新增分店</el-button></div></template>
    <el-table :data="filteredStores" empty-text="暂无营业中的分店">
      <el-table-column prop="name" label="分店名称" min-width="180" />
      <el-table-column label="微信到店" width="120"><template #default="{row}"><MiniProgramCode :store-id="row.id" :store-name="row.name" /></template></el-table-column>
      <el-table-column prop="address" label="地址" min-width="220" />
      <el-table-column prop="phone" label="联系电话" width="160" />
      <el-table-column label="营业时间" width="150"><template #default="{row}">{{ row.openTime }}–{{ row.closeTime }}</template></el-table-column>
      <el-table-column label="状态" width="120"><template #default="{row}"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '营业中' : '暂停营业' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="340" fixed="right"><template #default="{row}"><el-button link type="primary" :disabled="busyId !== null" @click="enter(row, '/room')">管理包间</el-button><el-button link :disabled="busyId !== null" @click="enter(row, '/store')">门店设置</el-button><el-button v-if="user.has('store:all')" link :type="row.status === 1 ? 'warning' : 'success'" :disabled="busyId !== null" @click="setStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button><el-tooltip v-if="user.has('store:all')" :disabled="row.status === 0" content="请先停用门店再删除"><span><el-button link type="danger" :disabled="row.status !== 0 || busyId !== null" @click="remove(row)">删除</el-button></span></el-tooltip></template></el-table-column>
    </el-table>
    <el-alert title="新增分店后，请先配置该店包间与员工；顾客账号和会员余额跨分店共享。" type="info" :closable="false" style="margin-top:20px" />
    <el-alert title="列表仅显示营业中的分店，已暂停营业和已删除的门店不显示。客户账号、会员余额及历史记录保留。" type="info" :closable="false" style="margin-top:12px" />
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
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import api from '@/api'
import MiniProgramCode from '@/components/MiniProgramCode.vue'
const user = useUserStore(), router = useRouter(), visible = ref(false), saving = ref(false), form = ref({})
const busyId = ref(null)
const filteredStores = computed(() => user.stores.filter(store => store.status === 1 && !store.deleted))
function open() { form.value = { name:'', address:'', phone:'', openTime:'10:00', closeTime:'22:00', status:0, reservationNotice:'' }; visible.value=true }
async function save() {
  if (!form.value.name.trim()) return ElMessage.warning('请填写分店名称')
  if (!form.value.openTime || !form.value.closeTime || form.value.closeTime <= form.value.openTime) return ElMessage.warning('请设置有效营业时间')
  saving.value=true
  try { await api.storeCreate(form.value); await user.loadStores(); visible.value=false; ElMessage.success(form.value.status === 1 ? '分店已创建，可配置包间和员工' : '分店已创建，暂停营业的门店不在列表显示') } finally { saving.value=false }
}
async function enter(row, path) { user.selectStore(row.id); await router.push({path, query:{}}) }
async function setStatus(row) {
  if (busyId.value !== null) return
  const status = row.status === 1 ? 0 : 1, action = status === 1 ? '启用' : '停用'
  try { await ElMessageBox.confirm(status === 0 ? `停用“${row.name}”后，该店及所属包间不再对顾客开放，不能新建预约；客户账号、会员余额、已有预约和账目全部保留，客户仍可在其他营业分店消费。` : `确认恢复“${row.name}”营业？原已启用的包间将恢复接受预约，单独停用的包间保持停用。`, `${action}门店`, { type:'warning', confirmButtonText:`确认${action}`, cancelButtonText:'取消' }) } catch { return }
  busyId.value = row.id
  try { await api.storeStatus(row.id, status); await user.loadStores(); ElMessage.success(`门店已${action}`) } finally { busyId.value = null }
}
async function remove(row) {
  if (busyId.value !== null || row.status !== 0) return
  try { await ElMessageBox.confirm(`确认删除已停用的“${row.name}”？存在关联数据的门店无法删除。`, '删除门店', { type:'warning', confirmButtonText:'确认删除', cancelButtonText:'取消' }) } catch { return }
  busyId.value = row.id
  try {
    await api.storeDelete(row.id)
    if (user.storeId === row.id) user.selectStore(null)
    await user.loadStores()
    ElMessage.success('门店已删除')
  } finally { busyId.value = null }
}
</script>
<style scoped>.heading{display:flex;align-items:center;justify-content:space-between}.heading p{font-size:13px;color:#7a8089;margin-bottom:0}</style>
