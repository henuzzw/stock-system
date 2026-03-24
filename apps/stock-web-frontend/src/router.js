import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from './views/Dashboard.vue'
import Symbols from './views/Symbols.vue'
import Status from './views/Status.vue'
import Register from './views/Register.vue'
import Login from './views/Login.vue'
import Account from './views/Account.vue'
import Orders from './views/Orders.vue'
import Trades from './views/Trades.vue'
import SymbolDetail from './views/SymbolDetail.vue'

const routes = [
  { path: '/', name: 'home', component: Dashboard },
  { path: '/symbols', name: 'symbols', component: Symbols },
  { path: '/status', name: 'status', component: Status },
  { path: '/register', name: 'register', component: Register },
  { path: '/login', name: 'login', component: Login },
  { path: '/account', name: 'account', component: Account },
  { path: '/orders', name: 'orders', component: Orders },
  { path: '/trades', name: 'trades', component: Trades },
  { path: '/symbol/:code', name: 'symbol', component: SymbolDetail, props: true }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
