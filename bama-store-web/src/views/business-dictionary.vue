<template>
  <div class="page-card business-dictionary" v-loading="loading">
    <h3 class="page-title">业务字典 <span class="page-sub">全局设置，对所有分店生效</span></h3>
    <el-alert v-if="loadFailed" title="未能读取业务开关，请重试后再修改。" type="error" :closable="false" show-icon />
    <template v-else-if="loaded">
      <div class="switch-row">
        <div><h4>业务总开关</h4><p>统一控制预约、资金操作、注册、微信绑定、通知订阅和业务资料新增、编辑、状态变更和删除</p></div>
        <el-switch :model-value="enabled" :loading="saving" :disabled="saving || loading" active-text="开启" inactive-text="关闭" @change="save" />
      </div>
      <el-alert :title="enabled ? '业务已开放' : '系统正在维护'" :type="enabled ? 'success' : 'info'" :closable="false" show-icon>
        <template #default>{{ enabled ? '各端可按原有权限办理预约、充值、扣减、注册、微信绑定、通知订阅和业务资料新增、编辑、状态变更和删除。' : '可浏览门店、房间、会员资料和历史记录；新建、确认、改期预约，以及充值、扣减、新用户注册、自动建档、微信绑定、通知订阅和业务资料新增、编辑、状态变更和删除（会员、员工、分店、包间、商品、Banner、临时关闭时段）均已暂停。' }}</template>
      </el-alert>
      <p class="detail">关闭不会删除历史数据或改变已有余额。已有账号仍可登录、浏览和查询；资料修改、取消／拒绝预约、到店核销和完成预约均暂停。管理员仍可修改本开关。历史通知队列按原配置继续处理。开关修改会记录到操作日志。</p>
      <div class="dictionary-key">字典键：<code>business_enabled</code><span>开启 = 1 · 关闭 = 0</span></div>
    </template>
    <el-button :disabled="saving || loading" @click="load" class="refresh">刷新状态</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const enabled = ref(false)
const loaded = ref(false)
const loading = ref(false)
const saving = ref(false)
const loadFailed = ref(false)
async function load() {
  loading.value = true
  loadFailed.value = false
  try {
    const state = await api.businessDictionary()
    enabled.value = state.enabled === true
    loaded.value = true
  } catch { loadFailed.value = true }
  finally { loading.value = false }
}
async function save(next) {
  if (saving.value || loading.value || !loaded.value || loadFailed.value) return
  saving.value = true
  try {
    const state = await api.updateBusinessDictionary(next)
    enabled.value = state.enabled === true
    ElMessage.success(enabled.value ? '业务已开放' : '已切换为展示模式')
  } catch { await load() }
  finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.business-dictionary{max-width:800px}.switch-row{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:24px 0}.switch-row h4{margin:0 0 8px;font-size:17px;color:#46372d}.switch-row p{margin:0;font-size:13px;color:#8a7969}.detail{font-size:13px;line-height:1.8;color:#8a7969;margin:18px 0}.dictionary-key{display:flex;flex-wrap:wrap;gap:8px;font-size:12px;color:#958677;padding-top:18px;border-top:1px solid #eee6dd}.dictionary-key span{margin-left:auto}.refresh{margin-top:24px}
</style>
