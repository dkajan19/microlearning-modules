import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:3000",
});

export const getTasks = (lessonId) => {
  return api.get(`/lessons/${lessonId}/tasks`);
};

export const createTask = (lessonId, task) => {
  return api.post(`/lessons/${lessonId}/tasks`, task);
};

export const updateTask = (taskId, task) => {
  return api.put(`/tasks/${taskId}`, task);
};

export const deleteTask = (taskId) => {
  return api.delete(`/tasks/${taskId}`);
};

export const getCourses = () => {
  return api.get("/courses");
};

export const createCourse = (course) => {
  return api.post("/courses", course);
};

export const updateCourse = (courseId, course) => {
  return api.put(`/courses/${courseId}`, course);
};

export const deleteCourse = (courseId) => {
  return api.delete(`/courses/${courseId}`);
};

export const getChapters = (courseId) => {
  return api.get(`/courses/${courseId}/chapters`);
};

export const createChapter = (courseId, chapter) => {
  return api.post(`/courses/${courseId}/chapters`, chapter);
};

export const updateChapter = (chapterId, chapter) => {
  return api.put(`/chapters/${chapterId}`, chapter);
};

export const deleteChapter = (chapterId) => {
  return api.delete(`/chapters/${chapterId}`);
};

export const getLessons = (chapterId) => {
  return api.get(`/chapters/${chapterId}/lessons`);
};

export const createLesson = (chapterId, lesson) => {
  return api.post(`/chapters/${chapterId}/lessons`, lesson);
};

export const updateLesson = (lessonId, lesson) => {
  return api.put(`/lessons/${lessonId}`, lesson);
};

export const deleteLesson = (lessonId) => {
  return api.delete(`/lessons/${lessonId}`);
};

export const updateTaskOrder = (lessonId, orderedTasks) => {
  return api.put(`/lessons/${lessonId}/tasks/reorder`, { orderedTasks });
};

export const getCourse = (courseId) => {
  return api.get(`/courses/${courseId}`);
};

export const updateChapterOrder = (courseId, orderedChapters) => {
  return api.put(`/courses/${courseId}/chapters/reorder`, { orderedChapters });
};

export const updateLessonOrder = (chapterId, orderedLessons) => {
  return api.put(`/chapters/${chapterId}/lessons/reorder`, { orderedLessons });
};

export const getChapterCount = (courseId) => {
  return api.get(`/courses/${courseId}/chapters/count`);
};

export const getLessonCount = (chapterId) => {
  return api.get(`/chapters/${chapterId}/lessons/count`);
};

export const getTaskCount = (lessonId) => {
  return api.get(`/lessons/${lessonId}/tasks/count`);
};

export const getPlayers = () => {
  return api.get("/players");
};


export const createPlayer = (player) => {
  return api.post("/players", player);
};

export const addPlayerScore = (scoreData) => {
  return api.post('/player-scores', scoreData);
};

export const getPlayerScores = (playerId) => {
  return api.get(`/player-scores/${playerId}`);
};

