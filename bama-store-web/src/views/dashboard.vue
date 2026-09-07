<template>
  <div>
    <div class="welcome-banner"><div><div class="eyebrow">STORE WORKSPACE · {{localDate()}}</div><h1>{{user.name}}，欢迎回到门店</h1><p>一盏好茶，一天有序的经营。</p></div><div class="welcome-seal">茶</div></div>
    <div class="quick-actions"><el-button v-for="action in actions.filter(a=>user.has(a.perm))" :key="action.path" @click="router.push(action.path)"><el-icon><component :is="action.icon"/></el-icon>{{action.label}}</el-button><el-button :loading="loading" @click="load">刷新数据</el-button></div>
    <el-alert v-if="error" title="部分数据加载失败，请刷新重试。" type="error" :closable="false" show-icon class="section-gap"/>
    <el-alert v-if="user.has('reservation:view') && pendingTotal>0" type="warning" :closable="false" class="section-gap"><template #title>有 {{pendingTotal}} 条预约申请待确认 <el-button link type="primary" @click="router.push('/reservation?status=PENDING')">去处理 →</el-button></template></el-alert>
    <div class="metric-grid" v-loading="loading"><div class="metric" v-for="stat in stats" :key="stat.label"><span>{{stat.label}}</span><strong>{{stat.value}}</strong><small>{{stat.note}}</small></div></div>
    <div class="page-card" v-if="user.has('reservation:view')"><div class="section-heading"><h3 class="page-title">今日待到店</h3><el-button link type="primary" @click="router.push({path:'/reservation',query:{date:localDate()}})">全部预约 {{total}} 条 →</el-button></div>
      <el-table :data="todayList" v-loading="loading" empty-text="今日暂无待到店预约"><el-table-column label="房间 / 预约单" min-width="210"><template #default="{row}"><b>{{row.roomName}}</b><div class="small muted">{{row.orderNo}}</div></template></el-table-column><el-table-column label="顾客" min-width="150"><template #default="{row}">{{row.contactName||row.memberName}}<div class="small muted">{{row.contactPhone||'未留电话'}}</div></template></el-table-column><el-table-column label="时段" width="160"><template #default="{row}">{{row.startTime}}–{{endTime(row)}}</template></el-table-column><el-table-column label="应付金额" width="130"><template #default="{row}">¥{{money(row.amount)}}</template></el-table-column><el-table-column label="操作" width="110"><template #default="{row}"><el-button link type="primary" :disabled="!user.has('reservation:verify')" @click="verify(row)">到店核销</el-button></template></el-table-column></el-table>
      <p class="small muted" v-if="total>10">展示前 10 条待到店预约，点击“全部预约”查看完整列表。</p>
    </div>
    <div class="report-grid section-gap"><div class="page-card"><h3 class="page-title">经营提醒</h3><div class="reminder"><span class="reminder-mark">01</span><div><b>顾客自动进入后台</b><p>成功注册即建立档案，无需储值；可在顾客管理中查看最近登录和消费记录。</p></div></div><div class="reminder"><span class="reminder-mark">02</span><div><b>预约和付款分开核对</b><p>到店核销用于接待管理，收款结果请以资金流水为准。</p></div></div></div><div class="page-card"><h3 class="page-title">今日经营口径</h3><p class="muted">消费、充值与新增顾客（跨店）按北京时间统计；预约按到店日期统计。</p><p class="muted">会员账户余额是当前时点数据，不属于今日收入。查看经营统计可选择日期并核对逐日明细。</p><el-button v-if="user.has('dashboard:view')" type="primary" plain @click="router.push('/reports')">查看经营统计 →</el-button></div></div>
  </div>
</template>
<script setup>
import {ref,computed,onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import api from '@/api'
import {useUserStore} from '@/store/user'
import {money,localDate,endTime,confirmAction} from '@/utils/format'
const pendingTotal=ref(0)
const user=useUserStore(),router=useRouter(),loading=ref(false),error=ref(false),data=ref({}),todayList=ref([]),total=ref(0)
const actions=[{label:'添加员工',path:'/staff',perm:'staff:manage',icon:'Avatar'},{label:'管理房间',path:'/room',perm:'reservation:manage',icon:'House'},{label:'预约安排',path:'/reservation',perm:'reservation:view',icon:'Calendar'},{label:'查找顾客',path:'/member',perm:'member:view',icon:'User'},{label:'代客储值',path:'/recharge',perm:'account:recharge',icon:'Wallet'}]
const stats=computed(()=>[{label:'今日消费',value:data.value.consumeAmount==null?'—':'¥'+money(data.value.consumeAmount),note:(data.value.consumeCount??'—')+' 笔成功消费'},{label:'今日充值本金',value:data.value.rechargeAmount==null?'—':'¥'+money(data.value.rechargeAmount),note:'赠送 ¥'+money(data.value.giftAmount)},{label:'今日新增顾客（跨店）',value:data.value.newCustomers??'—',note:'无需消费，注册即建档'},{label:'今日预约',value:data.value.reservationCount??'—',note:'按到店日期统计，含取消'},{label:'顾客总数',value:data.value.customerCount??'—',note:'全部已登记顾客'},{label:'顾客余额（跨店共享）',value:data.value.totalBalance==null?'—':'¥'+money(data.value.totalBalance),note:'账户余额合计'}])
async function load(){if(user.has('reservation:view'))try{pendingTotal.value=(await api.reservationPage({status:'PENDING',pageSize:1})).total}catch{}loading.value=true;error.value=false;try{data.value=await api.reports({startDate:localDate(),endDate:localDate()})}catch{error.value=true;data.value={}}if(user.has('reservation:view'))try{const result=await api.reservationPage({date:localDate(),status:'WAITING',pageNum:1,pageSize:10});todayList.value=result.records;total.value=result.total}catch{error.value=true;todayList.value=[]}loading.value=false}
async function verify(row){if(!await confirmAction(ElMessageBox,'确认顾客已到店并开始使用房间？'))return;try{await api.reservationVerify(row.id);ElMessage.success('已核销到店');await load()}catch{}}
onMounted(load)
</script>
