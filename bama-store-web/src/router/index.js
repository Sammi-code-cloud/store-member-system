import { createRouter, createWebHashHistory } from 'vue-router'

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
        meta: { title: '数据概览', icon: 'DataLine', perm: 'dashboard:view', group: '门店运营' } },
      { path: 'recharge', component: () => import('@/views/recharge.vue'),
        meta: { title: '代客储值', icon: 'Wallet', perm: 'account:recharge', group: '门店运营' } },
      { path: 'reservation', component: () => import('@/views/reservation.vue'),
        meta: { title: '预定核销', icon: 'Calendar', perm: 'reservation:view', group: '门店运营' } },

      { path: 'member', component: () => import('@/views/member.vue'),
        meta: { title: '会员管理', icon: 'User', perm: 'member:view', group: '资料管理' } },
      { path: 'product', component: () => import('@/views/product.vue'),
        meta: { title: '货品管理', icon: 'Goods', perm: 'product:view', group: '资料管理' } },
      { path: 'room', component: () => import('@/views/room.vue'),
        meta: { title: '茶室管理', icon: 'House', perm: 'reservation:manage', group: '资料管理' } },

      { path: 'staff', component: () => import('@/views/staff.vue'),
        meta: { title: '员工与权限', icon: 'Avatar', perm: 'staff:view', group: '系统设置' } },
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
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('bm_token')
  if (to.path === '/login') return next()
  if (!token) return next('/login')
  next()
})

export default router
