import { createRouter, createWebHistory } from 'vue-router'
import SocietyMemberEntry from '../views/society/MemberEntry.vue'

const routes = [
  {
    path: '/',
    redirect: '/society/member-entry'
  },
  {
    path: '/society/member-entry',
    name: 'SocietyMemberEntry',
    component: SocietyMemberEntry
  }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
