<template>
  <div class="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <label class="block text-lg font-semibold text-gray-700 mb-4">
      📝 Zadajte páry slovíčko - obrázok:
    </label>

    <div v-for="(pair, index) in task.pairs" :key="index"
      class="flex flex-col md:flex-row items-center space-y-2 md:space-y-0 md:space-x-3 mb-3 p-3 border rounded-lg bg-gray-50 shadow-sm">
      <input v-model="pair.word" type="text" placeholder="Slovo"
        class="border p-2 rounded-lg w-full md:w-1/4 focus:outline-none focus:ring-2 focus:ring-blue-400" />

      <input type="file" @change="onFileChange($event, index)"
        class="border p-2 rounded-lg w-full md:w-1/3 focus:outline-none focus:ring-2 focus:ring-blue-400" />

      <img v-if="pair.image" :src="pair.image"
        class="h-14 w-14 object-cover rounded-lg border cursor-pointer transition-transform hover:scale-110"
        @click="openImage(pair.image)" />

      <input v-model.number="pair.points" type="number" placeholder="Body"
        class="border p-2 rounded-lg w-16 focus:outline-none focus:ring-2 focus:ring-blue-400" min="0" />

      <button @click="removePair(index)" class="bg-red-500 text-white px-3 py-1 rounded-lg hover:bg-red-600 transition">
        🗑️
      </button>
    </div>

    <div class="mt-4 text-lg font-semibold">
      Celkové body: {{ totalPoints.toFixed(2) }}
    </div>

    <button @click="addPair" class="bg-green-500 text-white px-4 py-2 rounded-lg hover:bg-green-600 transition">
      ➕ Pridať pár
    </button>

    <div class="mt-6 bg-gray-100 p-4 rounded-lg shadow-md">
      <label class="block text-md font-semibold text-gray-700 mb-2">📜 JSON výstup:</label>
      <pre class="bg-gray-200 p-3 rounded-lg text-sm overflow-x-auto">
        <code>{{ formattedJson }}</code>
      </pre>
    </div>

    <div v-if="fullscreenImage"
      class="fixed inset-0 bg-black bg-opacity-80 flex items-center justify-center transition-opacity duration-300"
      @click="fullscreenImage = null">
      <img :src="fullscreenImage"
        class="max-w-full max-h-full rounded-lg shadow-lg transition-transform transform scale-100" />
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      fullscreenImage: null,
    };
  },
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
      const taskCopy = JSON.parse(JSON.stringify(this.task));

      taskCopy.pairs.forEach(pair => {
        if (pair.image) {
          pair.image = "BASE64STRING";
        }
      });

      return JSON.stringify(taskCopy, null, 2);
    },
  },
  methods: {
    addPair() {
      this.task.pairs.push({ word: "", image: "", points: 0 });
      this.$emit("update:modelValue", this.task);
    },
    removePair(index) {
      this.task.pairs.splice(index, 1);
      this.$emit("update:modelValue", this.task);
    },
    onFileChange(event, index) {
      const file = event.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.task.pairs[index].image = e.target.result;
          this.$emit("update:modelValue", this.task);
        };
        reader.readAsDataURL(file);
      }
    },
    openImage(image) {
      this.fullscreenImage = image;
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
