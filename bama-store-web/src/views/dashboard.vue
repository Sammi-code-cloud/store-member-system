<template>
  <div>
    <!-- 统计卡片：数据来自员工端同一接口 GET /api/dashboard -->
    <el-row :gutter="14">
      <el-col :span="8" v-for="s in stats" :key="s.label">
        <div class="stat" :style="{ borderTopColor: s.color }">
          <div class="lab">{{ s.label }}</div>
          <div class="val mono">{{ s.value }}</div>
          <div class="sub">{{ s.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card" style="margin-top:14px">
      <h3 class="page-title">今日待核销预定 <span class="page-sub">来自「预定核销」，可直接处理</span></h3>
      <el-table :data="todayList" v-loading="loading" size="default" empty-text="今日暂无待核销预定">
        <el-table-column prop="orderNo" label="预定单号" width="180" />
        <el-table-column prop="reserveDate" label="日期" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="100" />
        <el-table-column prop="hours" label="时长(小时)" width="100" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!userStore.has('reservation:verify')"
                       @click="onVerify(row)">核销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const data = ref({})
const todayList = ref([])

const stats = computed(() => [
  { label: '会员卡余额总额', value: '¥' + (data.value.totalBalance ?? '-'), sub: '全部会员卡内余额合计', color: '#8C1F28' },
  { label: '会员总数',      value: data.value.memberCount ?? '-',           sub: '已注册会员人数',        color: '#B99A5B' },
  { label: '在售货品数',    value: data.value.productCount ?? '-',          sub: '状态为上架的货品',      color: '#3F6B4B' },
  { label: '今日预定',      value: data.value.todayReservations ?? '-',     sub: '今日新增预定单',        color: '#3F6B4B' },
  { label: '今日消费笔数',  value: data.value.todayConsumeCount ?? '-',     sub: '今日扫码收款笔数',      color: '#8C1F28' },
  { label: '今日消费金额',  value: '¥' + (data.value.todayConsumeAmount ?? '-'), sub: '今日扣款金额合计',  color: '#B99A5B' }
])

async function load() {
  loading.value = true
  try {
    data.value = await api.dashboard()
    // 拉取待核销预定（状态 RESERVED），仅取当日
    const today = new Date().toISOString().slice(0, 10)
    const page = await api.reservationPage({ pageNum: 1, pageSize: 50, status: 'WAITING' })
    todayList.value = (page.records || []).filter(r => r.reserveDate === today)
  } finally {
    loading.value = false
  }
}

async function onVerify(row) {
  await ElMessageBox.confirm(`确认核销预定单 ${row.orderNo}？`, '核销确认', { type: 'warning' })
  await api.reservationVerify(row.id)
  ElMessage.success('核销成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.stat { background: #fff; border-radius: 8px; padding: 16px 18px; margin-bottom: 14px; border-top: 3px solid; }
.stat .lab { font-size: 13px; color: #8a9099; }
.stat .val { font-size: 26px; font-weight: 600; color: #1f2329; margin: 6px 0 4px; }
.stat .sub { font-size: 12px; color: #a4a9b0; }
</style>
