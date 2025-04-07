<template>
  <div class="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <label class="block text-lg font-semibold text-gray-700 mb-4">📝 Zadajte kontexty a možnosti:</label>

    <div v-for="(contextData, contextKey) in task.contexts" :key="contextKey"
      class="mb-6 p-4 border rounded-lg bg-gray-50 shadow-sm">
      <div class="flex items-center space-x-2 mb-2">
        <input v-model="contextData.name" type="text"
          class="border p-2 rounded-lg w-2/3 focus:outline-none focus:ring-2 focus:ring-blue-400"
          placeholder="Názov kontextu" />
        <button @click="removeContext(contextKey)"
          class="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition">
          🗑️
        </button>
      </div>

      <div v-for="(option, index) in contextData.options" :key="index" class="flex space-x-2 mb-2 items-center">
        <input v-model="contextData.options[index].text" type="text"
          class="border p-2 rounded-lg w-2/3 focus:outline-none focus:ring-2 focus:ring-blue-400"
          placeholder="Možnosť" />
        <input v-model.number="contextData.options[index].points" type="number"
          class="border p-2 rounded-lg w-16 focus:outline-none focus:ring-2 focus:ring-blue-400" placeholder="Body"
          min="0" />
        <button @click="removeOption(contextKey, index)"
          class="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition">
          🗑️
        </button>
      </div>

      <button @click="addOption(contextKey)"
        class="bg-green-500 text-white px-3 py-1 rounded-lg hover:bg-green-600 transition mt-2">
        ➕ Pridať možnosť
      </button>
    </div>

    <div class="mt-4 text-lg font-semibold">
      Celkové body: {{ totalPoints.toFixed(2) }}
    </div>

    <button @click="addContext" class="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition mt-4">
      ➕ Pridať kontext
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
      return this.modelValue && this.modelValue.contexts
        ? this.modelValue
        : { contexts: {} };
    },
    totalPoints() {
      const total = Object.values(this.task.contexts).reduce((sum, context) => {
        return sum + context.options.reduce((optionSum, option) => optionSum + (option.points || 0), 0);
      }, 0);

      this.$emit("update:modelValue", { ...this.task, totalPoints: total });
      return total;
    },

    formattedJson() {
      return JSON.stringify(this.task, null, 2);
    }
  },
  methods: {
    addContext() {
      const newContextKey = `context_${Object.keys(this.task.contexts).length + 1}`;
      this.task.contexts[newContextKey] = { name: `Kontext ${Object.keys(this.task.contexts).length + 1}`, options: [] };
      this.$emit("update:modelValue", this.task);
    },

    removeContext(contextKey) {
      delete this.task.contexts[contextKey];
      this.$emit("update:modelValue", this.task);
    },

    addOption(contextKey) {
      this.task.contexts[contextKey].options.push({ text: '', points: 0 });
      this.$emit("update:modelValue", this.task);
    },

    removeOption(contextKey, index) {
      this.task.contexts[contextKey].options.splice(index, 1);
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
