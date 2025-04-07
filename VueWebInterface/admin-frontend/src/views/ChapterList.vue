<template>
  <div class="max-w-4xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">📖 Kapitoly kurzu</h1>

    <router-link to="/" class="flex items-center text-blue-500 hover:underline mb-4">
      ⬅️ <span class="ml-1">Späť na kurzy</span>
    </router-link>

    <div class="mb-6 p-4 border border-gray-300 rounded-lg shadow-sm bg-gray-50">
      <h2 class="text-lg font-semibold text-gray-700 mb-3">
        {{ editingChapter ? "✏️ Upraviť kapitolu" : "➕ Pridať kapitolu" }}
      </h2>
      <input v-model="chapterName" type="text" placeholder="Zadajte názov kapitoly"
        class="w-full p-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 mb-3" />
      <div class="flex space-x-2">
        <button @click="saveChapter" class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition">
          {{ editingChapter ? "Uložiť zmeny" : "Pridať kapitolu" }}
        </button>
        <button v-if="editingChapter" @click="cancelEdit"
          class="w-full bg-gray-500 text-white py-2 rounded-lg hover:bg-gray-600 transition">
          ❌ Zrušiť úpravy
        </button>
      </div>
    </div>

    <draggable v-model="chapters" tag="ul" handle=".drag-handle" class="space-y-3" @end="saveChapterOrder">
      <template #item="{ element: chapter }">
        <li :key="chapter.id"
          class="p-4 border rounded-lg shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center transition hover:shadow-md"
          :style="{ borderColor: courseColor, backgroundColor: courseColor + '20' }">
          <div class="flex items-center space-x-2 mb-2 sm:mb-0 w-full sm:w-auto">
            <span class="drag-handle cursor-move mr-2">☰</span>
            <span class="text-lg font-medium text-gray-800">{{ chapter.name }}</span>
            <div class="flex space-x-2 ml-2 sm:ml-4">
              <span
                class="inline-flex items-center px-2 py-1 text-xs font-semibold bg-blue-500 text-white rounded-full">
                📝 {{ chapter.lessonCount }}
              </span>
              <span
                class="inline-flex items-center px-2 py-1 text-xs font-semibold bg-green-500 text-white rounded-full">
                📌 {{ chapter.taskCount }}
              </span>
            </div>
          </div>

          <div class="flex space-x-2 mt-2 sm:mt-0 w-full sm:w-auto justify-end">
            <router-link :to="`/course/${courseId}/chapter/${chapter.id}/lessons`"
              class="px-3 py-1 bg-purple-500 text-white rounded-lg hover:bg-purple-600 transition text-xs sm:text-base">
              📝 Lekcie
            </router-link>
            <button @click="toggleChapterVisibility(chapter)"
              class="px-3 py-1 rounded-lg transition text-xs sm:text-base"
              :class="chapter.visible ? 'bg-green-500 text-white hover:bg-green-600' : 'bg-gray-400 text-white hover:bg-gray-500'">
              {{ chapter.visible ? '👁️ Zobraziť' : '🙈 Skryť' }}
            </button>
            <button @click="editChapter(chapter)"
              class="px-3 py-1 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 transition text-xs sm:text-base">
              ✏️ Upraviť
            </button>
            <button @click="deleteChapter(chapter.id)"
              class="px-3 py-1 bg-red-500 text-white rounded-lg hover:bg-red-600 transition text-xs sm:text-base">
              🗑️ Vymazať
            </button>
          </div>
        </li>
      </template>
    </draggable>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getChapters, createChapter, updateChapter, deleteChapter, updateChapterOrder, getCourse, getLessonCount, getTaskCount, getLessons } from "../api/api.js";
import draggable from "vuedraggable";

export default {
  components: { draggable },
  setup() {
    const route = useRoute();
    const courseId = route.params.course_id;
    const chapters = ref();
    const chapterName = ref("");
    const editingChapter = ref(null);
    const courseColor = ref("#000000");

    const loadCourse = async () => {
      const response = await getCourse(courseId);
      courseColor.value = response.data.area_color;
    };

    const loadChapters = async () => {
      const response = await getChapters(courseId);
      chapters.value = response.data.sort((a, b) => a.position - b.position);

      for (let chapter of chapters.value) {
        const lessonResponse = await getLessonCount(chapter.id);
        chapter.lessonCount = lessonResponse.data.count;

        let totalTaskCount = 0;
        try {
          const lessonsResponse = await getLessons(chapter.id);
          const lessons = lessonsResponse.data;

          for (let lesson of lessons) {
            const taskResponse = await getTaskCount(lesson.id);
            totalTaskCount += taskResponse.data.count;
          }
          chapter.taskCount = totalTaskCount;
        } catch (error) {
          console.error("Error fetching lessons or task counts:", error);
          chapter.taskCount = 0;
        }
      }
    };

    const saveChapter = async () => {
      if (editingChapter.value) {
        await updateChapter(editingChapter.value.id, { name: chapterName.value, visible: editingChapter.value.visible });
        editingChapter.value = null;
      } else {
        await createChapter(courseId, { name: chapterName.value });
      }
      chapterName.value = "";
      await loadChapters();
    };

    const cancelEdit = () => {
      editingChapter.value = null;
      chapterName.value = "";
    };

    const saveChapterOrder = async () => {
      const orderedChapters = chapters.value.map((chapter, index) => ({
        id: chapter.id,
        position: index
      }));

      try {
        await updateChapterOrder(courseId, orderedChapters);
        alert("Poradie kapitol bolo uložené! 🔄");
        await loadChapters();
      } catch (error) {
        console.error("❌ Nepodarilo sa uložiť poradie kapitol:", error);
      }
    };

    const editChapter = (chapter) => {
      editingChapter.value = { ...chapter };
      chapterName.value = chapter.name;
    };

    const deleteChapterHandler = async (id) => {
      if (confirm("Naozaj chcete vymazať túto kapitolu?")) {
        await deleteChapter(id);
        await loadChapters();
      }
    };

    const toggleChapterVisibility = async (chapter) => {
      await updateChapter(chapter.id, { name: chapter.name, visible: !chapter.visible });
      const index = chapters.value.findIndex(ch => ch.id === chapter.id);
      if (index !== -1) {
        chapters.value[index].visible = !chapters.value[index].visible;
      }
    };

    onMounted(async () => {
      await loadCourse();
      await loadChapters();
    });

    return {
      chapters,
      chapterName,
      editingChapter,
      saveChapter,
      saveChapterOrder,
      editChapter,
      deleteChapter: deleteChapterHandler,
      courseId,
      courseColor,
      cancelEdit,
      toggleChapterVisibility,
    };
  },
};
</script>