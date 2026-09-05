<template>
  <div class="page-card">
    <h3 class="page-title">预定核销 <span class="page-sub">查看茶室预定并核销到店，等同员工端工作台的核销功能</span></h3>

    <div class="search-bar">
      <el-radio-group v-model="status" @change="onFilter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="WAITING">待核销</el-radio-button>
        <el-radio-button value="VERIFIED">已核销</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" style="margin-left:12px" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" v-loading="loading" empty-text="暂无预定记录">
      <el-table-column prop="orderNo" label="预定单号" width="180" />
      <el-table-column prop="reserveDate" label="预定日期" width="120" />
      <el-table-column prop="startTime" label="开始时段" width="100" />
      <el-table-column label="时长" width="90">
        <template #default="{ row }">{{ row.hours }} 小时</template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span class="mono">¥{{ row.amount }}</span></template>
      </el-table-column>
      <el-table-column label="茶室" width="140">
        <template #default="{ row }">{{ roomName(row.roomId) }}</template>
      </el-table-column>
      <el-table-column prop="memberId" label="会员ID" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'VERIFIED' ? 'success' : 'warning'" size="small">
            {{ row.status === 'VERIFIED' ? '已核销' : '待核销' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary"
                     :disabled="row.status === 'VERIFIED' || !userStore.has('reservation:verify')"
                     @click="onVerify(row)">核销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:12px" layout="total, sizes, prev, pager, next"
                   :total="total" :page-size="pageSize" :current-page="pageNum"
                   :page-sizes="[10, 20, 50]"
                   @size-change="onSizeChange" @current-change="onPageChange" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const rooms = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('WAITING')

// 茶室 ID 转名称展示
function roomName(id) {
  const r = rooms.value.find(x => x.id === id)
  return r ? r.name : '#' + id
}

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (status.value) params.status = status.value
    const res = await api.reservationPage(params)
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onFilter() {
  pageNum.value = 1
  load()
}
function onPageChange(p) {
  pageNum.value = p
  load()
}
function onSizeChange(s) {
  pageSize.value = s
  pageNum.value = 1
  load()
}

// 核销：调用与员工端相同的接口 POST /api/reservations/{id}/verify
async function onVerify(row) {
  await ElMessageBox.confirm('确认核销预定单 ' + row.orderNo + '？核销后不可撤销。', '核销确认', { type: 'warning' })
  await api.reservationVerify(row.id)
  ElMessage.success('核销成功')
  load()
}

onMounted(async () => {
  try {
    rooms.value = await api.roomList()
  } catch (e) {
    rooms.value = []
  }
  load()
})
</script>
