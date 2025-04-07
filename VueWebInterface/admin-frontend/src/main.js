import { createApp } from "vue";
import App from "./App.vue";
import router from "./router/router.js";
import "./index.css";

const isDarkMode = localStorage.getItem("theme") === "dark";
if (isDarkMode) {
  document.documentElement.classList.add("dark");
} else {
  document.documentElement.classList.remove("dark");
}

createApp(App).use(router).mount("#app");
