<template>
  <div class="page-card">
    <h3 class="page-title">代客储值 <span class="page-sub">为会员充值并登记赠送金额，等同员工端「会员储值」</span></h3>

    <el-steps :active="step" simple style="margin-bottom:20px">
      <el-step title="选择会员" />
      <el-step title="填写金额" />
      <el-step title="完成" />
    </el-steps>

    <!-- 第一步：搜索并选择会员 -->
    <div v-if="step === 0">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="输入会员姓名 / 手机号 / 会员卡号搜索" clearable
                  style="width:340px" @keyup.enter="loadMembers">
          <template #append><el-button :icon="Search" @click="loadMembers" /></template>
        </el-input>
      </div>
      <el-table :data="members" v-loading="loading" @row-click="onPick" highlight-current-row
                empty-text="请输入关键词搜索会员">
        <el-table-column prop="memberNo" label="会员卡号" width="140" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="level" label="等级" width="110">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="discount" label="折扣" width="90">
          <template #default="{ row }">{{ Number(row.discount) / 10 }} 折</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click.stop="onPick(row)">选择</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > 0" style="margin-top:12px" layout="total, prev, pager, next"
                     :total="total" :page-size="pageSize" :current-page="pageNum"
                     @current-change="p => { pageNum = p; loadMembers() }" />
    </div>

    <!-- 第二步：填写充值金额 -->
    <div v-if="step === 1" class="form-wrap">
      <el-descriptions :column="2" border size="small" style="margin-bottom:18px">
        <el-descriptions-item label="会员">{{ current.name }}（{{ current.memberNo }}）</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ current.phone }}</el-descriptions-item>
        <el-descriptions-item label="等级">{{ levelText(current.level) }} · {{ Number(current.discount) / 10 }} 折</el-descriptions-item>
        <el-descriptions-item label="当前余额">
          <b class="mono" style="color:var(--bm-brand)">¥{{ account.balance ?? '-' }}</b>
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="form" label-width="110px" style="max-width:520px">
        <el-form-item label="充值金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" style="width:220px" />
          <span class="quick">
            <el-button v-for="a in [500, 1000, 2000, 5000]" :key="a" size="small" @click="form.amount = a">{{ a }}</el-button>
          </span>
        </el-form-item>
        <el-form-item label="赠送金额">
          <el-input-number v-model="form.giftAmount" :min="0" :precision="2" :step="50" style="width:220px" />
          <span class="tip">选填，活动赠送额度，与充值金额一并计入卡内余额</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="如：双十一充值活动" maxlength="50" show-word-limit style="width:360px" />
        </el-form-item>
        <el-form-item label="到账合计">
          <span class="total mono">¥{{ ((form.amount || 0) + (form.giftAmount || 0)).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item>
          <el-button @click="step = 0">上一步</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">确认充值</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 第三步：结果 -->
    <el-result v-if="step === 2" icon="success" title="充值成功"
               :sub-title="`${current.name} 本次到账 ¥${lastAmount}`">
      <template #extra>
        <el-button type="primary" @click="reset">继续为其他会员充值</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '@/api'

const step = ref(0)
const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const members = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const current = ref({})
const account = ref({})
const lastAmount = ref(0)
const form = reactive({ amount: 500, giftAmount: 0, remark: '' })

const levelText = l => ({ BLACK_GOLD: '黑金卡', GOLD: '金卡', NORMAL: '普通' }[l] || l || '普通')
const levelTag  = l => ({ BLACK_GOLD: 'danger', GOLD: 'warning', NORMAL: 'info' }[l] || 'info')

async function loadMembers() {
  loading.value = true
  try {
    const res = await api.memberPage({ pageNum: pageNum.value, pageSize, keyword: keyword.value })
    members.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

// 选中会员后拉取其账户余额
async function onPick(row) {
  current.value = row
  account.value = await api.memberAccount(row.id)
  step.value = 1
}

async function onSubmit() {
  if (!form.amount || form.amount <= 0) return ElMessage.warning('请输入充值金额')
  const totalAmt = (form.amount + (form.giftAmount || 0)).toFixed(2)
  await ElMessageBox.confirm(
    `为 ${current.value.name} 充值 ¥${form.amount}，赠送 ¥${form.giftAmount || 0}，共到账 ¥${totalAmt}？`,
    '充值确认', { type: 'warning' }
  )
  submitting.value = true
  try {
    await api.recharge({
      memberId: current.value.id,
      amount: form.amount,
      giftAmount: form.giftAmount || 0,
      remark: form.remark
    })
    lastAmount.value = totalAmt
    step.value = 2
  } finally {
    submitting.value = false
  }
}

function reset() {
  step.value = 0
  current.value = {}
  account.value = {}
  form.amount = 500; form.giftAmount = 0; form.remark = ''
  loadMembers()
}

const route = useRoute()
onMounted(async () => {
  await loadMembers()
  if (route.query.memberId) {
    try { await onPick(await api.memberDetail(route.query.memberId)) } catch {}
  }
})
</script>

<style scoped>
.form-wrap { max-width: 720px; }
.quick { margin-left: 12px; display: inline-flex; gap: 6px; }
.tip { margin-left: 12px; font-size: 12px; color: #a4a9b0; }
.total { font-size: 22px; font-weight: 600; color: var(--bm-brand); }
</style>
