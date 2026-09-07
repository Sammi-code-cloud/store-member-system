<template>
  <el-container class="layout">
    <!-- 侧边栏：按权限过滤菜单，无权限的功能不显示 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="mark">八马</span>
        <span class="txt">门店管理后台</span>
      </div>
      <el-menu :default-active="route.path" router class="menu"
               background-color="#2b2f38" text-color="#c9ced6" active-text-color="#fff">
        <template v-for="(items, group) in menuGroups" :key="group">
          <div class="group-title">{{ group }}</div>
          <el-menu-item v-for="m in items" :key="m.path" :index="m.path">
            <el-icon><component :is="m.meta.icon" /></el-icon>
            <span>{{ m.meta.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="crumb">{{ route.meta.title || '' }}</div>
        <el-select :model-value="userStore.storeId" @change="switchStore" :disabled="!userStore.has('store:all')" style="width:260px" aria-label="当前分店">
          <el-option v-for="s in userStore.stores" :key="s.id" :value="s.id" :label="s.name + (s.status === 0 ? '（暂停营业）' : '')" />
        </el-select>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-icon><UserFilled /></el-icon>
            {{ userStore.name || '未登录' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>工号 {{ userStore.user?.staffNo }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view v-if="ready" :key="userStore.storeId" />
        <el-result v-else title="正在加载分店" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const ready = ref(false)
onMounted(async () => { await userStore.loadStores(); ready.value = true })
async function switchStore(id) {
  ready.value = false
  await router.replace({path: route.path, query: {}})
  userStore.selectStore(id)
  ready.value = true
}

// 取出主布局下的子路由，按 group 分组，并按权限码过滤
const menuGroups = computed(() => {
  const children = router.options.routes.find(r => r.path === '/')?.children || []
  const groups = {}
  children.forEach(r => {
    if (r.meta?.hidden) return
    if (!userStore.has(r.meta.perm)) return       // 无权限则不显示
    const g = r.meta.group || '其他'
    ;(groups[g] = groups[g] || []).push({ ...r, path: '/' + r.path })
  })
  return groups
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #2b2f38; overflow-y: auto; }
.logo { height: 56px; display: flex; align-items: center; gap: 8px; padding: 0 18px; color: #fff; }
.logo .mark { background: var(--bm-brand); border-radius: 4px; padding: 2px 6px; font-size: 13px; font-weight: 700; }
.logo .txt { font-size: 15px; font-weight: 600; }
.menu { border-right: none; }
.group-title { color: #6c7280; font-size: 12px; padding: 14px 18px 6px; }
.header { background: #fff; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e8eaed; height: 56px; }
.crumb { font-size: 15px; font-weight: 600; color: #1f2329; }
.user { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #4a5057; font-size: 14px; }
.main { padding: 16px; overflow-y: auto; }
</style>
