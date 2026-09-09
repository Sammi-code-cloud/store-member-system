<template>
  <div>
    <div class="page-heading"><div><div class="eyebrow">CUSTOMERS</div><h2>顾客管理</h2><p>每一次相遇，都从一份顾客档案开始</p></div><el-tag effect="plain">已登记 {{ total }} 位顾客</el-tag></div>
    <div class="page-card">
      <el-alert title="顾客档案与会员余额跨分店共享；注册自动建档。详情中的预约和资金流水按当前分店展示。" type="info" :closable="false" show-icon />
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="姓名 / 账号 / 手机号 / 顾客编号" clearable style="width:280px" @keyup.enter="search" @clear="search" />
        <el-select v-model="level" placeholder="全部等级" clearable style="width:140px" @change="search"><el-option v-for="(text, key) in levels" :key="key" :label="text" :value="key" /></el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width:130px" @change="search"><el-option label="正常" :value="1"/><el-option label="停用" :value="0"/></el-select>
        <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="注册开始日期" end-placeholder="结束日期" @change="search" />
        <el-button type="primary" @click="search">查询</el-button><el-button @click="load">刷新</el-button><el-button v-if="user.has('member:manage')" type="primary" @click="newMember">新增会员</el-button>
      </div>
      <el-table :data="list" v-loading="loading" empty-text="暂无符合条件的顾客">
        <el-table-column label="顾客" min-width="160"><template #default="{row}"><b>{{ row.name }}</b><div class="muted small">{{ row.memberNo }}</div></template></el-table-column>
        <el-table-column prop="username" label="登录账号" min-width="130"><template #default="{row}">{{ row.username || '历史档案' }}</template></el-table-column>
        <el-table-column label="手机号" width="135"><template #default="{row}">{{ row.phone || '未绑定' }}</template></el-table-column>
        <el-table-column label="等级" width="110"><template #default="{row}"><el-tag size="small" :type="row.level === 'NORMAL' ? 'info' : 'warning'">{{ levels[row.level] || row.level }}</el-tag></template></el-table-column>
        <el-table-column label="余额 / 累计消费" width="160"><template #default="{row}"><span class="amount">¥{{ money(row.balance) }}</span><div class="muted small">已消费 ¥{{ money(row.totalConsume) }}</div></template></el-table-column>
        <el-table-column label="注册时间" width="175"><template #default="{row}">{{ dateText(row.createTime) }}</template></el-table-column>
        <el-table-column label="最近登录" width="175"><template #default="{row}">{{ dateText(row.lastLoginTime) }}</template></el-table-column>
        <el-table-column label="状态" width="85"><template #default="{row}"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '正常' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right"><template #default="{row}"><el-button link type="primary" @click="showDetail(row)">档案</el-button><el-button v-if="user.has('member:manage')" link type="primary" @click="edit(row)">编辑</el-button><el-button v-if="user.has('member:manage') && row.status===1" link type="primary" @click="showCode(row)">绑定微信</el-button></template></el-table-column>
      </el-table>
      <el-pagination class="pagination" layout="total, prev, pager, next" :total="total" :page-size="20" v-model:current-page="page" @current-change="load" />
    </div>
    <el-drawer v-model="drawer" title="顾客档案" size="min(880px, 95vw)">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="顾客">{{ current.name }}</el-descriptions-item><el-descriptions-item label="编号">{{ current.memberNo }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ current.phone || '未绑定' }}</el-descriptions-item><el-descriptions-item label="等级">{{ levels[current.level] }} · {{ Number(current.discount || 100) / 10 }} 折</el-descriptions-item>
          <el-descriptions-item label="卡内余额">¥{{ money(account.balance) }}</el-descriptions-item><el-descriptions-item label="累计充值本金">¥{{ money(account.totalRecharge) }}</el-descriptions-item>
          <el-descriptions-item label="累计消费">¥{{ money(account.totalConsume) }}</el-descriptions-item>
          <el-descriptions-item label="门店备注" :span="2">{{ current.remark || '暂无备注' }}</el-descriptions-item>
        </el-descriptions>
        <el-button v-if="user.has('account:recharge')" type="primary" class="section-gap" @click="router.push({ path: '/recharge', query: { memberId: current.id } })">为该顾客充值</el-button>
        <el-tabs v-model="tab" class="section-gap">
          <el-tab-pane label="资金记录" name="transactions">
            <el-table :data="txns.records || []" empty-text="暂无资金记录"><el-table-column label="时间" min-width="160"><template #default="{row}">{{ dateText(row.createTime) }}</template></el-table-column><el-table-column label="类型" width="100"><template #default="{row}">{{ txnTypes[row.type] || row.type }}</template></el-table-column><el-table-column label="金额" width="100"><template #default="{row}">¥{{ money(row.amount) }}</template></el-table-column><el-table-column prop="bizNo" label="流水号" min-width="160"/></el-table>
            <el-pagination class="pagination" layout="total, prev, pager, next" :total="txns.total || 0" v-model:current-page="txnPage" :page-size="10" @current-change="loadTxns" />
          </el-tab-pane>
          <el-tab-pane label="预约记录" name="reservations">
            <el-table :data="bookings.records || []" empty-text="暂无预约记录"><el-table-column prop="roomName" label="房间" min-width="120"/><el-table-column label="预约时间" min-width="220"><template #default="{row}">{{ row.reserveDate }} {{ row.startTime }}–{{ endTime(row) }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{row}">{{ reservationStates[row.status] }}</template></el-table-column><el-table-column label="金额"><template #default="{row}">¥{{ money(row.amount) }}</template></el-table-column></el-table>
            <el-pagination class="pagination" layout="total, prev, pager, next" :total="bookings.total || 0" v-model:current-page="bookingPage" :page-size="10" @current-change="loadBookings" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
    <el-dialog v-model="creating" title="新增会员" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px"><el-form-item label="会员姓名" required><el-input v-model="newForm.name" maxlength="32" :disabled="creatingBusy"/></el-form-item><el-form-item label="手机号" required><el-input v-model="newForm.phone" maxlength="11" :disabled="creatingBusy"/></el-form-item><el-form-item label="备注"><el-input v-model="newForm.remark" type="textarea" maxlength="500" :disabled="creatingBusy"/></el-form-item></el-form>
      <p class="muted small">新会员余额为0，创建后可生成微信绑定码，充值请使用储值功能。</p>
      <template #footer><el-button :disabled="creatingBusy" @click="creating=false">取消</el-button><el-button type="primary" :loading="creatingBusy" @click="createMember">创建并生成绑定码</el-button></template>
    </el-dialog>
    <el-dialog v-model="codeDialog" title="会员微信绑定" width="460px" @closed="codeRequest++;bindingCode={};codeLoading=false">
      <div v-loading="codeLoading" style="text-align:center;min-height:160px">
        <p>{{codeMember.name}} · {{codeMember.phone}}</p>
        <el-alert v-if="bindingCode.bound" title="该会员已绑定微信，可直接微信登录。" type="success" :closable="false"/>
        <template v-else-if="bindingCode.image"><img :src="bindingCode.image" alt="会员微信绑定二维码" style="width:280px;max-width:100%"/><p>请会员使用本人微信扫码，并填写登记的手机号。</p><p class="muted small">有效期10分钟，仅限本人使用；重新生成后旧码失效。</p></template>
        <p v-else-if="!codeLoading" class="muted">{{codeError || '暂未生成绑定码'}}</p>
      </div>
      <template #footer><el-button @click="codeDialog=false">关闭</el-button><el-button v-if="!bindingCode.bound" type="primary" :loading="codeLoading" @click="showCode(codeMember)">重新生成</el-button></template>
    </el-dialog>
    <el-dialog v-model="editing" title="编辑顾客资料" width="480px">
      <el-form label-width="80px"><el-form-item label="姓名" required><el-input v-model="form.name" maxlength="32"/></el-form-item><el-form-item label="门店备注"><el-input type="textarea" v-model="form.remark" maxlength="500" show-word-limit :rows="4"/></el-form-item><el-form-item label="账号状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item></el-form>
      <p class="muted small">停用后顾客无法登录或预订，余额和历史记录保留。</p>
      <template #footer><el-button @click="editing=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { useUserStore } from '@/store/user'
import { money, dateText, levels, txnTypes, reservationStates, endTime } from '@/utils/format'
const user = useUserStore(), router = useRouter()
const keyword=ref(''), level=ref(''), status=ref(''), dates=ref([]), page=ref(1), total=ref(0), list=ref([]), loading=ref(false)
const drawer=ref(false), detailLoading=ref(false), current=ref({}), account=ref({}), txns=ref({}), bookings=ref({}), txnPage=ref(1), bookingPage=ref(1), tab=ref('transactions')
const editing=ref(false), saving=ref(false), form=reactive({})
const creating=ref(false),creatingBusy=ref(false),newForm=reactive({name:'',phone:'',remark:''})
const codeDialog=ref(false),codeLoading=ref(false),codeMember=ref({}),bindingCode=ref({}),codeError=ref('')
let codeRequest=0
function newMember(){Object.assign(newForm,{name:'',phone:'',remark:''});creating.value=true}
async function createMember(){
  if(creatingBusy.value)return
  const phone=String(newForm.phone||'').trim(),name=newForm.name.trim()
  if(!name || !/^1[3-9]\d{9}$/.test(phone))return ElMessage.warning('请填写姓名及正确的11位手机号')
  creatingBusy.value=true
  try{const member=await api.memberCreate({...newForm,name,phone});creating.value=false;ElMessage.success('会员已创建');await load();await showCode(member)}catch{}finally{creatingBusy.value=false}
}
async function showCode(member){
  if(codeLoading.value)return
  const id=++codeRequest;codeMember.value=member;bindingCode.value={};codeError.value='';codeDialog.value=true;codeLoading.value=true
  try{const result=await api.memberWechatCode(member.id);if(id===codeRequest)bindingCode.value=result}catch(e){if(id===codeRequest)codeError.value=e.message||'生成失败，请重试'}finally{if(id===codeRequest)codeLoading.value=false}
}
let requestId=0
async function load() {
  const id=++requestId; loading.value=true
  try { const result=await api.memberPage({pageNum:page.value,pageSize:20,keyword:keyword.value,level:level.value,status:status.value === '' ? undefined : status.value,startDate:dates.value?.[0],endDate:dates.value?.[1]}); if(id===requestId){list.value=result.records;total.value=result.total} } catch { if(id===requestId) list.value=[] } finally { if(id===requestId) loading.value=false }
}
function search(){page.value=1;load()}
async function loadTxns(){txns.value=await api.memberTransactions(current.value.id,{pageNum:txnPage.value,pageSize:10})}
async function loadBookings(){bookings.value=await api.memberReservations(current.value.id,{pageNum:bookingPage.value,pageSize:10})}
async function showDetail(row){current.value=row;account.value={};txns.value={};bookings.value={};txnPage.value=1;bookingPage.value=1;tab.value='transactions';drawer.value=true;detailLoading.value=true;try{const result=await Promise.all([api.memberAccount(row.id),loadTxns(),loadBookings()]);account.value=result[0]}catch{}finally{detailLoading.value=false}}
function edit(row){Object.assign(form,{id:row.id,name:row.name,remark:row.remark||'',status:row.status});editing.value=true}
async function save(){if(!form.name?.trim())return ElMessage.warning('请填写姓名');saving.value=true;try{await api.memberUpdate(form.id,form);editing.value=false;ElMessage.success('顾客资料已更新');await load()}catch{}finally{saving.value=false}}
onMounted(load)
</script>
