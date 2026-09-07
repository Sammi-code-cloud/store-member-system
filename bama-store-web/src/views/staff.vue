<template>
  <div class="page-card">
    <h3 class="page-title">员工与权限 <span class="page-sub">添加员工、维护资料和分配角色；停用后立即停止访问</span></h3>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="姓名 / 手机号 / 工号" clearable
                style="width:280px" @keyup.enter="onSearch">
        <template #append><el-button :icon="Search" @click="onSearch" /></template>
      </el-input>
      <el-button type="primary" :icon="Plus" style="margin-left:12px"
                 :disabled="!userStore.has('staff:manage')" @click="openDialog">新增员工</el-button>
      <el-button style="margin-left:8px" @click="permDrawer = true">查看权限清单</el-button>
    </div>

    <el-table :data="list" v-loading="loading" empty-text="暂无员工数据">
      <el-table-column prop="staffNo" label="工号" width="140" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号（登录账号）" width="180" />
      <el-table-column label="角色" min-width="170"><template #default="{ row }"><el-tag v-for="name in row.roleNames" :key="name" size="small" style="margin:2px">{{ name }}</el-tag></template></el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="!userStore.has('staff:manage')"
                     @change="v => onToggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!userStore.has('staff:manage')" @click="openDialog(row)">编辑</el-button>
          <el-button link type="primary" :disabled="!userStore.has('staff:manage')"
                     @click="onResetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:12px" layout="total, prev, pager, next"
                   :total="total" :page-size="pageSize" :current-page="pageNum"
                   @current-change="onPageChange" />

    <!-- 新增员工 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑员工' : '新增员工'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" placeholder="将作为员工端登录账号" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" required>
          <el-input v-model="form.password" type="password" show-password maxlength="64" placeholder="8–64 位密码" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleIds" multiple placeholder="可多选" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name + '（' + r.remark + '）'" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限清单 -->
    <el-drawer v-model="permDrawer" title="系统权限清单" size="460px">
      <p class="drawer-tip">权限按模块划分。员工分配预设角色后，员工端与后台的功能入口会自动按角色控制。</p>
      <div v-for="(items, mod) in permGroups" :key="mod" class="perm-group">
        <div class="mod">{{ mod }}</div>
        <el-tag v-for="p in items" :key="p.id" size="small" style="margin:0 6px 6px 0">
          {{ p.name }} <span class="code">{{ p.code }}</span>
        </el-tag>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
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
const permDrawer = ref(false)
const roles = ref([])
const permissions = ref([])

const form = reactive({ id: null, name: '', phone: '', password: '', roleIds: [] })

// 权限按 module 分组展示
const permGroups = computed(() => {
  const g = {}
  permissions.value.forEach(p => {
    const m = p.module || '其他'
    if (!g[m]) g[m] = []
    g[m].push(p)
  })
  return g
})

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (keyword.value) params.keyword = keyword.value
    const res = await api.staffPage(params)
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
  Object.assign(form, { id: row?.id || null, name: row?.name || '', phone: row?.phone || '', password: '', roleIds: row?.roleIds ? [...row.roleIds] : [] })
  dialog.value = true
}

async function onSave() {
  if (!form.name.trim() || !/^1[3-9]\d{9}$/.test(form.phone)) return ElMessage.warning('请填写姓名和正确的手机号')
  if (!form.id && (form.password.length < 8 || form.password.length > 64)) return ElMessage.warning('密码需为 8–64 位')
  if (!form.roleIds.length) return ElMessage.warning('请至少选择一个角色')
  saving.value = true
  try {
    if (form.id) await api.staffUpdate(form.id, { name: form.name, phone: form.phone, roleIds: form.roleIds })
    else await api.staffCreate({ ...form, storeId: userStore.storeId })
    ElMessage.success(form.id ? '员工资料已更新' : '员工创建成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

// 启用 / 停用：停用后该员工无法登录员工端
async function onToggle(row, checked) {
  try {
    await ElMessageBox.confirm((checked ? '启用' : '停用') + '员工「' + row.name + '」？', '账号状态', { type: 'warning' })
    await api.staffStatus(row.id, checked ? 1 : 0)
    ElMessage.success(checked ? '已启用' : '已停用')
    await load()
  } catch {}
}

async function onResetPassword(row) {
  try {
  const { value } = await ElMessageBox.prompt(
    '为 ' + row.name + ' 设置新密码', '重置密码',
    { inputType: 'password', inputPlaceholder: '8–64 位新密码', inputPattern: /^.{8,64}$/, inputErrorMessage: '密码需为 8–64 位' }
  )
  await api.staffResetPassword(row.id, value)
  ElMessage.success('密码已重置')
  } catch {}
}

onMounted(async () => {
  load()
  try {
    roles.value = await api.roles()
    permissions.value = await api.permissions()
  } catch (e) {
    // 无权限查看角色时忽略
  }
})
</script>

<style scoped>
.drawer-tip { font-size: 13px; color: #8a9099; line-height: 1.7; margin: 0 0 16px; }
.perm-group { margin-bottom: 16px; }
.perm-group .mod { font-size: 13px; font-weight: 600; color: #1f2329; margin-bottom: 8px; }
.code { color: #a4a9b0; font-size: 11px; margin-left: 4px; }
</style>
