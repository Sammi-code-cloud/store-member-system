<template>
  <div class="page-card">
    <h3 class="page-title">货品管理 <span class="page-sub">录入茶品与周边货品，供小程序商城与扫码结算使用</span></h3>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="货品名称 / 条码" clearable
                style="width:280px" @keyup.enter="onSearch">
        <template #append><el-button :icon="Search" @click="onSearch" /></template>
      </el-input>
      <el-button type="primary" :icon="Plus" style="margin-left:12px"
                 :disabled="!userStore.has('product:manage')" @click="openDialog()">新增货品</el-button>
    </div>

    <el-table :data="list" v-loading="loading" empty-text="暂无货品数据">
      <el-table-column prop="name" label="货品名称" min-width="160" />
      <el-table-column prop="barcode" label="条码" width="150" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column prop="spec" label="规格" width="110" />
      <el-table-column label="零售价" width="100">
        <template #default="{ row }"><span class="mono">¥{{ row.retailPrice }}</span></template>
      </el-table-column>
      <el-table-column label="会员价" width="100">
        <template #default="{ row }"><span class="mono">¥{{ row.memberPrice }}</span></template>
      </el-table-column>
      <el-table-column label="库存" width="110">
        <template #default="{ row }">
          <span :class="{ warn: row.stock <= row.warnStock }">{{ row.stock }}</span>
          <el-tag v-if="row.stock <= row.warnStock" type="danger" size="small" style="margin-left:6px">低库存</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="!userStore.has('product:manage')"
                     @change="v => onToggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!userStore.has('product:manage')"
                     @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :disabled="!userStore.has('product:manage')"
                     @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:12px" layout="total, prev, pager, next"
                   :total="total" :page-size="pageSize" :current-page="pageNum"
                   @current-change="onPageChange" />

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑货品' : '新增货品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="货品名称" required>
          <el-input v-model="form.name" placeholder="如 特级铁观音" />
        </el-form-item>
        <el-form-item label="条码">
          <el-input v-model="form.barcode" placeholder="扫码枪可直接录入，需保证唯一" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如 乌龙茶 / 红茶 / 茶具" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" placeholder="如 250g/盒" />
        </el-form-item>
        <el-form-item label="零售价" required>
          <el-input-number v-model="form.retailPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="会员价">
          <el-input-number v-model="form.memberPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="预警库存">
          <el-input-number v-model="form.warnStock" :min="0" />
          <span class="tip">库存低于该值时列表标红提示</span>
        </el-form-item>
        <el-form-item label="销售渠道">
          <el-select v-model="form.channel" placeholder="请选择" style="width:200px">
            <el-option label="门店 + 商城" value="ALL" />
            <el-option label="仅门店" value="STORE" />
            <el-option label="仅商城" value="MALL" />
          </el-select>
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
import { Search, Plus } from '@element-plus/icons-vue'
import api from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')
const dialog = ref(false)

const defaultForm = {
  id: null, name: '', barcode: '', category: '', spec: '',
  retailPrice: 0, memberPrice: 0, stock: 0, warnStock: 10, channel: 'ALL', status: 1
}
const form = reactive({ ...defaultForm })

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (keyword.value) params.keyword = keyword.value
    const res = await api.productPage(params)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  load()
}
function onPageChange(p) {
  pageNum.value = p
  load()
}

function openDialog(row) {
  Object.assign(form, defaultForm)
  if (row) Object.assign(form, row)
  dialog.value = true
}

async function onSave() {
  if (!form.name) return ElMessage.warning('请填写货品名称')
  saving.value = true
  try {
    await api.productSave({ ...form, storeId: userStore.storeId })
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 上架 / 下架切换
async function onToggle(row, checked) {
  await api.productStatus(row.id, checked ? 1 : 0)
  ElMessage.success(checked ? '已上架' : '已下架')
  load()
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除货品「' + row.name + '」？', '删除确认', { type: 'warning' })
  await api.productDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.warn { color: #d9534f; font-weight: 600; }
.tip { margin-left: 10px; font-size: 12px; color: #a4a9b0; }
</style>
