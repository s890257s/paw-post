// 建立 app
import { createApp } from 'vue'
import App from './App.vue'
const app = createApp(App)

// pinia
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

// router
import router from './router'
app.use(router)

// vuetify
import "@mdi/font/css/materialdesignicons.css";
import "vuetify/styles";
import { createVuetify } from "vuetify";
import * as components from "vuetify/components";
import * as directives from "vuetify/directives";
const vuetify = createVuetify({ components, directives });
app.use(vuetify);

// vue-toastification
import Toast from "vue-toastification";
import "vue-toastification/dist/index.css";
app.use(Toast, {
    position: "bottom-right", // 訊息顯示在右下角
    timeout: 3000,         // 訊息顯示 3 秒後自動消失
});

// 掛載
app.mount('#app')
