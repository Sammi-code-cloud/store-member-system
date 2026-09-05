<template>
  <div class="page-card">
    <h3 class="page-title">茶室管理 <span class="page-sub">维护可预定的茶室与包厢，顾客端预定页读取此处数据</span></h3>

    <div class="search-bar">
      <el-button type="primary" :icon="Plus"
                 :disabled="!userStore.has('reservation:manage')" @click="openDialog()">新增茶室</el-button>
      <el-button :icon="Refresh" style="margin-left:8px" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" empty-text="暂无茶室，请先新增">
      <el-table-column prop="name" label="茶室名称" min-width="160" />
      <el-table-column prop="roomType" label="类型" width="120" />
      <el-table-column prop="capacity" label="容纳人数" width="120" />
      <el-table-column label="每小时价格" width="130">
        <template #default="{ row }"><span class="mono">¥{{ row.priceHour }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '可预定' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!userStore.has('reservation:manage')"
                     @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :disabled="!userStore.has('reservation:manage')"
                     @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑茶室' : '新增茶室'" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="茶室名称" required>
          <el-input v-model="form.name" placeholder="如 观山雅室" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.roomType" placeholder="请选择" style="width:100%">
            <el-option label="包厢" value="包厢" />
            <el-option label="卡座" value="卡座" />
            <el-option label="大厅" value="大厅" />
          </el-select>
        </el-form-item>
        <el-form-item label="容纳人数">
          <el-input v-model="form.capacity" placeholder="如 4-6人" />
        </el-form-item>
        <el-form-item label="每小时价格" required>
          <el-input-number v-model="form.priceHour" :min="0" :precision="2" :step="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">可预定</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import api from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialog = ref(false)

const defaultForm = { id: null, name: '', roomType: '包厢', capacity: '', priceHour: 100, status: 1 }
const form = reactive({ ...defaultForm })

async function load() {
  loading.value = true
  try {
    list.value = await api.roomList()
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, defaultForm)
  if (row) Object.assign(form, row)
  dialog.value = true
}

async function onSave() {
  if (!form.name) return ElMessage.warning('请填写茶室名称')
  saving.value = true
  try {
    await api.roomSave({ ...form, storeId: userStore.storeId })
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除茶室「' + row.name + '」？已有预定的茶室建议改为停用。', '删除确认', { type: 'warning' })
  await api.roomDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
