<template>
  <div class="page-card" style="max-width:620px">
    <h3 class="page-title">门店设置 <span class="page-sub">门店信息将展示在顾客端小程序首页</span></h3>

    <el-form :model="form" label-width="100px" v-loading="loading">
      <el-form-item label="门店名称" required>
        <el-input v-model="form.name" placeholder="如 五缘湾旗舰店" />
      </el-form-item>
      <el-form-item label="门店地址">
        <el-input v-model="form.address" type="textarea" :rows="2" placeholder="门店详细地址" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.phone" placeholder="如 0592-8888888" />
      </el-form-item>
      <el-form-item label="营业状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">正常营业</el-radio>
          <el-radio :value="0">暂停营业</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving"
                   :disabled="!userStore.has('store:manage')" @click="onSave">保存设置</el-button>
        <el-button @click="load">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" show-icon style="margin-top:8px"
              title="当前仅支持单门店配置，多门店管理需后端补充门店列表接口后开放。" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
// 门店 ID 取当前登录员工所属门店
const storeId = userStore.storeId
const form = reactive({ name: '', address: '', phone: '', status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await api.storeDetail(storeId)
    Object.assign(form, data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.name) return ElMessage.warning('请填写门店名称')
  saving.value = true
  try {
    await api.storeUpdate(storeId, { ...form })
    ElMessage.success('保存成功')
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
