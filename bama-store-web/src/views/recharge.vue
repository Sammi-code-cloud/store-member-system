<template>
  <div class="page-card recharge-page">
    <header class="recharge-header"><div><h3>代客充值</h3><p>为会员账户充值，赠送金额单独登记</p></div><span class="header-note">会员服务 / 储值</span></header>

    <ol class="recharge-steps" aria-label="充值进度"><li v-for="(label,index) in ['选择会员','填写金额','充值完成']" :key="label" :class="{active:step===index,complete:step>index}" :aria-current="step===index?'step':undefined"><span>{{step>index?'✓':index+1}}</span>{{label}}</li></ol>

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
        <el-table-column prop="balance" label="会员卡余额" width="140" align="right">
          <template #default="{ row }">
            <b class="mono" style="color:var(--bm-brand)">{{ formatBalance(row.balance) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="110">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
          </template>
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
    <div v-if="step === 1" class="recharge-layout">
      <section class="amount-panel">
        <div class="section-heading"><h4>填写充值信息</h4><span>金额单位：元</span></div>
        <el-form :model="form" label-position="top" :disabled="submitting">
          <el-form-item label="充值金额" class="principal-field">
            <div class="amount-control"><span class="currency">¥</span><el-input-number v-model="form.amount" aria-label="充值金额" :min="0.01" :precision="2" :step="100" :controls="false" /></div>
            <div class="quick"><el-button v-for="a in [500,1000,2000,5000]" :key="a" :class="{selected:form.amount===a}" :aria-pressed="form.amount===a" @click="form.amount=a">{{a.toLocaleString()}} 元</el-button></div>
          </el-form-item>
          <el-form-item label="赠送金额（选填）">
            <div class="amount-control gift-control"><span class="currency">¥</span><el-input-number v-model="form.giftAmount" aria-label="赠送金额" :min="0" :precision="2" :step="50" :controls="false" /></div>
            <p class="field-note">活动赠送金额，与充值本金一并计入会员余额。</p>
          </el-form-item>
          <el-form-item label="充值备注（选填）"><el-input v-model="form.remark" type="textarea" :rows="3" placeholder="如：到店充值、会员活动赠送" maxlength="50" show-word-limit resize="none" /></el-form-item>
        </el-form>
      </section>
      <aside class="review-panel">
        <div class="member-heading"><span>充值会员</span><el-button link type="primary" :disabled="submitting" @click="step=0">更换会员</el-button></div>
        <div class="member-identity"><div class="member-avatar">{{(current.name || '会').slice(0,1)}}</div><div><h4>{{current.name}}</h4><span class="member-level">{{levelText(current.level)}}会员</span></div></div>
        <dl class="member-details"><div><dt>手机号</dt><dd>{{current.phone || '未填写'}}</dd></div><div><dt>会员卡号</dt><dd>{{current.memberNo}}</dd></div><div><dt>当前余额</dt><dd class="mono">{{formatBalance(account.balance)}}</dd></div></dl>
        <div class="receipt"><h4>到账预览</h4><div class="receipt-line"><span>充值本金</span><b>{{formatBalance(form.amount || 0)}}</b></div><div class="receipt-line"><span>赠送金额</span><b>{{formatBalance(form.giftAmount || 0)}}</b></div><div class="receipt-total"><span>本次到账</span><strong>¥{{((form.amount || 0)+(form.giftAmount || 0)).toFixed(2)}}</strong></div><div class="receipt-line after-balance"><span>充值后余额</span><b>{{account.balance==null?'—':formatBalance(Number(account.balance)+(form.amount || 0)+(form.giftAmount || 0))}}</b></div></div>
        <p class="review-note">请核对会员和金额，确认后计入会员账户。</p>
        <el-button class="confirm-recharge" type="primary" :loading="submitting" @click="onSubmit">确认充值</el-button>
        <el-button class="back-button" :disabled="submitting" @click="step=0">返回选择会员</el-button>
      </aside>
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
const formatBalance = value => value == null || value === '' || !Number.isFinite(Number(value))
  ? '—'
  : `¥${Number(value).toFixed(2)}`

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
.recharge-page{width:100%;max-width:none;box-sizing:border-box;margin:0;padding:28px 32px!important}.recharge-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:28px}.recharge-header h3{margin:0;font-size:23px;font-weight:650;color:#302b27}.recharge-header p{margin:9px 0 0;font-size:13px;color:#8b827a}.header-note{font-size:12px;color:#9a8d81}.recharge-steps{display:flex;list-style:none;padding:18px 24px;margin:0 0 32px;background:#f8f6f2;border-radius:12px;gap:24px}.recharge-steps li{display:flex;align-items:center;gap:10px;flex:1;font-size:13px;color:#948b84}.recharge-steps li:not(:last-child)::after{content:'';height:1px;background:#e5dfd8;flex:1;margin-left:14px}.recharge-steps li span{display:flex;align-items:center;justify-content:center;width:26px;height:26px;flex-shrink:0;border-radius:50%;background:#ece7e0;color:#8e8278;font-size:12px}.recharge-steps li.active{color:var(--bm-brand);font-weight:600}.recharge-steps li.active span{background:var(--bm-brand);color:#fff}.recharge-steps li.complete span{background:#f1e3df;color:var(--bm-brand)}
.recharge-layout{display:grid;grid-template-columns:minmax(0,1fr) 350px;gap:40px;align-items:start}.amount-panel{padding:4px 0}.section-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:26px}.section-heading h4,.receipt h4{font-size:16px;margin:0;color:#413930}.section-heading span{font-size:12px;color:#9a8b7e}.amount-panel :deep(.el-form-item){margin-bottom:26px}.amount-panel :deep(.el-form-item__label){font-size:13px;font-weight:600;color:#51473d;margin-bottom:10px!important}.amount-control{width:100%;position:relative}.amount-control .currency{position:absolute;z-index:1;left:18px;top:0;line-height:62px;font-size:24px;color:#9d8c7b}.amount-control :deep(.el-input-number){width:100%}.amount-control :deep(.el-input__wrapper){height:62px;box-sizing:border-box;border-radius:10px;background:#fffdfa;padding-left:43px}.amount-control :deep(.el-input__inner){text-align:left;font-size:28px;font-weight:600;color:#493b30;font-variant-numeric:tabular-nums}.gift-control :deep(.el-input__wrapper){height:46px;background:#fff}.gift-control .currency{line-height:46px;font-size:18px}.gift-control :deep(.el-input__inner){font-size:19px;font-weight:500}.quick{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px;width:100%;margin-top:12px}.quick .el-button{margin:0!important;height:36px;padding:0 6px;border-radius:8px;color:#6c5a4a;background:#fff}.quick .selected{color:var(--bm-brand);border-color:var(--bm-brand);background:#fcf2ef}.field-note{margin:8px 0 0;font-size:12px;color:#998b7d;line-height:1.7}.amount-panel :deep(.el-textarea__inner){border-radius:10px;padding:12px 14px;font-family:inherit}.review-panel{padding:24px;border:1px solid #ece3da;border-radius:16px;background:#fcfaf6}.member-heading{display:flex;justify-content:space-between;align-items:center;font-size:12px;color:#9a8979}.member-identity{display:flex;gap:12px;align-items:center;margin:20px 0}.member-avatar{display:flex;align-items:center;justify-content:center;width:46px;height:46px;flex-shrink:0;background:#eee2d5;color:#8e6345;border-radius:12px;font-size:21px}.member-identity h4{font-size:18px;line-height:1.5;margin:0 0 3px;overflow-wrap:anywhere;color:#41372d}.member-level{font-size:11px;color:#9b7b5c}.member-details{margin:0 0 22px}.member-details>div{display:flex;align-items:baseline;justify-content:space-between;gap:12px;margin-top:12px;font-size:12px}.member-details dt{color:#9b8d80;flex-shrink:0}.member-details dd{margin:0;text-align:right;overflow-wrap:anywhere;color:#675645;font-variant-numeric:tabular-nums}.receipt{border-top:1px solid #e8dfd5;padding-top:22px}.receipt h4{font-size:14px;margin-bottom:18px}.receipt-line{display:flex;justify-content:space-between;gap:12px;font-size:13px;margin:13px 0;color:#8e7e6d}.receipt-line b{font-weight:500;color:#63513f;font-variant-numeric:tabular-nums}.receipt-total{display:flex;flex-wrap:wrap;align-items:baseline;justify-content:space-between;gap:8px;padding:18px 0 8px;border-top:1px dashed #dfd2c4;margin-top:18px;font-size:13px;color:#67523f}.receipt-total strong{font-size:30px;letter-spacing:-.5px;color:var(--bm-brand);font-variant-numeric:tabular-nums;overflow-wrap:anywhere}.after-balance{font-size:12px}.review-note{font-size:11px;color:#a18d7a;margin:22px 0 12px;line-height:1.8}.confirm-recharge{width:100%;height:44px;border-radius:9px;font-size:15px;font-weight:600}.back-button{width:100%;margin:10px 0 0!important;background:transparent;border:0;color:#9c8877;font-size:12px}.search-bar{margin-bottom:20px}.search-bar :deep(.el-input){max-width:100%}
@media(max-width:1000px){.recharge-layout{grid-template-columns:minmax(0,1fr) 310px;gap:24px}.recharge-page{padding:22px!important}.review-panel{padding:20px}}
@media(max-width:760px){.recharge-layout{grid-template-columns:1fr;gap:12px}.header-note{display:none}.recharge-steps{gap:12px;padding:16px 12px}.recharge-steps li{font-size:12px;gap:6px}.recharge-steps li:not(:last-child)::after{display:none}.recharge-page{padding:18px!important}.quick{gap:6px}}
</style>
