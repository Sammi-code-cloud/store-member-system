<template>
  <div>
    <div class="page-heading"><div><div class="eyebrow">TEA ROOMS</div><h2>可预订房间</h2><p>维护空间、价格与开放时间，让每一场茶叙有处安放</p></div><el-button type="primary" :disabled="!canManage" @click="edit()"><el-icon><Plus/></el-icon>新增房间</el-button></div>
    <div class="metric-grid compact"><div class="metric"><span>全部房间</span><strong>{{ rooms.length }}</strong></div><div class="metric"><span>开放预订</span><strong>{{ rooms.filter(r=>r.status===1).length }}</strong></div><div class="metric"><span>暂停预订</span><strong>{{ rooms.filter(r=>r.status!==1).length }}</strong></div></div>
    <div class="page-card">
      <div class="filter-bar"><el-input v-model="keyword" placeholder="搜索房间名称 / 类型" clearable style="width:260px"/><el-radio-group v-model="status"><el-radio-button value="all">全部</el-radio-button><el-radio-button :value="1">开放预订</el-radio-button><el-radio-button :value="0">已停用</el-radio-button></el-radio-group><el-button @click="load">刷新</el-button></div>
      <el-table :data="filtered" v-loading="loading" empty-text="暂无符合条件的房间">
        <el-table-column label="房间" min-width="230"><template #default="{row}"><div class="room-cell"><el-image v-if="row.image" :src="row.image" fit="cover" class="room-thumb"><template #error><div class="room-placeholder">茶</div></template></el-image><div v-else class="room-placeholder">茶</div><div><b>{{row.name}}</b><div class="muted small">{{row.roomType}} · {{row.capacity || '人数未设置'}}</div></div></div></template></el-table-column>
        <el-table-column label="价格" width="150"><template #default="{row}"><span class="amount">¥{{money(row.priceHour)}} / 时</span><div class="small muted">{{row.minHours}} 小时起订</div></template></el-table-column>
        <el-table-column label="开放时间" width="145"><template #default="{row}">{{row.openTime}}–{{row.closeTime}}<div class="small muted">可提前 {{row.advanceDays}} 天</div></template></el-table-column>
        <el-table-column prop="facilities" label="设施" min-width="170" show-overflow-tooltip/>
        <el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.status===1?'success':'info'">{{row.status===1?'开放预订':'已停用'}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="250" fixed="right"><template #default="{row}"><el-button link type="primary" :disabled="!canManage" @click="edit(row)">编辑</el-button><el-button link type="primary" :disabled="!canManage" @click="showClosures(row)">关闭时段</el-button><el-button link @click="router.push({path:'/reservation',query:{roomId:row.id}})">预约</el-button><el-button link type="danger" :disabled="!canManage" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
      <p class="small muted">开放预订不代表所有时段空闲；预约占用和临时关闭会实时影响顾客可选时段。</p>
    </div>
    <el-dialog v-model="dialog" :title="form.id?'编辑房间':'新增房间'" width="650px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16"><el-col :span="14"><el-form-item label="房间名称" required><el-input v-model="form.name" maxlength="64"/></el-form-item></el-col><el-col :span="10"><el-form-item label="类型"><el-select v-model="form.roomType"><el-option v-for="type in ['包厢','卡座','大厅']" :key="type" :value="type" :label="type"/></el-select></el-form-item></el-col></el-row>
        <el-form-item label="容纳人数"><el-input v-model="form.capacity" placeholder="如 4–6 人" maxlength="32"/></el-form-item>
        <el-form-item label="图片地址"><el-input v-model="form.image" placeholder="https://…" maxlength="255"/></el-form-item>
        <el-form-item label="房间介绍"><el-input type="textarea" v-model="form.description" :rows="2" maxlength="1000"/></el-form-item>
        <el-form-item label="设施"><el-input v-model="form.facilities" placeholder="如 茶具、空调、独立洗手间" maxlength="255"/></el-form-item>
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="每小时价格" required><el-input-number v-model="form.priceHour" :min="0" :max="99999999" :precision="2" :step="10"/></el-form-item></el-col><el-col :span="12"><el-form-item label="起订小时"><el-input-number v-model="form.minHours" :min="0.5" :max="12" :step="0.5" :precision="1"/></el-form-item></el-col></el-row>
        <el-form-item label="开放时间" required><el-time-select v-model="form.openTime" start="00:00" end="23:30" step="00:30" style="width:150px"/><span style="margin:0 12px">至</span><el-time-select v-model="form.closeTime" start="00:30" end="23:30" step="00:30" style="width:150px"/></el-form-item>
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="提前预订天数"><el-input-number v-model="form.advanceDays" :min="0" :max="365"/></el-form-item></el-col><el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :max="9999"/></el-form-item></el-col></el-row>
        <el-form-item label="开放预订"><el-switch v-model="form.status" :active-value="1" :inactive-value="0"/><span class="small muted" style="margin-left:12px">停用保留已有预约；调价仅影响新订单</span></el-form-item>
      </el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存房间</el-button></template>
    </el-dialog>
    <el-drawer v-model="closureDrawer" :title="'临时关闭 · '+current.name" size="min(650px,95vw)">
      <el-alert title="关闭时段将禁止新预约；已有预约的时段需先处理预约。" type="info" :closable="false"/>
      <el-form label-width="90px" class="section-gap"><el-form-item label="日期" required><el-date-picker v-model="closure.closureDate" value-format="YYYY-MM-DD" :disabled-date="d=>d < new Date(localDate()+'T00:00:00')"/></el-form-item><el-form-item label="起止时间" required><el-time-select v-model="closure.startTime" start="00:00" end="23:30" step="00:30" style="width:150px"/><span style="margin:0 8px">至</span><el-time-select v-model="closure.endTime" start="00:30" end="23:30" step="00:30" style="width:150px"/></el-form-item><el-form-item label="原因" required><el-input v-model="closure.reason" placeholder="如 设备维护、临时包场" maxlength="255"/></el-form-item><el-form-item><el-button type="primary" :loading="saving" @click="closeSlot">添加关闭时段</el-button></el-form-item></el-form>
      <el-table :data="closures" empty-text="暂无待生效的关闭时段"><el-table-column prop="closureDate" label="日期" width="120"/><el-table-column label="时段" width="125"><template #default="{row}">{{row.startTime}}–{{row.endTime}}</template></el-table-column><el-table-column prop="reason" label="原因"/><el-table-column label="操作" width="80"><template #default="{row}"><el-button link type="primary" @click="reopen(row)">恢复</el-button></template></el-table-column></el-table>
    </el-drawer>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useUserStore } from '@/store/user'
import { money, localDate, confirmAction } from '@/utils/format'
const user=useUserStore(),router=useRouter(),canManage=computed(()=>user.has('reservation:manage'))
const rooms=ref([]),keyword=ref(''),status=ref('all'),loading=ref(false),saving=ref(false),dialog=ref(false),closureDrawer=ref(false),current=ref({}),closures=ref([])
const defaults={id:null,name:'',roomType:'包厢',capacity:'',image:'',description:'',facilities:'',priceHour:100,minHours:1,openTime:'10:00',closeTime:'22:00',advanceDays:30,sortOrder:0,status:1}
const form=reactive({...defaults}),closure=reactive({closureDate:localDate(),startTime:'10:00',endTime:'22:00',reason:''})
const filtered=computed(()=>rooms.value.filter(r=>(status.value==='all'||r.status===status.value)&&(!keyword.value||(r.name+' '+r.roomType).includes(keyword.value))))
async function load(){loading.value=true;try{rooms.value=await api.roomList()}catch{}finally{loading.value=false}}
function edit(row){Object.assign(form,defaults,row||{});dialog.value=true}
async function save(){if(!form.name.trim())return ElMessage.warning('请填写房间名称');saving.value=true;try{await api.roomSave(form);dialog.value=false;ElMessage.success('房间已保存');await load()}catch{}finally{saving.value=false}}
async function remove(row){if(!await confirmAction(ElMessageBox,'删除「'+row.name+'」？有关联预约的房间请改为停用。'))return;try{await api.roomDelete(row.id);ElMessage.success('已删除');await load()}catch{}}
async function showClosures(row){current.value=row;closures.value=[];Object.assign(closure,{closureDate:localDate(),startTime:row.openTime,endTime:row.closeTime,reason:''});closureDrawer.value=true;try{closures.value=await api.roomClosures(row.id)}catch{}}
async function closeSlot(){saving.value=true;try{await api.roomClose(current.value.id,closure);ElMessage.success('关闭时段已添加');closures.value=await api.roomClosures(current.value.id);closure.reason=''}catch{}finally{saving.value=false}}
async function reopen(row){if(!await confirmAction(ElMessageBox,'恢复该时段的房间预订？'))return;try{await api.roomReopen(current.value.id,row.id);closures.value=await api.roomClosures(current.value.id);ElMessage.success('该时段已恢复')}catch{}}
onMounted(load)
</script>
<style scoped>.room-cell{display:flex;align-items:center;gap:12px}.room-thumb,.room-placeholder{width:54px;height:54px;border-radius:8px;flex-shrink:0}.room-placeholder{display:grid;place-items:center;background:#f1eee4;color:#8d7046;font-family:serif;font-size:24px}</style>
