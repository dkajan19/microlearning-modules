import retrofit2.Call
import retrofit2.http.*

interface ApiServiceLocal {

    @GET("/courses")
    fun getCourses(): Call<List<CourseLocal>>

    @POST("/courses")
    fun createCourse(@Body course: CourseLocal): Call<CourseLocal>

    @PUT("/courses/{id}")
    fun updateCourse(@Path("id") id: Int, @Body course: CourseLocal): Call<Void>

    @DELETE("/courses/{id}")
    fun deleteCourse(@Path("id") id: Int): Call<Void>

    @GET("/courses/{id}")
    fun getCourse(@Path("id") id: Int): Call<CourseLocal>

    @GET("/courses/{course_id}/chapters")
    fun getChapters(@Path("course_id") courseId: Int): Call<List<ChapterLocal>>

    @POST("/courses/{course_id}/chapters")
    fun createChapter(
        @Path("course_id") courseId: Int,
        @Body chapter: ChapterLocal
    ): Call<ChapterLocal>

    @PUT("/chapters/{id}")
    fun updateChapter(@Path("id") id: Int, @Body chapter: ChapterLocal): Call<Void>

    @DELETE("/chapters/{id}")
    fun deleteChapter(@Path("id") id: Int): Call<Void>

    @PUT("/courses/{course_id}/chapters/reorder")
    fun reorderChapters(
        @Path("course_id") courseId: Int,
        @Body orderedChapters: List<ChapterLocal>
    ): Call<Void>

    @GET("/courses/{course_id}/chapters/count")
    fun getChapterCount(@Path("course_id") courseId: Int): Call<CountResponse>

    @GET("/chapters/{chapter_id}/lessons")
    fun getLessons(@Path("chapter_id") chapterId: Int): Call<List<LessonLocal>>

    @POST("/chapters/{chapter_id}/lessons")
    fun createLesson(
        @Path("chapter_id") chapterId: Int,
        @Body lesson: LessonLocal
    ): Call<LessonLocal>

    @PUT("/lessons/{id}")
    fun updateLesson(@Path("id") id: Int, @Body lesson: LessonLocal): Call<Void>

    @DELETE("/lessons/{id}")
    fun deleteLesson(@Path("id") id: Int): Call<Void>

    @PUT("/chapters/{chapter_id}/lessons/reorder")
    fun reorderLessons(
        @Path("chapter_id") chapterId: Int,
        @Body orderedLessons: List<LessonLocal>
    ): Call<Void>

    @GET("/chapters/{chapter_id}/lessons/count")
    fun getLessonCount(@Path("chapter_id") chapterId: Int): Call<CountResponse>

    @GET("/lessons/{lesson_id}/tasks")
    fun getTasks(@Path("lesson_id") lessonId: Int): Call<List<TaskLocal>>

    @POST("/lessons/{lesson_id}/tasks")
    fun createTask(@Path("lesson_id") lessonId: Int, @Body task: TaskLocal): Call<TaskLocal>

    @PUT("/tasks/{id}")
    fun updateTask(@Path("id") id: Int, @Body task: TaskLocal): Call<Void>

    @DELETE("/tasks/{id}")
    fun deleteTask(@Path("id") id: Int): Call<Void>

    @PUT("/lessons/{lesson_id}/tasks/reorder")
    fun reorderTasks(
        @Path("lesson_id") lessonId: Int,
        @Body orderedTasks: List<TaskLocal>
    ): Call<Void>

    @GET("/lessons/{lesson_id}/tasks/count")
    fun getTaskCount(@Path("lesson_id") lessonId: Int): Call<CountResponse>

    @GET("/players")
    fun getPlayers(): Call<List<PlayerLocal>>

    @POST("/players")
    fun createPlayer(@Body player: PlayerLocal): Call<PlayerLocal>

    @POST("/player-scores")
    fun addPlayerScore(@Body playerScore: PlayerScoreLocal): Call<PlayerScoreLocal>

    @GET("/player-scores/{player_id}")
    fun getPlayerScores(@Path("player_id") playerId: Int?): Call<List<PlayerScoreLocal>>

    @Multipart
    @POST("/upload")
    fun uploadImage(@Part("image") image: String): Call<ImageResponse>
}

