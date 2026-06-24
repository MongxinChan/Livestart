import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import { message } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)

app.config.errorHandler = (error, instance, info) => {
  console.error('[App ErrorHandler]', {
    error,
    info,
    component: instance?.type,
  })
  message.error('后台页面运行时报错，请打开控制台查看 [App ErrorHandler] 日志')
}

window.addEventListener('error', (event) => {
  console.error('[Window Error]', {
    message: event.message,
    filename: event.filename,
    lineno: event.lineno,
    colno: event.colno,
    error: event.error,
  })
})

window.addEventListener('unhandledrejection', (event) => {
  console.error('[Unhandled Rejection]', event.reason)
})

app.use(Antd)
app.use(router)
app.mount('#app')
