import { createRouter, createWebHistory } from "vue-router";
import CourseList from "../views/CourseList.vue";
import ChapterList from "../views/ChapterList.vue";
import LessonList from "../views/LessonList.vue";
import TaskList from "../views/TaskList.vue";
import Leaderboard from "../views/PlayersScore.vue";

const routes = [
  { path: "/", component: CourseList },
  { path: "/course/:course_id/chapters", component: ChapterList },
  { path: "/course/:course_id/chapter/:chapter_id/lessons", component: LessonList },
  { path: "/course/:course_id/chapter/:chapter_id/lesson/:lesson_id/tasks", component: TaskList },
  { path: "/playersScore", component: Leaderboard },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
