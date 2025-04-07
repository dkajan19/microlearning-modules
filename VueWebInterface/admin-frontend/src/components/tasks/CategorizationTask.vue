<template>
  <div class="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <label class="block text-lg font-semibold text-gray-700 mb-4">
      📌 Zadajte kategórie a slová:
    </label>

    <div v-for="(categoryData, categoryKey) in task.categories" :key="categoryKey"
      class="mb-6 p-4 border rounded-lg bg-gray-50 shadow-sm">
      <div class="flex items-center space-x-2 mb-2">
        <input v-model="categoryData.name" type="text"
          class="border p-2 rounded-lg w-2/3 focus:outline-none focus:ring-2 focus:ring-blue-400"
          placeholder="Názov kategórie" />
        <button @click="removeCategory(categoryKey)"
          class="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition">
          🗑️
        </button>
      </div>

      <div v-for="(word, index) in categoryData.words" :key="index" class="flex space-x-2 mb-2 items-center">
        <input v-model="categoryData.words[index].text" type="text"
          class="border p-2 rounded-lg w-2/3 focus:outline-none focus:ring-2 focus:ring-blue-400" placeholder="Slovo" />
        <input v-model.number="categoryData.words[index].points" type="number"
          class="border p-2 rounded-lg w-16 focus:outline-none focus:ring-2 focus:ring-blue-400" placeholder="Body"
          min="0" />
        <button @click="removeWord(categoryKey, index)"
          class="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition">
          🗑️
        </button>
      </div>

      <button @click="addWord(categoryKey)"
        class="bg-green-500 text-white px-3 py-1 rounded-lg hover:bg-green-600 transition mt-2">
        ➕ Pridať slovo
      </button>
    </div>

    <div class="mt-4 text-lg font-semibold">
      Celkové body: {{ totalPoints.toFixed(2) }}
    </div>

    <button @click="addCategory" class="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition mt-4">
      ➕ Pridať kategóriu
    </button>

    <div class="mt-6 bg-gray-100 p-4 rounded-lg shadow-md">
      <label class="block text-md font-semibold text-gray-700 mb-2">📜 JSON výstup:</label>
      <pre class="bg-gray-200 p-3 rounded-lg text-sm overflow-x-auto">
        <code>{{ formattedJson }}</code>
      </pre>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    modelValue: Object,
  },
  computed: {
    task() {
      return this.modelValue && this.modelValue.categories
        ? this.modelValue
        : { categories: {} };
    },
    totalPoints() {
      const total = Object.values(this.task.categories).reduce((sum, category) => {
        return sum + category.words.reduce((wordSum, word) => wordSum + (word.points || 0), 0);
      }, 0);

      this.$emit("update:modelValue", { ...this.task, totalPoints: total });
      return total;
    },

    formattedJson() {
      return JSON.stringify(this.task, null, 2);
    }
  },
  methods: {
    addCategory() {
      const newCategoryKey = `category_${Object.keys(this.task.categories).length + 1}`;
      this.task.categories[newCategoryKey] = {
        name: `Kategória ${Object.keys(this.task.categories).length + 1}`,
        words: []
      };
      this.$emit("update:modelValue", this.task);
    },

    removeCategory(categoryKey) {
      delete this.task.categories[categoryKey];
      this.$emit("update:modelValue", this.task);
    },

    addWord(categoryKey) {
      this.task.categories[categoryKey].words.push({ text: '', points: 0 });
      this.$emit("update:modelValue", this.task);
    },

    removeWord(categoryKey, index) {
      this.task.categories[categoryKey].words.splice(index, 1);
      this.$emit("update:modelValue", this.task);
    },
  },
};
</script>

<style scoped>
button {
  font-size: 14px;
  font-weight: bold;
}

button:hover {
  cursor: pointer;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: monospace;
}
</style>
