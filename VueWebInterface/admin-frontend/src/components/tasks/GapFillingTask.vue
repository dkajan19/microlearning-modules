<template>
  <div class="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <label class="block text-lg font-semibold text-gray-700 mb-4">
      ✍️ Vytvorte vetu s medzerou (doplňovanie slov):
    </label>

    <div v-for="(item, index) in task.sentences" :key="index" class="mb-6 p-4 border rounded-lg bg-gray-50 shadow-sm">
      <div class="flex flex-col md:flex-row space-y-2 md:space-y-0 md:space-x-2 mb-2">
        <input v-model="item.sentence" type="text" placeholder="Zadajte vetu s medzerou (_)"
          class="border p-2 rounded-lg w-full md:w-2/3 focus:outline-none focus:ring-2 focus:ring-blue-400" />
        <input v-model="item.correct" type="text" placeholder="Správne slovo"
          class="border p-2 rounded-lg w-full md:w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-400" />
        <input v-model.number="item.points" type="number" placeholder="Body"
          class="border p-2 rounded-lg w-16 focus:outline-none focus:ring-2 focus:ring-blue-400" min="0" />
      </div>

      <div v-if="item.sentence && item.correct" class="mt-2 p-3 border rounded bg-gray-100 text-gray-800 text-lg">
        <span v-html="getFilledSentence(item.sentence, item.correct)"></span>
      </div>

      <button @click="removeSentence(index)"
        class="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition mt-2">
        🗑️ Odstrániť vetu
      </button>
    </div>

    <div class="mt-4 text-lg font-semibold">
      Celkové body: {{ totalPoints.toFixed(2) }}
    </div>

    <button @click="addSentence" class="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition mt-4">
      ➕ Pridať vetu
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
      return this.modelValue && this.modelValue.sentences
        ? this.modelValue
        : { sentences: [] };
    },
    totalPoints() {
      const total = this.task.sentences.reduce((sum, sentence) => sum + (sentence.points || 0), 0);

      this.$emit("update:modelValue", { ...this.task, totalPoints: total });
      return total;
    },

    formattedJson() {
      return JSON.stringify(this.task, null, 2);
    }
  },
  methods: {
    addSentence() {
      this.task.sentences.push({
        sentence: "_",
        correct: "",
        points: 0
      });
      this.$emit("update:modelValue", this.task);
    },

    removeSentence(index) {
      this.task.sentences.splice(index, 1);
      this.$emit("update:modelValue", this.task);
    },

    getFilledSentence(sentence, correct) {
      return sentence.replace(/_/g, `<strong class="text-blue-600">${correct}</strong>`);
    }
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
