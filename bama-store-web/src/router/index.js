import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import api from '@/api'

/**
 * 路由表
 * meta.title      侧边栏菜单名
 * meta.icon       Element Plus 图标组件名
 * meta.perm       所需权限码，与后端 t_permission.code 一致；为空表示登录即可访问
 * meta.group      菜单分组
 */
const routes = [
  { path: '/login', component: () => import('@/views/login.vue'), meta: { hidden: true } },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard.vue'),
        meta: { title: '工作台', icon: 'DataLine', perm: 'dashboard:view', group: '门店运营' } },
      { path: 'reports', component: () => import('@/views/reports.vue'), meta: { title: '经营统计', icon: 'TrendCharts', perm: 'dashboard:view', group: '门店运营' } },
      { path: 'transactions', component: () => import('@/views/records.vue'), meta: { title: '资金流水', icon: 'Tickets', perm: 'account:view', group: '门店运营' } },
      { path: 'audit', component: () => import('@/views/records.vue'), meta: { title: '操作记录', icon: 'Document', perm: 'audit:view', group: '系统设置' } },
      { path: 'recharge', component: () => import('@/views/recharge.vue'),
        meta: { title: '代客储值', icon: 'Wallet', perm: 'account:recharge', group: '门店运营' } },
      { path: 'reservation', component: () => import('@/views/reservation.vue'),
        meta: { title: '预约管理', icon: 'Calendar', perm: 'reservation:view', group: '门店运营' } },

      { path: 'member', component: () => import('@/views/member.vue'),
        meta: { title: '顾客管理', icon: 'User', perm: 'member:view', group: '资料管理' } },
      { path: 'product', component: () => import('@/views/product.vue'),
        meta: { title: '货品管理', icon: 'Goods', perm: 'product:view', group: '资料管理' } },
      { path: 'banners', component: () => import('@/views/banners.vue'), meta: { title: '首页 Banner', icon: 'Picture', perm: 'store:manage', group: '资料管理' } },
      { path: 'room', component: () => import('@/views/room.vue'),
        meta: { title: '房间管理', icon: 'House', perm: 'reservation:manage', group: '资料管理' } },

      { path: 'staff', component: () => import('@/views/staff.vue'),
        meta: { title: '员工与权限', icon: 'Avatar', perm: 'staff:view', group: '系统设置' } },
      { path: 'branches', component: () => import('@/views/branches.vue'), meta: { title: '分店管理', icon: 'Shop', perm: 'store:all', group: '系统设置' } },
      { path: 'store', component: () => import('@/views/store.vue'),
        meta: { title: '门店设置', icon: 'Shop', perm: 'store:manage', group: '系统设置' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard', meta: { hidden: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 登录守卫：未登录一律跳转登录页
let checkedToken = null
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('bm_token')
  if (to.path === '/login') return next()
  if (!token) return next('/login')
  const user = useUserStore()
  if (checkedToken !== token) {
    try {
      const profile = await api.me()
      user.user = { ...user.user, ...profile }
      localStorage.setItem('bm_user', JSON.stringify(user.user))
      checkedToken = token
    } catch { user.logout(); return next('/login') }
  }
  if (to.meta.perm && !user.has(to.meta.perm)) {
    const first = routes.find(r => r.path === '/').children.find(r => user.has(r.meta.perm))
    return next(first ? '/' + first.path : '/login')
  }
  next()
})

export default router
