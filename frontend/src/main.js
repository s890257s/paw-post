// 建立 app
import { createApp } from 'vue'
import App from './App.vue'
const app = createApp(App)

// pinia
import { createPinia } from 'pinia'
app.use(createPinia())

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

// 掛載
app.mount('#app')
