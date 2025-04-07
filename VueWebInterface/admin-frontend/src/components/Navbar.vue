<template>
  <nav class="bg-gray-200 text-gray-800 shadow-md">
    <div class="max-w-6xl mx-auto px-4">
      <div class="flex justify-between items-center py-4">
        <router-link to="/" class="relative flex items-center">
          <span class="font-bold text-2xl block" style="color: #f2be22; -webkit-text-stroke: 0.7px #0f2550;">Jazykové
            kurzy</span>

          <img src="https://priscilla.fitped.eu//img/priscilla-logo.230a794e.svg" alt="Priscilla Logo"
            class="h-4 absolute top-[-10px] right-[-60px]" />
        </router-link>

        <div class="hidden md:flex space-x-6">
          <router-link to="/" class="nav-link">📚 Kurzy</router-link>
          <router-link to="/playersScore" class="nav-link">🏆 Skóre hráčov</router-link>
        </div>

        <button @click="toggleMobileMenu" class="md:hidden focus:outline-none">
          <svg class="w-6 h-6 text-gray-800" fill="none" stroke="currentColor" viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7"></path>
          </svg>
        </button>
      </div>

      <div v-if="mobileMenuOpen" class="md:hidden py-4">
        <router-link to="/" class="mobile-link">📚 Kurzy</router-link>
        <router-link to="/playersScore" class="mobile-link">🏆 Skóre hráčov</router-link>
      </div>
    </div>
  </nav>
</template>

<script>
import { ref } from "vue";

export default {
  setup() {
    const mobileMenuOpen = ref(false);

    const toggleMobileMenu = () => {
      mobileMenuOpen.value = !mobileMenuOpen.value;
    };

    const logout = () => {
      alert("Odhlásenie úspešné! 👋");
    };

    return {
      mobileMenuOpen,
      toggleMobileMenu,
      logout,
    };
  },
};
</script>

<style scoped>
.nav-link {
  @apply text-gray-800 hover:bg-gray-300 px-4 py-2 rounded-lg transition;
}

.mobile-link {
  @apply block text-gray-800 bg-gray-200 py-2 px-4 hover:bg-gray-300 transition w-full rounded-lg;
}
</style>