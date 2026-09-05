<template>
  <div class="page-card">
    <h3 class="page-title">会员管理 <span class="page-sub">查看会员资料、卡内余额与消费统计</span></h3>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="姓名 / 手机号 / 会员卡号" clearable
                style="width:300px" @keyup.enter="onSearch">
        <template #append><el-button :icon="Search" @click="onSearch" /></template>
      </el-input>
    </div>

    <el-table :data="list" v-loading="loading" empty-text="暂无会员数据">
      <el-table-column prop="memberNo" label="会员卡号" width="140" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="等级" width="110">
        <template #default="{ row }">
          <el-tag :type="levelTag(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="折扣" width="90">
        <template #default="{ row }">{{ row.discount }} 折</template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showAccount(row)">账户详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:12px" layout="total, prev, pager, next"
                   :total="total" :page-size="pageSize" :current-page="pageNum"
                   @current-change="onPageChange" />

    <!-- 账户详情抽屉 -->
    <el-drawer v-model="drawer" title="会员账户详情" size="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="姓名">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="会员卡号">{{ current.memberNo }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ current.phone }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          {{ levelText(current.level) }} · {{ current.discount }} 折
        </el-descriptions-item>
        <el-descriptions-item label="卡内余额">
          <b class="mono" style="color:var(--bm-brand);font-size:18px">¥{{ account.balance ?? '-' }}</b>
        </el-descriptions-item>
        <el-descriptions-item label="累计充值">
          <span class="mono">¥{{ account.totalRecharge ?? '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="累计消费">
          <span class="mono">¥{{ account.totalConsume ?? '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-button type="primary" style="margin-top:18px;width:100%"
                 :disabled="!userStore.has('account:recharge')" @click="goRecharge">
        去为该会员充值
      </el-button>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import api from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')
const drawer = ref(false)
const current = ref({})
const account = ref({})

// 会员等级中文映射，与小程序端保持一致
function levelText(l) {
  return { BLACK_GOLD: '黑金卡', GOLD: '金卡', NORMAL: '普通' }[l] || l || '普通'
}
function levelTag(l) {
  return { BLACK_GOLD: 'danger', GOLD: 'warning', NORMAL: 'info' }[l] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (keyword.value) params.keyword = keyword.value
    const res = await api.memberPage(params)
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

async function showAccount(row) {
  current.value = row
  account.value = await api.memberAccount(row.id)
  drawer.value = true
}

function goRecharge() {
  router.push('/recharge')
}

onMounted(load)
</script>
