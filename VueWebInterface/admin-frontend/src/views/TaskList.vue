<template>
  <div class="max-w-4xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">📌 Úlohy v lekcii</h1>

    <router-link :to="`/course/${courseId}/chapter/${chapterId}/lessons`"
      class="inline-flex items-center text-blue-600 hover:underline mb-6">
      ⬅️ Späť na lekcie
    </router-link>

    <div class="mb-6 p-4 border border-gray-300 rounded-lg shadow-sm bg-gray-50" ref="editTaskContainer">
      <h2 class="text-lg font-semibold text-gray-700 mb-3">
        {{ editingTask ? "✏️ Upraviť úlohu" : "➕ Pridať úlohu" }}
      </h2>

      <input v-model="taskName" :list="tasks" type="text" placeholder="Zadajte názov úlohy"
        class="w-full p-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 mb-3" />


      <select v-model="taskType" @change="handleTaskTypeChange"
        class="w-full p-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 mb-3">
        <option value="translation">Preklad slovíčok</option>
        <option value="matching_definitions">Párovanie s definíciami</option>
        <option value="matching_images">Párovanie s obrázkami</option>
        <option value="categorization">Rozdelenie do kategórií</option>
        <option value="context_choice">Výber slova podľa kontextu</option>
        <option value="sentence_building">Skladanie viet</option>
        <option value="gap_filling">Doplňovanie chýbajúcich slov</option>
      </select>

      <component :is="taskComponent" v-model="taskData" />

      <div class="flex space-x-2 mt-3">
        <button @click="saveTask" class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition">
          {{ editingTask ? "Uložiť zmeny" : "Pridať úlohu" }}
        </button>

        <button v-if="editingTask" @click="cancelEditing"
          class="w-1/2 bg-gray-500 text-white py-2 rounded-lg hover:bg-gray-600 transition">
          ❌ Neuložiť
        </button>
      </div>
    </div>

    <draggable :list="tasks" tag="ul" handle=".drag-handle"
      class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6" :item-key="'id'" @end="saveTaskOrder">
      <template #item="{ element: task }">
        <li :key="task.id" :style="{
          border: '1px solid ' + '#f2be22',
          backgroundColor: 'rgba(242, 190, 34, 0.125)',
        }"
          class="p-4 bg-white border border-gray-300 rounded-lg shadow-sm flex flex-col justify-between items-center space-y-3 transition hover:shadow-md min-h-[120px]">
          <span class="drag-handle cursor-move text-gray-400">☰</span>
          <div class="flex flex-col items-center space-y-2 w-full">
            <div class="flex flex-col items-center w-full">
              <span class="text-lg font-medium text-center truncate w-full">{{ task.name || "Bez názvu" }}</span>
              <span class="text-sm text-gray-600 text-center">{{ task.type }}</span>
            </div>
            <div class="flex justify-between w-full">
              <button @click="toggleTaskVisibility(task)" class="py-1 rounded-lg transition text-xs w-1/3"
                :class="task.visible ? 'bg-green-500 text-white hover:bg-green-600' : 'bg-gray-400 text-white hover:bg-gray-500'">
                {{ task.visible ? '👁️ Zobraziť' : '🙈 Skryť' }}
              </button>
              <button @click="editTask(task)"
                class="py-1 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 transition text-xs w-1/3 ml-1">
                ✏️ Upraviť
              </button>
              <button @click="deleteTask(task.id)"
                class="py-1 bg-red-500 text-white rounded-lg hover:bg-red-600 transition text-xs w-1/3 ml-1">
                🗑️ Vymazať
              </button>
            </div>
          </div>
        </li>
      </template>
    </draggable>
  </div>
</template>

<script>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getTasks, createTask, updateTask, deleteTask, updateTaskOrder } from "../api/api.js";
import TranslationTask from "../components/tasks/TranslationTask.vue";
import MatchingDefinitionsTask from "../components/tasks/MatchingDefinitionsTask.vue";
import MatchingImagesTask from "../components/tasks/MatchingImagesTask.vue";
import CategorizationTask from "../components/tasks/CategorizationTask.vue";
import ContextChoiceTask from "../components/tasks/ContextChoiceTask.vue";
import SentenceBuildingTask from "../components/tasks/SentenceBuildingTask.vue";
import GapFillingTask from "../components/tasks/GapFillingTask.vue";
import draggable from "vuedraggable";

export default {
  components: {
    TranslationTask, MatchingDefinitionsTask, MatchingImagesTask,
    CategorizationTask, ContextChoiceTask, SentenceBuildingTask,
    GapFillingTask, draggable
  },
  setup() {
    const route = useRoute();
    const lessonId = computed(() => route.params.lesson_id);
    const chapterId = computed(() => route.params.chapter_id);
    const courseId = computed(() => route.params.course_id);
    const tasks = ref([]);
    const taskName = ref("");
    const taskType = ref("translation");
    const taskData = ref({});
    const editingTask = ref(null);
    const editTaskContainer = ref(null);
    const isUpdatingOrder = ref(false);

    const taskComponent = computed(() => {
      switch (taskType.value) {
        case "translation": return "TranslationTask";
        case "matching_definitions": return "MatchingDefinitionsTask";
        case "matching_images": return "MatchingImagesTask";
        case "categorization": return "CategorizationTask";
        case "context_choice": return "ContextChoiceTask";
        case "sentence_building": return "SentenceBuildingTask";
        case "gap_filling": return "GapFillingTask";
        default: return null;
      }
    });

    const loadTasks = async () => {
      if (!lessonId.value) return;
      console.log("Načítavam úlohy...");
      try {
        const response = await getTasks(lessonId.value);
        const sortedTasks = response.data.sort((a, b) => a.position - b.position);

        tasks.value.length = 0;
        tasks.value.push(...sortedTasks);

      } catch (error) {
        console.error("Chyba pri načítavaní úloh:", error);
      }
    };

    const saveTask = async () => {
      const newTask = {
        name: taskName.value,
        type: taskType.value,
        data: taskData.value,
        points: taskData.value.totalPoints || 0,
      };

      if (editingTask.value) {
        await updateTask(editingTask.value.id, { ...newTask, visible: editingTask.value.visible });
        editingTask.value = null;
      } else {
        await createTask(lessonId.value, { ...newTask, visible: true });
      }

      resetForm();
      await loadTasks();
    };


    const editTask = (task) => {
      editingTask.value = { ...task };
      taskName.value = task.name;
      taskType.value = task.type;
      taskData.value = { ...task.data };

      if (editTaskContainer.value) {
        editTaskContainer.value.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      }
    };

    const handleTaskTypeChange = () => {
      if (editingTask.value) {
        if (confirm("Neuložené zmeny budú stratené. Chcete pokračovať?")) {
          cancelEditing();
        } else {
          return;
        }
      }
    };

    const cancelEditing = () => {
      editingTask.value = null;
      resetForm();
    };

    const deleteTaskHandler = async (id) => {
      if (confirm("Naozaj chcete vymazať túto úlohu?")) {
        await deleteTask(id);
        await loadTasks();
      }
    };

    const resetForm = () => {
      taskName.value = "";
      taskType.value = "translation";
      taskData.value = {};
    };



    const saveTaskOrder = async () => {
      const orderedTasks = tasks.value.map((task, index) => ({
        id: task.id,
        position: index
      }));

      try {
        await updateTaskOrder(lessonId.value, orderedTasks);
        alert("Poradie lekcií bolo uložené! 🔄");
        await loadTasks();
      } catch (error) {
        console.error("❌ Nepodarilo sa uložiť poradie lekcií:", error);
      }
    };



    const toggleTaskVisibility = async (task) => {
      await updateTask(task.id, { name: task.name, type: task.type, data: task.data, points: task.points, visible: !task.visible });

      const index = tasks.value.findIndex(t => t.id === task.id);
      if (index !== -1) {
        tasks.value[index].visible = !tasks.value[index].visible;
      }
    };



    onMounted(loadTasks);

    return {
      tasks,
      taskName,
      taskType,
      taskData,
      editingTask,
      saveTask,
      editTask,
      handleTaskTypeChange,
      cancelEditing,
      deleteTask: deleteTaskHandler,
      saveTaskOrder,
      courseId,
      chapterId,
      lessonId,
      taskComponent,
      editTaskContainer,
      toggleTaskVisibility,
    };
  },
};
</script>