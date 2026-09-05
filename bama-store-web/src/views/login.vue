<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <span class="mark">八马</span>
        <div>
          <div class="t">门店管理后台</div>
          <div class="s">会员储值 · 扫码收款 · 门店运营</div>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="onSubmit">登 录</el-button>
      </el-form>

      <div class="tip">仅限已授权的门店员工登录，账号由管理员在「员工与权限」中录入</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch (e) {
    // 错误提示已由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #7d1a22 0%, #3f6b4b 100%); }
.login-box { width: 380px; background: #fff; border-radius: 12px; padding: 34px 32px 26px;
  box-shadow: 0 12px 40px rgba(0,0,0,.18); }
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.brand .mark { background: var(--bm-brand); color: #fff; border-radius: 6px;
  padding: 8px 10px; font-size: 16px; font-weight: 700; }
.brand .t { font-size: 18px; font-weight: 600; color: #1f2329; }
.brand .s { font-size: 12px; color: #8a9099; margin-top: 3px; }
.btn { width: 100%; margin-top: 4px; }
.tip { font-size: 12px; color: #a4a9b0; margin-top: 18px; line-height: 1.6; text-align: center; }
</style>
