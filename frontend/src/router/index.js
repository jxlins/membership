import { createRouter, createWebHistory } from 'vue-router'

import SocietyMemberEntry from '../views/society/MemberEntry.vue'
import SocietyMemberAdmin from '../views/society/MemberAdmin.vue'
import { hasAdminAuth } from '../utils/adminAuth'

const routes = [
  {
    path: '/',
    redirect: '/society/member-entry'
  },
  {
    path: '/society/member-entry',
    name: 'SocietyMemberEntry',
    component: SocietyMemberEntry
  },
  {
    path: '/society/member-admin',
    name: 'SocietyMemberAdmin',
    component: SocietyMemberAdmin,
    meta: {
      requiresAdmin: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE_PATH || '/member/'),
  routes
})

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAdmin && !hasAdminAuth()) {
    next({ path: '/society/member-entry' })
    return
  }
  next()
})

export default router
