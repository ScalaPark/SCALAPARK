import { createRouter, createWebHistory } from 'vue-router'
import OperatorPage from '../views/OperatorPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'operator',
      component: OperatorPage
    }
  ],
})

export default router
