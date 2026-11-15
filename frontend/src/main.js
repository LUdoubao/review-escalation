import { createApp } from 'vue'
import App from './App.vue'
import './styles/main.css'

// 基线不引入路由与全局状态容器：单一页面即可承载联通性验证，减少后续定位改动落点时的干扰。
createApp(App).mount('#app')
