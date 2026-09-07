<template>
  <div>
    <div class="page-heading"><div><div class="eyebrow">{{audit?'ACTIVITY LOG':'TRANSACTIONS'}}</div><h2>{{audit?'操作记录':'资金流水'}}</h2><p>{{audit?'每一次操作都有迹可循':'查看每一笔充值、赠送与消费，核对账户余额变化'}}</p></div></div>
    <div class="page-card"><div class="filter-bar"><el-input v-model="keyword" :placeholder="audit?'员工 / 操作 / 业务单号':'顾客 / 手机号 / 流水号 / 订单号'" clearable style="width:300px" @keyup.enter="search" @clear="search"/><el-select v-if="!audit" v-model="type" placeholder="全部类型" clearable style="width:145px" @change="search"><el-option v-for="(label,key) in txnTypes" :key="key" :label="label" :value="key"/></el-select><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" @change="search"/><el-button type="primary" @click="search">查询</el-button><el-button @click="load">刷新</el-button></div>
    <el-table :data="list" v-loading="loading" :empty-text="audit?'暂无操作记录':'暂无资金流水'">
      <el-table-column label="时间" width="180"><template #default="{row}">{{dateText(row.createTime)}}</template></el-table-column>
      <template v-if="audit"><el-table-column prop="actor" label="操作者" width="115"/><el-table-column prop="action" label="操作" width="145"/><el-table-column prop="target" label="业务对象 / 单号" min-width="210"/><el-table-column prop="detail" label="变更内容" min-width="350"/></template>
      <template v-else><el-table-column prop="bizNo" label="流水号" min-width="220"/><el-table-column prop="memberName" label="顾客" width="110"/><el-table-column label="类型" width="115"><template #default="{row}"><el-tag :type="row.type==='CONSUME'?'warning':'success'">{{txnTypes[row.type]||row.type}}</el-tag></template></el-table-column><el-table-column label="金额" width="120"><template #default="{row}"><b :class="row.type==='CONSUME'?'':'positive'">{{row.type==='CONSUME'?'-':'+'}}{{money(row.amount)}}</b></template></el-table-column><el-table-column label="变动前余额" width="125"><template #default="{row}">¥{{money(row.balanceBefore)}}</template></el-table-column><el-table-column label="变动后余额" width="125"><template #default="{row}">¥{{money(row.balanceAfter)}}</template></el-table-column><el-table-column prop="staffName" label="操作员工" width="110"/><el-table-column prop="refOrderNo" label="关联订单" min-width="210"/><el-table-column prop="remark" label="备注" min-width="180"/></template>
    </el-table><el-pagination class="pagination" layout="total, prev, pager, next" :total="total" :page-size="20" v-model:current-page="page" @current-change="load"/>
    <p class="muted small">{{audit?'记录成功的员工、顾客、房间、预约、充值、扣款和门店设置操作；历史版本未记录的操作不会补造。':'流水保留原始金额和余额变动；退款执行尚未接入。'}}</p>
    </div>
  </div>
</template>
<script setup>
import {ref,computed,watch,onMounted} from 'vue'
import {useRoute} from 'vue-router'
import api from '@/api'
import {money,dateText,txnTypes} from '@/utils/format'
const route=useRoute(),audit=computed(()=>route.path==='/audit')
const list=ref([]),keyword=ref(''),type=ref(''),dates=ref([]),page=ref(1),total=ref(0),loading=ref(false)
let requestId=0
async function load(){const id=++requestId;loading.value=true;try{const params={pageNum:page.value,pageSize:20,keyword:keyword.value,type:type.value||undefined,startDate:dates.value?.[0],endDate:dates.value?.[1]};const result=await(audit.value?api.auditLogs(params):api.transactions(params));if(id===requestId){list.value=result.records;total.value=result.total}}catch{if(id===requestId)list.value=[]}finally{if(id===requestId)loading.value=false}}
function search(){page.value=1;load()}
watch(()=>route.path,()=>{keyword.value='';type.value='';dates.value=[];list.value=[];total.value=0;search()})
onMounted(load)
</script>
