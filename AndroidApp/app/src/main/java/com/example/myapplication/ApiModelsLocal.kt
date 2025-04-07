import com.google.gson.JsonElement

data class CourseLocal(
    val id: Int,
    val name: String,
    val icon: String?,
    val area_color: String?,
    val visible: Int,
    val description: String?
)

data class ChapterLocal(
    val id: Int,
    val course_id: Int,
    val name: String,
    val position: Int,
    val visible: Int
)

data class LessonLocal(
    val id: Int,
    val chapter_id: Int,
    val name: String,
    val position: Int,
    val visible: Int
)

data class TaskLocal(
    val id: Int,
    val lesson_id: Int,
    val name: String,
    val type: String,
    val data: JsonElement?,
    val position: Int,
    val visible: Int
)

data class CountResponse(
    val count: Int
)

data class ImageResponse(
    val image: String
)

data class PlayerLocal(
    val id: Int?,
    val user_id: Int,
    val name: String,
    val email: String,
    val score: Int
)

data class PlayerScoreLocal(
    val player_id: Int,
    val task_id: Int,
    val score: Double,
    val attempt_count: Int
)