<template>
  <div class="max-w-4xl mx-auto p-6 bg-white shadow-lg rounded-lg">
    <h1 class="text-3xl font-bold text-gray-800 mb-6">🏆 Skóre hráčov</h1>

    <div v-if="loading" class="text-center text-gray-500">Loading leaderboard data...</div>

    <div v-else v-for="player in leaderboardData" :key="player.id" class="pb-4">
      <div @click="togglePlayer(player.id)" :style="{
        border: '1px solid ' + '#f2561d',
        backgroundColor: getRGBA('#f2561d'),
        color: '#f2561d',
      }"
        class="player-item cursor-pointer flex justify-between items-center p-4 bg-gray-100 hover:bg-gray-200 rounded-lg shadow-md"
        :class="{ 'cursor-not-allowed': player.courses.length === 0 }"
        :title="player.courses.length === 0 ? 'No courses available' : ''"
        :aria-disabled="player.courses.length === 0 ? 'true' : 'false'">
        <span class="font-semibold text-lg">🧑‍🏫 {{ player.name || 'Bez mena' }}</span>
        <span class="text-xs italic">(user_id:{{ player.user_id || 'žiadna hodnota' }})</span>
        <span class="text-xs italic">({{ player.courses.length }} kurzy)</span>
        <span class="text-sm font-semibold">{{ player.totalPoints.toFixed(2) }} / {{ player.maxPoints.toFixed(2)
          }}</span>
      </div>

      <div v-if="expandedPlayer === player.id" class="ml-6 mt-3">
        <div v-for="course in player.courses" :key="course.id" class="mb-4">
          <div @click="toggleCourse(course.id)" :style="{
            border: '1px solid ' + course.area_color,
            backgroundColor: getRGBA(course.area_color),
            color: black,
          }"
            class="cursor-pointer flex justify-between items-center p-4 course-item rounded-lg shadow-sm transition-all duration-300 ease-in-out">
            <span class="font-semibold">📚 {{ course.name || 'Bez názvu' }}</span>
            <span class="text-xs italic">(course_id:{{ course.id || 'žiadna hodnota' }})</span>
            <span class="text-xs italic">({{ course.chapters.length }} kapitoly)</span>
            <span class="text-sm font-semibold">{{ course.totalPoints.toFixed(2) }} / {{ course.maxPoints.toFixed(2)
              }}</span>
          </div>


          <div v-if="expandedCourse === course.id" class="ml-6 mt-3">
            <div v-for="chapter in course.chapters" :key="chapter.id" class="mb-4">
              <div @click="toggleChapter(chapter.id)" :style="{
                border: '1px solid ' + course.area_color,
                backgroundColor: getRGBA(course.area_color),
                color: course.black,
              }"
                class="cursor-pointer flex justify-between items-center p-4 chapter-item rounded-lg shadow-sm transition-all duration-300 ease-in-out">
                <span class="font-semibold">📖 {{ chapter.name || 'Bez názvu' }}</span>
                <span class="text-xs italic">(chapter_id:{{ chapter.id || 'žiadna hodnota' }})</span>
                <span class="text-xs italic">({{ chapter.lessons.length }} lekcie)</span>
                <span class="text-sm font-semibold">{{ chapter.totalPoints.toFixed(2) }} / {{
                  chapter.maxPoints.toFixed(2) }}</span>
              </div>


              <div v-if="expandedChapter === chapter.id" class="ml-6 mt-3">
                <div v-for="lesson in chapter.lessons" :key="lesson.id" class="mb-4">
                  <div @click="toggleLesson(lesson.id)" :style="{
                    border: '1px solid ' + '#d91a1a',
                    backgroundColor: getRGBA('#d91a1a'),
                    color: '#d91a1a',
                  }"
                    class="cursor-pointer flex justify-between items-center p-4 lesson-item rounded-lg shadow-sm transition-all duration-300 ease-in-out">
                    <span class="font-semibold">📝 {{ lesson.name || 'Bez názvu' }}</span>
                    <span class="text-xs italic">(lesson_id:{{ lesson.id || 'žiadna hodnota' }})</span>
                    <span class="text-xs italic">({{ lesson.tasks.length }} úlohy)</span>
                    <span class="text-sm font-semibold">{{ lesson.totalPoints.toFixed(2) }} / {{
                      lesson.maxPoints.toFixed(2) }}</span>
                  </div>

                  <div v-if="expandedLesson === lesson.id" class="ml-6 mt-3">
                    <div v-for="task in lesson.tasks" :key="task.id" class="mb-4">
                      <div @click="toggleTask(task.id)" :style="{
                        border: '1px solid ' + '#f2be22',
                        backgroundColor: getRGBA('#f2be22'),
                        color: '#f2be22',
                      }"
                        class="cursor-pointer flex justify-between items-center p-4 task-item rounded-lg shadow-sm transition-all duration-300 ease-in-out">
                        <span class="font-semibold">📌 {{ task.name || 'Bez názvu' }}</span>
                        <span class="text-xs italic">(task_id:{{ task.id || 'žiadna hodnota' }})</span>
                        <span v-if="task.type" class="italic text-xs">({{ task.type }})</span>
                        <span class="text-xs italic">({{ task.attempts }}) pokusy</span>
                        <span class="text-sm font-semibold">{{ task.latestScore.toFixed(2) }} / {{
                          task.points.toFixed(2) }}</span>
                      </div>

                      <div v-if="expandedTask === task.id" class="ml-6 mt-3">
                        <div v-for="attempt in task.attemptsData" :key="attempt.attemptNumber"
                          class="text-sm text-gray-600">
                          <div class="flex justify-between items-center p-4 border-b-2 border-gray-500">
                            <span class="font-semibold">🎯 Pokus {{ attempt.attemptNumber }}</span>
                            <span class="text-xs italic">(attempt_id:{{ attempt.playerscoreId || 'žiadna hodnota'
                              }})</span>
                            <span class="text-sm">{{ attempt.score.toFixed(2) }} body</span>
                            <span class="text-xs italic">Dátum: {{ attempt.createdAt }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getPlayers, getPlayerScores, getCourses, getChapters, getLessons, getTasks } from "../api/api";

export default {
  data() {
    return {
      loading: false,
      expandedPlayer: null,
      expandedCourse: null,
      expandedChapter: null,
      expandedLesson: null,
      expandedTask: null,
      leaderboardData: [],
    };
  },
  mounted() {
    this.fetchLeaderboardData();
  },
  methods: {
    togglePlayer(playerId) {
      const player = this.leaderboardData.find(p => p.id === playerId);
      if (player && player.courses.length > 0) {
        this.expandedPlayer = this.expandedPlayer === playerId ? null : playerId;
        this.expandedCourse = null;
        this.expandedChapter = null;
        this.expandedLesson = null;
        this.expandedTask = null;
      }
    },
    toggleCourse(courseId) {
      this.expandedCourse = this.expandedCourse === courseId ? null : courseId;
      this.expandedChapter = null;
      this.expandedLesson = null;
      this.expandedTask = null;
    },
    toggleChapter(chapterId) {
      this.expandedChapter = this.expandedChapter === chapterId ? null : chapterId;
      this.expandedLesson = null;
      this.expandedTask = null;
    },
    toggleLesson(lessonId) {
      this.expandedLesson = this.expandedLesson === lessonId ? null : lessonId;
      this.expandedTask = null;
    },
    toggleTask(taskId) {
      this.expandedTask = this.expandedTask === taskId ? null : taskId;
    },

    getRGBA(hex) {
      if (!hex) return 'rgba(242, 190, 34, 0.2)';

      hex = hex.replace('#', '');

      let r = parseInt(hex.substring(0, 2), 16);
      let g = parseInt(hex.substring(2, 4), 16);
      let b = parseInt(hex.substring(4, 6), 16);

      return `rgba(${r}, ${g}, ${b}, 0.125)`;
    },

    async fetchLeaderboardData() {
      this.loading = true;
      try {
        const playersResponse = await getPlayers();
        const players = playersResponse.data;

        const coursesResponse = await getCourses();
        const courses = coursesResponse.data;

        this.leaderboardData = await this.processLeaderboardData(players, courses);
      } catch (error) {
        console.error("Error fetching leaderboard data:", error);
      } finally {
        this.loading = false;
      }
    },
    async processLeaderboardData(players, courses) {
      const allPlayerScoresResponse = await Promise.all(players.map(player => getPlayerScores(player.id)));
      const allPlayerScores = allPlayerScoresResponse.map(response => ({
        playerId: response.config.url.split('/').pop(),
        scores: response.data
      }));

      return await Promise.all(players.map(async (player) => {
        const playerSpecificScores = allPlayerScores.find(ps => ps.playerId === String(player.id))?.scores || [];
        const playerCourses = [];
        let playerTotalPoints = 0;
        let playerMaxPoints = 0;

        for (const course of courses) {
          const chaptersResponse = await getChapters(course.id);
          //const chapters = chaptersResponse.data;
          const chapters = chaptersResponse.data.sort((a, b) => a.position - b.position);
          const courseData = { ...course, totalPoints: 0, maxPoints: 0, chapters: [] };

          for (const chapter of chapters) {
            const lessonsResponse = await getLessons(chapter.id);
            //const lessons = lessonsResponse.data;
            const lessons = lessonsResponse.data.sort((a, b) => a.position - b.position);
            const chapterData = { ...chapter, totalPoints: 0, maxPoints: 0, lessons: [] };

            for (const lesson of lessons) {
              const tasksResponse = await getTasks(lesson.id);
              //const tasks = tasksResponse.data;
              const tasks = tasksResponse.data.sort((a, b) => a.position - b.position);
              const lessonData = { ...lesson, totalPoints: 0, maxPoints: 0, tasks: [] };

              for (const task of tasks) {
                const scoreEntries = playerSpecificScores.filter(score => score.task_id === task.id);
                const taskAttemptsData = scoreEntries.map((scoreEntry, index) => ({
                  attemptNumber: index + 1,
                  score: scoreEntry.score,
                  playerscoreId: scoreEntry.id,
                  createdAt: scoreEntry.created_at
                }));

                const latestAttempt = taskAttemptsData.reduce((latest, attempt) => {
                  return new Date(attempt.createdAt) > new Date(latest.createdAt) ? attempt : latest;
                }, { score: 0, createdAt: 0 });

                lessonData.totalPoints += Math.min(latestAttempt.score, task.points);
                lessonData.maxPoints += task.points;
                lessonData.tasks.push({
                  ...task,
                  latestScore: latestAttempt.score,
                  attempts: taskAttemptsData.length,
                  attemptsData: taskAttemptsData
                });
              }

              lessonData.tasks = lessonData.tasks.filter(task => task.attempts > 0);
              if (lessonData.tasks.length > 0) {
                chapterData.totalPoints += lessonData.totalPoints;
                chapterData.maxPoints += lessonData.maxPoints;
                chapterData.lessons.push(lessonData);
              }
            }

            if (chapterData.lessons.length > 0) {
              courseData.totalPoints += chapterData.totalPoints;
              courseData.maxPoints += chapterData.maxPoints;
              courseData.chapters.push(chapterData);
            }
          }

          if (courseData.chapters.length > 0) {
            playerCourses.push(courseData);
            playerTotalPoints += courseData.totalPoints;
            playerMaxPoints += courseData.maxPoints;
          }
        }

        return {
          id: player.id,
          user_id: player.user_id,
          name: player.name,
          totalPoints: playerTotalPoints,
          maxPoints: playerMaxPoints,
          courses: playerCourses
        };
      }));
    }

  }
};
</script>

<style scoped>

.player-item:hover,
.course-item:hover,
.chapter-item:hover,
.lesson-item:hover,
.task-item:hover {
  /* background-color: #e5e7eb !important;
   color: black !important;
   border: 1px solid #9a9b9e !important; */
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.5) !important;
}

.player-item.cursor-not-allowed {
  cursor: not-allowed !important;
  /* opacity: 0.5; */
  /* box-shadow: none !important; */
}
</style>