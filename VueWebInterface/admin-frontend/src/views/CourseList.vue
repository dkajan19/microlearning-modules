<template>
    <div class="max-w-4xl mx-auto p-6 bg-white shadow-lg rounded-lg">
        <h1 class="text-3xl font-bold text-gray-800 mb-6">📚 Kurzy</h1>

        <div class="mb-6 p-4 border border-gray-300 rounded-lg shadow-sm bg-gray-50">
            <h2 class="text-lg font-semibold text-gray-700 mb-3">
                {{ editingCourse ? "✏️ Upraviť kurz" : "➕ Pridať kurz" }}
            </h2>
            <input v-model="courseName" type="text" placeholder="Zadajte názov kurzu"
                class="w-full p-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 mb-3" />
            <textarea v-model="courseDescription" placeholder="Zadajte popis kurzu"
                class="w-full p-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 mb-3"></textarea>
            <div class="flex items-center space-x-3 mb-3">
                <input v-model="courseColor" type="color" class="w-12 h-12 border rounded-lg cursor-pointer" />
                <span class="text-gray-700 text-sm">{{ courseColor }}</span>
            </div>
            <div class="mb-3">
                <input type="file" @change="onFileChange"
                    class="w-full p-2 border rounded-lg cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-400" />
            </div>
            <div v-if="courseIcon" class="flex flex-col items-center mb-3">
                <img :src="courseIcon" class="h-16 w-16 object-cover rounded-lg border shadow-sm mb-2" />
                <button @click="removeIcon"
                    class="px-2 py-1 text-xs bg-red-500 text-white rounded-lg hover:bg-red-600 transition">
                    ❌ Odstrániť ikonku
                </button>
            </div>
            <div class="flex space-x-2">
                <button @click="saveCourse"
                    class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition">
                    {{ editingCourse ? "Uložiť zmeny" : "Pridať kurz" }}
                </button>
                <button v-if="editingCourse" @click="cancelEdit"
                    class="w-full bg-gray-500 text-white py-2 rounded-lg hover:bg-gray-600 transition">
                    ❌ Zrušiť úpravy
                </button>
            </div>
        </div>

        <ul class="space-y-3">
            <li v-for="course in courses" :key="course.id"
                class="p-4 border rounded-lg shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center transition hover:shadow-md"
                :style="{ borderColor: course.area_color, backgroundColor: course.area_color + '20' }">
                <div class="flex items-center space-x-3 mb-2 sm:mb-0">
                    <img v-if="course.icon" :src="course.icon"
                        class="h-10 w-10 object-cover rounded-lg border shadow-sm" />
                    <span class="text-lg font-medium">{{ course.name }}</span>
                    <div class="flex space-x-2 ml-0 sm:ml-4">
                        <span
                            class="inline-flex items-center px-2 py-1 text-xs font-semibold bg-red-500 text-white rounded-full">
                            📖 {{ course.chapterCount }}
                        </span>
                        <span
                            class="inline-flex items-center px-2 py-1 text-xs font-semibold bg-blue-500 text-white rounded-full">
                            📝 {{ course.lessonCount }}
                        </span>
                        <span
                            class="inline-flex items-center px-2 py-1 text-xs font-semibold bg-green-500 text-white rounded-full">
                            📌 {{ course.taskCount }}
                        </span>
                    </div>
                </div>
                <div class="flex space-x-2 mt-2 sm:mt-0">
                    <router-link :to="`/course/${course.id}/chapters`"
                        class="px-3 py-1 bg-purple-500 text-white rounded-lg hover:bg-purple-600 transition text-xs sm:text-base">
                        📖 Kapitoly
                    </router-link>
                    <button @click="toggleCourseVisibility(course)"
                        class="px-3 py-1 rounded-lg transition text-xs sm:text-base"
                        :class="course.visible ? 'bg-green-500 text-white hover:bg-green-600' : 'bg-gray-400 text-white hover:bg-gray-500'">
                        {{ course.visible ? '👁️ Zobraziť' : '🙈 Skryť' }}
                    </button>
                    <button @click="editCourse(course)"
                        class="px-3 py-1 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 transition text-xs sm:text-base">
                        ✏️ Upraviť
                    </button>
                    <button @click="deleteCourse(course.id)"
                        class="px-3 py-1 bg-red-500 text-white rounded-lg hover:bg-red-600 transition text-xs sm:text-base">
                        🗑️ Vymazať
                    </button>
                </div>
            </li>
        </ul>
    </div>
</template>

<script>
import { ref, onMounted } from "vue";
import { getCourses, createCourse, updateCourse, deleteCourse, getChapterCount, getLessonCount, getTaskCount, getChapters, getLessons } from "../api/api.js";

export default {
    setup() {
        const courses = ref();
        const courseName = ref("");
        const courseColor = ref("#000000");
        const courseIcon = ref(null);
        const editingCourse = ref(null);
        const courseDescription = ref("");

        const loadCourses = async () => {
            const response = await getCourses();
            courses.value = response.data;

            for (let course of courses.value) {
                try {
                    const chapterResponse = await getChapterCount(course.id);
                    course.chapterCount = chapterResponse.data.count;

                    let totalLessonCount = 0;
                    let totalTaskCount = 0;

                    const chaptersResponse = await getChapters(course.id);
                    const chapters = chaptersResponse.data;

                    for (let chapter of chapters) {
                        const lessonResponse = await getLessonCount(chapter.id);
                        totalLessonCount += lessonResponse.data.count;

                        const lessonsResponse = await getLessons(chapter.id);
                        const lessons = lessonsResponse.data;

                        for (let lesson of lessons) {
                            const taskResponse = await getTaskCount(lesson.id);
                            totalTaskCount += taskResponse.data.count;
                        }
                    }
                    course.lessonCount = totalLessonCount;
                    course.taskCount = totalTaskCount;
                } catch (error) {
                    console.error("Error fetching counts for course:", error);
                    course.chapterCount = 0;
                    course.lessonCount = 0;
                    course.taskCount = 0;
                }
            }
        };

        const saveCourse = async () => {
            const courseData = {
                name: courseName.value,
                description: courseDescription.value,
                area_color: courseColor.value,
                icon: courseIcon.value !== null ? courseIcon.value : "",
            };

            if (editingCourse.value) {
                await updateCourse(editingCourse.value.id, { ...courseData, visible: editingCourse.value.visible });
                editingCourse.value = null;
            } else {
                await createCourse(courseData);
            }

            resetForm();
            await loadCourses();
        };

        const editCourse = (course) => {
            editingCourse.value = { ...course };
            courseName.value = course.name;
            courseDescription.value = course.description;
            courseColor.value = course.area_color || "#000000";
            courseIcon.value = course.icon !== "" ? course.icon : null;

            document.querySelector('input[type="file"]').value = "";
        };

        const cancelEdit = () => {
            editingCourse.value = null;
            resetForm();
        };

        const deleteCourseHandler = async (id) => {
            if (confirm("Naozaj chcete vymazať tento kurz?")) {
                await deleteCourse(id);
                await loadCourses();
            }
        };

        const onFileChange = (event) => {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    courseIcon.value = e.target.result;
                };
                reader.readAsDataURL(file);
            }

            event.target.value = "";
        };

        const removeIcon = () => {
            courseIcon.value = null;
            if (editingCourse.value) {
                editingCourse.value.icon = "";
            }
        };

        const resetForm = () => {
            courseName.value = "";
            courseDescription.value = "";
            courseColor.value = "#000000";
            courseIcon.value = null;
        };

        const toggleCourseVisibility = async (course) => {
            await updateCourse(course.id, {
                name: course.name,
                description: course.description,
                area_color: course.area_color,
                icon: course.icon,
                visible: !course.visible,
            });
            await loadCourses();
        };

        onMounted(loadCourses);

        return {
            courses,
            courseName,
            courseDescription,
            courseColor,
            courseIcon,
            editingCourse,
            saveCourse,
            editCourse,
            deleteCourse: deleteCourseHandler,
            onFileChange,
            removeIcon,
            cancelEdit,
            toggleCourseVisibility,
        };
    },
};
</script>
