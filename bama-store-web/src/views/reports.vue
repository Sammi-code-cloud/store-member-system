<template>
  <div>
    <div class="page-heading"><div><div class="eyebrow">BUSINESS INSIGHTS</div><h2>经营统计</h2><p>看清门店经营的每一天</p></div><div class="muted small">统计时区：北京时间</div></div>
    <div class="page-card filter-bar"><el-radio-group v-model="preset" @change="setRange"><el-radio-button value="today">今日</el-radio-button><el-radio-button value="yesterday">昨日</el-radio-button><el-radio-button value="week">近7天</el-radio-button><el-radio-button value="month">近30天</el-radio-button></el-radio-group><el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" @change="preset='';load()"/><el-button type="primary" :loading="loading" @click="load">刷新报表</el-button><span class="muted small" v-if="updated">更新于 {{updated}}</span></div>
    <el-alert v-if="error" type="error" title="报表加载失败，请点击刷新重试。" :closable="false" show-icon class="section-gap"/>
    <div v-loading="loading">
      <div class="metric-grid"><div v-for="card in cards" :key="card.title" class="metric"><span>{{card.title}}</span><strong>{{card.value}}</strong><small>{{card.note}}</small></div></div>
      <div class="report-grid">
        <div class="page-card"><h3 class="page-title">消费与充值趋势</h3><div class="chart-legend"><span><i class="dot consume"/>消费金额</span><span><i class="dot recharge"/>充值本金</span></div>
          <div v-if="data.trend?.length" class="trend-chart"><svg viewBox="0 0 800 225" role="img" aria-label="每日消费和充值趋势，可在下方查看逐日明细"><g v-for="n in 4" :key="n"><line x1="55" :y1="20+(n-1)*50" x2="780" :y2="20+(n-1)*50" stroke="#eceee7"/><text x="48" :y="24+(n-1)*50" text-anchor="end" fill="#8b9184" font-size="11">{{Math.round(maxValue*(4-n)/3)}}</text></g><polyline :points="points('consume')" fill="none" stroke="#8c1f28" stroke-width="3"/><polyline :points="points('recharge')" fill="none" stroke="#9b8458" stroke-width="3"/><circle v-for="(d,i) in data.trend" :key="d.date" :cx="x(i)" :cy="y(d.consume)" r="3" fill="#8c1f28"><title>{{d.date}} 消费 ¥{{money(d.consume)}} / 充值 ¥{{money(d.recharge)}}</title></circle><text x="55" y="208" fill="#8b9184" font-size="12">{{range[0]}}</text><text x="780" y="208" text-anchor="end" fill="#8b9184" font-size="12">{{range[1]}}</text></svg></div><el-empty v-else description="暂无趋势数据"/>
        </div>
        <div class="page-card"><h3 class="page-title">预约状态</h3><p class="muted small">按预约到店日期统计</p><div v-for="(label,key) in reservationStates" :key="key" class="status-stat"><div><span>{{label}}</span><b>{{data.statuses?.[key] ?? '—'}}</b></div><el-progress :percentage="data.reservationCount?Math.round((data.statuses[key]||0)/data.reservationCount*100):0" :show-text="false" :stroke-width="8" :color="key==='CANCELLED'?'#d8dad1':key==='USING'?'#617654':'#b69a68'"/></div></div>
      </div>
      <div class="page-card section-gap"><h3 class="page-title">房间预订排行</h3><p class="muted small">预订率按当前营业时间估算，扣除临时关闭时段；历史营业规则调整可能影响分母。预约金额为应付金额，未计入实收消费。</p><el-table :data="data.rooms || []" empty-text="暂无房间数据"><el-table-column type="index" label="排名" width="70"/><el-table-column prop="name" label="房间" min-width="150"/><el-table-column prop="count" label="有效预约"/><el-table-column prop="hours" label="预订小时"/><el-table-column prop="availableHours" label="可预订小时（估算）" min-width="160"/><el-table-column label="预订率" min-width="120"><template #default="{row}">{{row.bookingRate==null?'—':row.bookingRate+'%'}}</template></el-table-column><el-table-column label="预约应付金额" min-width="150"><template #default="{row}">¥{{money(row.bookedAmount)}}</template></el-table-column></el-table></div>
      <div class="report-grid section-gap"><div class="page-card"><h3 class="page-title">员工业务量</h3><el-table :data="data.staff || []" empty-text="暂无员工数据"><el-table-column prop="name" label="员工"/><el-table-column prop="consumeCount" label="收款笔数"/><el-table-column label="收款金额"><template #default="{row}">¥{{money(row.consumeAmount)}}</template></el-table-column><el-table-column prop="verified" label="到店核销"/></el-table><p class="muted small">核销按预约日期归属，不代表员工提成。</p></div><div class="page-card"><h3 class="page-title">统计口径</h3><ul class="definition-list"><li>消费仅统计成功支付的消费订单。</li><li>充值本金与储值赠送分别统计，不和消费相加作为收入。</li><li>新增顾客（跨店）按首次建档时间去重；顾客和余额为跨分店共享档案。</li><li>预约按到店日期统计，取消订单不计入有效预订。</li><li>当前未接入退款执行；本页不提供净收入或实际使用率指标。</li></ul></div></div>
      <div class="page-card section-gap"><el-collapse><el-collapse-item title="逐日明细 · 消费、充值和新增顾客（跨店）" name="daily"><el-table :data="data.trend || []" max-height="420"><el-table-column prop="date" label="日期"/><el-table-column label="消费金额"><template #default="{row}">¥{{money(row.consume)}}</template></el-table-column><el-table-column label="充值本金"><template #default="{row}">¥{{money(row.recharge)}}</template></el-table-column><el-table-column prop="customers" label="新增顾客（跨店）"/></el-table></el-collapse-item></el-collapse></div>
    </div>
  </div>
</template>
<script setup>
import {ref,computed,onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import api from '@/api'
import {money,localDate,reservationStates} from '@/utils/format'
const data=ref({}),range=ref([localDate(-6),localDate()]),preset=ref('week'),loading=ref(false),error=ref(false),updated=ref('')
const cards=computed(()=>[
 {title:'消费金额',value:data.value.consumeAmount==null?'—':'¥'+money(data.value.consumeAmount),note:(data.value.consumeCount??'—')+' 笔成功消费'},
 {title:'充值本金',value:data.value.rechargeAmount==null?'—':'¥'+money(data.value.rechargeAmount),note:'赠送 ¥'+money(data.value.giftAmount)},
 {title:'新增顾客（跨店）',value:data.value.newCustomers??'—',note:'注册或首次登录建档'},
 {title:'消费顾客',value:data.value.consumeCustomers??'—',note:'区间内消费人数，去重'},
 {title:'预约订单',value:data.value.reservationCount??'—',note:'按到店日期统计，含取消'},
 {title:'顾客余额（跨店共享）',value:data.value.totalBalance==null?'—':'¥'+money(data.value.totalBalance),note:'时点余额，非区间内收入'}])
const maxValue=computed(()=>Math.max(1,...(data.value.trend||[]).flatMap(d=>[Number(d.consume),Number(d.recharge)])))
const x=i=>data.value.trend?.length===1?417:55+i/Math.max(1,(data.value.trend?.length||1)-1)*725
const y=v=>170-Number(v)/maxValue.value*150
const points=key=>(data.value.trend||[]).map((d,i)=>x(i)+','+y(d[key])).join(' ')
function setRange(){const days={today:[0,0],yesterday:[-1,-1],week:[-6,0],month:[-29,0]}[preset.value];range.value=days.map(localDate);load()}
let requestId=0
async function load(){if(!range.value?.[0]||!range.value?.[1])return ElMessage.warning('请选择日期范围');const id=++requestId;loading.value=true;error.value=false;try{const result=await api.reports({startDate:range.value[0],endDate:range.value[1]});if(id===requestId){data.value=result;updated.value=new Date().toLocaleTimeString('zh-CN')}}catch{if(id===requestId){error.value=true;data.value={};updated.value=''}}finally{if(id===requestId)loading.value=false}}
onMounted(load)
</script>
<style scoped>.trend-chart svg{width:100%;height:auto;margin-top:16px}.chart-legend{display:flex;gap:20px;font-size:12px;color:#7d8378}.chart-legend span{display:flex;align-items:center;gap:6px}.dot{width:8px;height:8px;border-radius:50%;display:inline-block}.dot.consume{background:#8c1f28}.dot.recharge{background:#9b8458}.status-stat{margin:22px 0}.status-stat>div:first-child{display:flex;justify-content:space-between;margin-bottom:9px;font-size:13px}.definition-list{padding-left:20px;color:#7d8378;font-size:13px;line-height:2.2}</style>
