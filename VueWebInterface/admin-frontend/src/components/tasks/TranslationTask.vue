<template>
  <div class="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <label class="block text-lg font-semibold text-gray-700 mb-4">
      🌍 Zadajte slová a ich preklad:
    </label>

    <div v-for="(pair, index) in task.pairs" :key="index"
      class="flex items-center space-x-2 mb-3 p-3 border rounded-lg bg-gray-50 shadow-sm">
      <input v-model="pair.word" type="text" placeholder="Slovo"
        class="border p-2 rounded-lg w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-400" />
      <input v-model="pair.translation" type="text" placeholder="Preklad"
        class="border p-2 rounded-lg w-1/3 focus:outline-none focus:ring-2 focus:ring-green-400" />
      <input v-model.number="pair.points" type="number" placeholder="Body"
        class="border p-2 rounded-lg w-16 focus:outline-none focus:ring-2 focus:ring-yellow-400" min="0" />
      <button @click="removePair(index)" class="bg-red-500 text-white px-3 py-2 rounded-lg hover:bg-red-600 transition">
        🗑️
      </button>
    </div>

    <div class="mt-4 text-lg font-semibold">
      Celkové body: {{ totalPoints.toFixed(2) }}
    </div>

    <button @click="addPair" class="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition">
      ➕ Pridať slovo
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
      return this.modelValue && this.modelValue.pairs ? this.modelValue : { pairs: [] };
    },
    totalPoints() {
      const total = this.task.pairs.reduce((sum, pair) => sum + (pair.points || 0), 0);

      this.$emit("update:modelValue", { ...this.task, totalPoints: total });
      return total;
    },

    formattedJson() {
      return JSON.stringify(this.task, null, 2);
    },
  },
  methods: {
    addPair() {
      this.task.pairs.push({ word: "", translation: "", points: 0 });
      this.$emit("update:modelValue", this.task);
    },
    removePair(index) {
      this.task.pairs.splice(index, 1);
      this.$emit("update:modelValue", this.task);
    },
  },
};
</script>

<style scoped>
input {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 8px;
  transition: all 0.2s ease-in-out;
}

input:focus {
  border-color: #4a90e2;
  box-shadow: 0 0 5px rgba(74, 144, 226, 0.5);
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: monospace;
}
</style>
