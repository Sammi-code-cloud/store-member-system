<template>
  <div class="page-card">
    <h3 class="page-title">员工与权限 <span class="page-sub">维护员工资料、角色和可访问分店；权限变更立即生效</span></h3>

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
      <el-table-column label="可访问分店" min-width="200">
        <template #default="{ row }">
          <el-tag v-if="row.allStores" size="small" type="warning">全部分店（总部权限）</el-tag>
          <template v-else><el-tag v-for="name in row.storeNames" :key="name" size="small" style="margin:2px">{{ name }}</el-tag></template>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="!userStore.has('staff:manage')"
                     @change="v => onToggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!userStore.has('staff:manage')" @click="openDialog(row)">编辑</el-button>
          <el-button link type="primary" :disabled="!userStore.has('staff:manage')"
                     @click="onResetPassword(row)">重置密码</el-button>
          <el-button link type="primary" :disabled="!userStore.has('staff:manage') || row.status !== 1" @click="openBindCode(row)">绑定微信</el-button>
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
        <el-form-item label="可访问分店" required>
          <el-select v-model="form.storeIds" multiple placeholder="选择员工可切换的分店" style="width:100%">
            <el-option v-for="s in userStore.stores" :key="s.id" :value="s.id"
                       :label="s.name + (s.id === form.homeStoreId ? '（所属分店）' : '')" :disabled="s.id === form.homeStoreId" />
          </el-select>
          <p class="scope-tip">所属分店始终保留。只能授予你有权访问的分店，员工切换后查看对应分店的数据。</p>
          <p v-if="hasHeadquartersRole" class="scope-tip">总部管理员角色可访问全部分店（含以后新增的分店）；多选范围用于移除总部角色后的访问权限。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindVisible" title="员工微信绑定码" width="420px" align-center>
      <div class="bind-code" v-loading="bindLoading">
        <h3>{{ bindStaff.name }}</h3>
        <img v-if="bindImage" :src="bindImage" alt="员工微信绑定二维码" />
        <el-alert v-if="bindError" :title="bindError" type="warning" :closable="false" />
        <p>员工使用本人微信扫一扫，输入登记的手机号即可绑定，无需密码。二维码 10 分钟内有效，只能绑定一次，请仅交给对应员工。</p>
        <a v-if="bindImage" :href="bindImage" :download="bindStaff.name + '-微信绑定码.' + (bindImage.startsWith('data:image/png') ? 'png' : 'jpg')">下载二维码</a>
        <el-button v-if="bindError" :loading="bindLoading" @click="openBindCode(bindStaff)">重新生成</el-button>
        <el-button v-else-if="bindImage" :loading="bindLoading" @click="openBindCode(bindStaff)">刷新绑定码</el-button>
      </div>
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
const bindVisible = ref(false), bindLoading = ref(false), bindImage = ref(''), bindError = ref(''), bindStaff = ref({})
let bindRequest = 0
async function openBindCode(row) {
  const requestId = ++bindRequest
  bindStaff.value = {id: row.id, name: row.name}
  bindVisible.value = true; bindLoading.value = true; bindImage.value = ''; bindError.value = ''
  try {
    const result = await api.staffWechatCode(row.id)
    if (requestId === bindRequest) bindImage.value = result.image
  } catch (e) {
    if (requestId === bindRequest) bindError.value = e.message || '二维码生成失败，请重试'
  } finally { if (requestId === bindRequest) bindLoading.value = false }
}

const form = reactive({ id: null, name: '', phone: '', password: '', roleIds: [], storeIds: [], homeStoreId: null })
const hasHeadquartersRole = computed(() => roles.value.some(r => r.code === 'HEADQUARTERS' && form.roleIds.includes(r.id)))

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

async function openDialog(row) {
  await userStore.loadStores()
  const homeStoreId = row?.storeId || userStore.storeId
  const storeIds = row?.storeIds ? [...row.storeIds] : [homeStoreId]
  if (storeIds.some(id => !userStore.stores.some(s => s.id === id))) return ElMessage.warning('该员工的分店范围超出你的权限，请联系总部管理员修改')
  Object.assign(form, { id: row?.id || null, name: row?.name || '', phone: row?.phone || '', password: '', roleIds: row?.roleIds ? [...row.roleIds] : [], storeIds, homeStoreId })
  dialog.value = true
}

async function onSave() {
  if (!form.name.trim() || !/^1[3-9]\d{9}$/.test(form.phone)) return ElMessage.warning('请填写姓名和正确的手机号')
  if (!form.id && (form.password.length < 8 || form.password.length > 64)) return ElMessage.warning('密码需为 8–64 位')
  if (!form.roleIds.length) return ElMessage.warning('请至少选择一个角色')
  if (!form.storeIds.length || !form.storeIds.includes(form.homeStoreId)) return ElMessage.warning('请选择可访问分店，并保留所属分店')
  saving.value = true
  try {
    let createdId = null
    if (form.id) await api.staffUpdate(form.id, { name: form.name, phone: form.phone, roleIds: form.roleIds, storeIds: form.storeIds })
    else createdId = await api.staffCreate({ ...form, storeId: userStore.storeId })
    ElMessage.success(form.id ? '员工资料已更新' : '员工创建成功')
    dialog.value = false
    if (createdId) await openBindCode({id: createdId, name: form.name})
    await userStore.loadStores()
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
.bind-code{text-align:center;min-height:280px}.bind-code img{width:258px;height:258px;max-width:100%;object-fit:contain}.bind-code p{font-size:13px;line-height:1.8;color:#827568}.bind-code a{color:var(--el-color-primary)}
.drawer-tip { font-size: 13px; color: #8a9099; line-height: 1.7; margin: 0 0 16px; }
.scope-tip { font-size: 12px; line-height: 1.7; color: #737b87; margin: 6px 0 0; }
.perm-group { margin-bottom: 16px; }
.perm-group .mod { font-size: 13px; font-weight: 600; color: #1f2329; margin-bottom: 8px; }
.code { color: #a4a9b0; font-size: 11px; margin-left: 4px; }
</style>
