package com.example.myapplication

data class LoginRequest(
    val grant_type: String = "password",
    val client_id: String = "2",
    val client_secret: String = "iQuGUAzqc187j7IKQ94tTVJAywHCAzYBGAMTxEtr",
    val username: String,
    val password: String
)

data class Performance(
    val xp: Int?,
    val coins: Int?,
    val level: Int?
)

data class LoginResponse(
    val access_token: String?,
    val user_id: Int,
    val name: String?,
    val surname: String?,
    val email: String?,
    val role_id: Int?,
    val content_type_id: Int?,
    val theme_id: Int?,
    val theme_value: String?,
    val performance: Performance?,
    val expires_in: Long
)

data class UserDetailsResponse(
    val user_id: Int,
    val name: String,
    val surname: String,
    val email: String,
    val role_id: Int,
    val content_type_id: Int,
    val theme_id: Int,
    val theme_value: String,
    val performance: Performance,
    val list: List<Course>,
)

data class CoursesResponse(
    val list: List<Course>
)

data class Course(
    val area_color: String,
    val course_id: Int,
    val name: String,
    val description: String?,
    val score: String,
    val max_score: String,
    val passed: Int,
    val all: Int,
    val content_count: Int,
    val program_count: Int,
    val task_count: Int,
    val content_passed: Int,
    val program_passed: Int,
    val task_passed: Int,
    val progress: Int,
)

data class Leader(
    val nickname: String,
    val xp: String,
    val country: String,
    val level_id: Int,
    val groups: String
)

data class LeadersResponse(
    val count: Int,
    val list: List<Leader>
)


data class ProfileResponse(
    val name: String?,
    val surname: String?,
    val nickname: String?,
    val groups: String?,
    val yob: Int?,
    val xp: Int?,
    val level_id: Int?,
    val id: Int?,
    val email: String?,
    val pref_lang_id: Int?,
    val content_type_id: Int?,
    val country_id: Int?,
    val role_id: Int?,
    val theme_id: Int?
)

data class RegistrationDataResponse(
    val groups: List<Group>,
    val countries: List<Country>
)

data class Group(
    val id: Int,
    val group_name: String
)

data class Country(
    val id: Int,
    val iso_code_2: String,
    val country_name: String
)

data class RegistrationRequest(
    val name: String,
    val surname: String,
    val country: Int,
    val nick: String,
    val group: String,
    val age: Int,
    val content_type_id: Int,
    val theme_id: Int,
    val lang: Int
)

data class CourseChaptersResponse(
    val title: String,
    val course: Course,
    val area: Area,
    val category: Category,
    val chapter_list: List<Chapter>,
)

data class Chapter(
    val chapter_id: Int,
    val chapter_name: String,
    val chapter_order: Int,
    val tasks_nonfinished: Int,
    val tasks_finished: Int,
    val programs_nonfinished: Int,
    val programs_finished: Int
)

data class Area(val id: Int, val name: String, val area_color: String)
data class Category(val id: Int, val name: String)

data class CategoriesResponse(
    val title: String,
    val list: List<CategoryDetailed>
)

data class CategoryDetailed(
    val title: String,
    val category_id: Int,
    val sort_order: Int,
    val color: String,
    val areas: ValueWrapper,
    val courses: ValueWrapper,
    val chapters: ValueWrapper,
    val lessons: ValueWrapper,
    val codes: ValueWrapper
)

data class ValueWrapper(
    val text: String,
    val value: String
)

data class AreasResponse(
    val category: CategoryInfo,
    val areas: List<AreaDetailed>
)

data class CategoryInfo(
    val id: Int,
    val name: String
)

data class AreaDetailed(
    val id: Int,
    val area_name: String,
    val area_color: String,
    val area_icon: String?,
    val category_id: Int,
    val area_order: Int,
    val number_of_courses: Int
)

data class AreaAllCoursesResponse(
    val area: AreaInfo,
    val category: CategoryInfo,
    val list: List<CourseDetailed>
)

data class AreaInfo(
    val id: Int,
    val name: String,
    val area_color: String
)

data class CourseDetailed(
    val id: Int,
    val title: String,
    val course_order: Int,
    val description: String,
    val course_status: String?,
    val task_finished: Int,
    val program_finished: Int,
    val content_all: Int,
    val task_all: Int,
    val program_all: Int,
    val start_date: String?,
    val finish_date: String?
)

data class ActiveLessonsResponse(
    val chapter: ChapterInfo,
    val course: CourseInfo,
    val area: AreaInfo,
    val category: CategoryInfo,
    val user_course_id: Int,
    val lesson_list: List<Lesson>
)

data class ChapterInfo(
    val id: Int,
    val name: String
)

data class CourseInfo(
    val id: Int,
    val name: String
)

data class Lesson(
    val lesson_order: Int,
    val lesson_id: Int,
    val lesson_name: String,
    val tasks_nonfinished: Int,
    val tasks_finished: Int,
    val programs_nonfinished: Int,
    val programs_finished: Int
)

data class ActiveTasksResponse(
    val course_id: String,
    val user_course_id: Int,
    val chapter_id: String,
    val lesson_id: String,
    val task_list: List<Task>
)

data class Task(
    val task_id: Int,
    val task_type_id: Int,
    val score: Int,
    val max_score: Int,
    val first_time: Int,
    val passed: Int,
    val help_showed: Int,
    val answer_showed: Int,
    val task_order: Int,
    val discuss_count: Int,
    val globals: String,
    val content: String,
    val start_time: String?,
    val end_time: String?,
    val answer: String,
    val comment: String?,
    val clarity: Int,
    val difficulty: Int
)

data class TaskEvaluationResponse(
    val result: TaskEvaluationResult,
    val user: UserPerformance
)

data class TaskEvaluationResult(
    val rating: Int,
    val answers: List<AnswerEvaluation>
)

data class AnswerEvaluation(
    val answer: String,
    val feedback: String,
    val rating: Int
)

data class UserPerformance(
    val xp: Int,
    val coins: Int,
    val level: Int,
    val badges: List<String>
)

data class CourseEnrollmentResponse(
    val course_id: String,
    val course_status: String,
    val finish_date: String?,
    val id: Int,
    val max_score: Int,
    val programs_finished: Int,
    val start_date: StartDate,
    val date: String,
    val timezone: String,
    val timezone_type: Int,
    val tasks_finished: Int,
    val user_id: Int
)

data class StartDate(
    val date: String,
    val timezone_type: Int,
    val timezone: String
)