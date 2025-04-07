<template>
  <div class="max-w-4xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">📝 Lekcie kapitoly</h1>

    <router-link :to="`/course/${courseId}/chapters`"
      class="inline-flex items-center text-blue-600 hover:underline mb-6">
      ⬅️ Späť na kapitoly
    </router-link>

    <div class="mb-6 p-4 border border-gray-300 rounded-lg shadow-sm bg-gray-50">
      <h2 class="text-lg font-semibold text-gray-700 mb-3">
        {{ editingLesson ? "✏️ Upraviť lekciu" : "➕ Pridať lekciu" }}
      </h2>
      <input v-model="lessonName" type="text" placeholder="Zadajte názov lekcie"
        class="w-full p-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 mb-3" />
      <div class="flex space-x-2">
        <button @click="saveLesson" class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition">
          {{ editingLesson ? "Uložiť zmeny" : "Pridať lekciu" }}
        </button>
        <button v-if="editingLesson" @click="cancelEdit"
          class="w-full bg-gray-500 text-white py-2 rounded-lg hover:bg-gray-600 transition">
          ❌ Zrušiť úpravy
        </button>
      </div>
    </div>

    <draggable v-model="lessons" tag="ul" handle=".drag-handle" class="space-y-3" @end="saveLessonOrder"
      :item-key="'id'">
      <template #item="{ element: lesson }">
        <li :key="lesson.id"
          class="p-4 border rounded-lg shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center transition hover:shadow-md"
          :style="{ borderColor: courseColor, backgroundColor: courseColor + '20' }">
          <div class="flex items-center space-x-2 mb-2 sm:mb-0 w-full sm:w-auto flex-wrap">
            <span class="drag-handle cursor-move mr-2">☰</span>
            <span class="text-lg font-medium text-gray-800">{{ lesson.name }}</span>
            <div
              class="inline-flex items-center px-2 py-1 text-xs font-semibold bg-green-500 text-white rounded-full ml-2">
              📌 {{ lesson.taskCount }}
            </div>
          </div>

          <div class="flex space-x-2 mt-2 sm:mt-0 w-full sm:w-auto justify-end">
            <router-link :to="`/course/${courseId}/chapter/${chapterId}/lesson/${lesson.id}/tasks`"
              class="px-3 py-1 bg-purple-500 text-white rounded-lg hover:bg-purple-600 transition text-xs sm:text-base">
              📌 Úlohy
            </router-link>
            <button @click="toggleLessonVisibility(lesson)" class="px-3 py-1 rounded-lg transition text-xs sm:text-base"
              :class="lesson.visible ? 'bg-green-500 text-white hover:bg-green-600' : 'bg-gray-400 text-white hover:bg-gray-500'">
              {{ lesson.visible ? '👁️ Zobraziť' : '🙈 Skryť' }}
            </button>
            <button @click="editLesson(lesson)"
              class="px-3 py-1 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 transition text-xs sm:text-base">
              ✏️ Upraviť
            </button>
            <button @click="deleteLesson(lesson.id)"
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
import { getLessons, createLesson, updateLesson, deleteLesson, updateLessonOrder, getTaskCount, getCourse } from "../api/api.js";
import draggable from "vuedraggable";

export default {
  components: { draggable },
  setup() {
    const route = useRoute();
    const chapterId = route.params.chapter_id;
    const courseId = route.params.course_id;
    const lessons = ref();
    const lessonName = ref("");
    const editingLesson = ref(null);
    const courseColor = ref("#d91a1a");

    const loadCourse = async () => {
      const response = await getCourse(courseId);
      //courseColor.value = response.data.area_color;
    };

    const loadLessons = async () => {
      if (!chapterId) {
        console.error("❌ Chyba: chapterId je undefined!");
        return;
      }
      try {
        const response = await getLessons(chapterId);
        lessons.value = response.data.sort((a, b) => a.position - b.position);

        for (let lesson of lessons.value) {
          const taskResponse = await getTaskCount(lesson.id);
          lesson.taskCount = taskResponse.data.count;
        }
      } catch (error) {
        console.error("❌ Nepodarilo sa načítať lekcie:", error);
      }
    };

    const saveLesson = async () => {
      if (editingLesson.value) {
        await updateLesson(editingLesson.value.id, { name: lessonName.value, visible: editingLesson.value.visible });
        editingLesson.value = null;
      } else {
        await createLesson(chapterId, { name: lessonName.value });
      }
      lessonName.value = "";
      await loadLessons();
    };

    const editLesson = (lesson) => {
      editingLesson.value = { ...lesson };
      lessonName.value = lesson.name;
    };

    const cancelEdit = () => {
      editingLesson.value = null;
      lessonName.value = "";
    };

    const deleteLessonHandler = async (id) => {
      if (confirm("Naozaj chcete vymazať túto lekciu?")) {
        await deleteLesson(id);
        await loadLessons();
      }
    };

    const saveLessonOrder = async () => {
      const orderedLessons = lessons.value.map((lesson, index) => ({
        id: lesson.id,
        position: index
      }));

      try {
        await updateLessonOrder(chapterId, orderedLessons);
        alert("Poradie lekcií bolo uložené! 🔄");
        await loadLessons();
      } catch (error) {
        console.error("❌ Nepodarilo sa uložiť poradie lekcií:", error);
      }
    };

    const toggleLessonVisibility = async (lesson) => {
      await updateLesson(lesson.id, { name: lesson.name, visible: !lesson.visible });
      const index = lessons.value.findIndex(l => l.id === lesson.id);
      if (index !== -1) {
        lessons.value[index].visible = !lessons.value[index].visible;
      }
    };

    onMounted(async () => {
      await loadCourse();
      await loadLessons();
    });

    return {
      lessons,
      lessonName,
      editingLesson,
      saveLesson,
      editLesson,
      deleteLesson: deleteLessonHandler,
      saveLessonOrder,
      cancelEdit,
      courseId,
      chapterId,
      courseColor,
      toggleLessonVisibility,
    };
  },
};
</script>