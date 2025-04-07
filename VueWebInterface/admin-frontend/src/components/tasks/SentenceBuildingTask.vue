<template>
  <div class="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <label class="block text-lg font-semibold text-gray-700 mb-4">
      🔤 Zadajte vety na skládanie:
    </label>

    <div v-for="(sentence, index) in task.sentences" :key="index"
      class="mb-6 p-4 border rounded-lg bg-gray-50 shadow-sm">
      <div class="mb-2">
        <strong class="text-blue-600">✅ Správna veta:</strong>
        <input v-model="sentence.correct" type="text" placeholder="Zadajte správnu vetu"
          class="border p-2 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-blue-400"
          @input="splitWords(index)" />
      </div>

      <div class="p-3 bg-white border rounded-lg shadow-md">
        <draggable v-model="sentence.words" group="words" class="flex flex-wrap gap-2" item-key="text">
          <template #item="{ element }">
            <span class="draggable-item">{{ element }}</span>
          </template>
        </draggable>
      </div>

      <div class="mt-2 text-gray-700 text-sm">
        🔢 Počet slov: {{ sentence.words.length }} | 🎯 Body: {{ sentence.points }}
      </div>

      <button @click="removeSentence(index)"
        class="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600 transition mt-3">
        🗑️ Odstrániť vetu
      </button>
    </div>

    <div class="mt-4 text-lg font-semibold">
      Celkové body: {{ totalPoints.toFixed(2) }}
    </div>

    <button @click="addSentence" class="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600 transition">
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
import draggable from "vuedraggable";

export default {
  components: {
    draggable,
  },
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
    },
  },
  methods: {
    addSentence() {
      this.task.sentences.push({ correct: "", words: [], points: 0 });
      this.$emit("update:modelValue", this.task);
    },

    removeSentence(index) {
      this.task.sentences.splice(index, 1);
      this.$emit("update:modelValue", this.task);
    },

    splitWords(index) {
      const sentence = this.task.sentences[index];
      if (sentence.correct) {
        sentence.words = sentence.correct.split(" ").sort(() => Math.random() - 0.5);
        sentence.points = sentence.words.length;
        this.$emit("update:modelValue", this.task);
      }
    },
  },
};
</script>

<style scoped>
.draggable-item {
  display: inline-block;
  padding: 10px 14px;
  background-color: #f0f4f8;
  border: 1px solid #cbd5e0;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s ease-in-out;
}

.draggable-item:hover {
  background-color: #e2e8f0;
  transform: scale(1.05);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 8px;
  border: 1px solid #ddd;
}

th {
  background-color: #f4f4f4;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: monospace;
}
</style>
