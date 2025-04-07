package com.example.myapplication

import ChapterLocal
import CourseLocal
import PlayerLocal
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import checkConnectivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.await

class ChaptersLocalActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters)

        val containerMain = findViewById<LinearLayout>(R.id.main_content)
        containerMain.visibility = View.INVISIBLE

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val courseId = intent.getIntExtra("COURSE_ID", -1)

        if (courseId == -1) {
            Toast.makeText(this, "Invalid course ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        swipeRefreshLayout.setOnRefreshListener {
            refreshData(courseId)
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.white, R.color.white)

        val logoutIcon = findViewById<ImageView>(R.id.logout_icon)
        logoutIcon.setOnClickListener {
            TokenManager.clearTokens(sharedPreferences)
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        val goToProfileButton = findViewById<ImageView>(R.id.profile_icon)
        goToProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)


        bottomNavigationView?.menu?.let {
            for (menuItem in it.iterator()) {
                menuItem.isCheckable = false
                menuItem.isChecked = false
                menuItem.isCheckable = true
            }
        }

        bottomNavigationView.menu.findItem(R.id.leaders).isEnabled = true

        bottomNavigationView.menu.findItem(R.id.courses).isEnabled = true

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.courses -> {
                    val intent = Intent(this, CoursesActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }

                R.id.leaders -> {
                    val intent = Intent(this, LeadersActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                R.id.allcourses -> {
                    val intent = Intent(this, AllCoursesActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                else -> false
            }
            true
        }

        refreshData(courseId)
    }

    override fun onResume() {
        super.onResume()

        val courseId = intent.getIntExtra("COURSE_ID", -1)
        refreshData(courseId)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun refreshData(courseId: Int) {
        lifecycleScope.launch {
            try {
                val containerMain = findViewById<LinearLayout>(R.id.main_content) ?: return@launch
                containerMain.visibility = View.INVISIBLE

                checkConnectivity()

                fetchUserDetails()

                val courseResponse = getCourseDetails(courseId)
                val chaptersResponse = getCourseChapters(courseId)

                val chapters = chaptersResponse
                    .filter { it.visible == 1 }
                    .map { chapterLocal ->
                        val player = getPlayerFromPreferences()
                        val taskPassedForChapter =
                            calculateTaskPassedForChapter(player?.id, chapterLocal.id)
                        val taskCountForChapter = calculateTaskCountForChapter(chapterLocal.id)

                        Chapter(
                            chapter_id = chapterLocal.id,
                            chapter_name = chapterLocal.name,
                            chapter_order = chapterLocal.position,
                            tasks_nonfinished = taskCountForChapter,
                            tasks_finished = taskPassedForChapter,
                            programs_nonfinished = 0,
                            programs_finished = 0
                        )
                    }


                createCourseInfoLayout(courseResponse)

                val totalChaptersTextView = findViewById<TextView>(R.id.total_chapters)
                totalChaptersTextView.text = "${chapters.size}"

                val container = findViewById<LinearLayout>(R.id.chapters_container) ?: return@launch
                container.removeAllViews()

                courseResponse.area_color?.let {
                    ChapterBubbleUtil.createChapterBubbles(
                        chapters, this@ChaptersLocalActivity,
                        it, true
                    )
                }

                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 2000
                    addUpdateListener { animation ->
                        val alpha = animation.animatedValue as Float
                        containerMain.alpha = alpha
                    }
                }
                animator.start()

                containerMain.visibility = View.VISIBLE

                swipeRefreshLayout.isRefreshing = false

                chapters.forEachIndexed { index, chapter ->
                    Log.d(
                        "CHAPTER_DATA",
                        "Chapter at index $index: name=${chapter.chapter_name}, tasks_nonfinished=${chapter.tasks_nonfinished}"
                    )
                }

            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChaptersLocalActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun calculateTaskPassedForChapter(playerId: Int?, chapterId: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClientLocal.instance.getPlayerScores(playerId).execute()
                val playerScores = response.body() ?: emptyList()

                val lessonsResponse = RetrofitClientLocal.instance.getLessons(chapterId).execute()
                val visibleLessons =
                    lessonsResponse.body()?.filter { it.visible != 0 } ?: emptyList()

                val chapterTaskIds = visibleLessons.flatMap { lesson ->
                    val tasksResponse = RetrofitClientLocal.instance.getTasks(lesson.id).execute()
                    tasksResponse.body()?.filter { it.visible != 0 }?.map { it.id } ?: emptyList()
                }.toSet()

                val uniqueTasksPassed = playerScores
                    .map { it.task_id }
                    .filter { it in chapterTaskIds }
                    .toSet()

                return@withContext uniqueTasksPassed.size
            } catch (e: Exception) {
                Log.e(
                    "TASK_PASSED_CHAPTER",
                    "Error fetching player scores for chapter: ${e.localizedMessage}"
                )
                return@withContext 0
            }
        }
    }

    /*
    private suspend fun calculateTaskPassedForChapter(playerId: Int?, chapterId: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClientLocal.instance.getPlayerScores(playerId).execute()
                val playerScores = response.body() ?: emptyList()

                val lessonsResponse = RetrofitClientLocal.instance.getLessons(chapterId).execute()
                val lessons = lessonsResponse.body() ?: emptyList()

                val chapterTaskIds = lessons.flatMap { lesson ->
                    val tasksResponse = RetrofitClientLocal.instance.getTasks(lesson.id).execute()
                    tasksResponse.body()?.map { it.id } ?: emptyList()
                }.toSet()

                val uniqueTasksPassed = playerScores
                    .map { it.task_id }
                    .filter { it in chapterTaskIds }
                    .toSet()

                return@withContext uniqueTasksPassed.size
            } catch (e: Exception) {
                Log.e("TASK_PASSED_CHAPTER", "Error fetching player scores for chapter: ${e.localizedMessage}")
                return@withContext 0
            }
        }
    }

     */

    private suspend fun calculateTaskCountForChapter(chapterId: Int): Int =
        withContext(Dispatchers.IO) {
            try {
                val lessonsResponse = RetrofitClientLocal.instance.getLessons(chapterId).execute()
                val visibleLessons = lessonsResponse.body()?.filter { it.visible != 0 }.orEmpty()

                Log.d(
                    "TASK_COUNT_CHAPTER",
                    "Visible lessons for chapter $chapterId: $visibleLessons"
                )

                val taskCounts = visibleLessons.map { lesson ->
                    async {
                        try {
                            val response =
                                RetrofitClientLocal.instance.getTasks(lesson.id).execute()
                            val visibleTaskCount =
                                response.body()?.filter { it.visible != 0 }?.count() ?: 0
                            visibleTaskCount
                        } catch (e: Exception) {
                            Log.e(
                                "TASK_COUNT_CHAPTER",
                                "Error fetching tasks for lesson ${lesson.id}: ${e.localizedMessage}"
                            )
                            0
                        }
                    }
                }

                val totalTaskCount = taskCounts.awaitAll().sum()
                Log.d(
                    "TASK_COUNT_CHAPTER",
                    "Total visible task count for chapter $chapterId: $totalTaskCount"
                )

                return@withContext totalTaskCount
            } catch (e: Exception) {
                Log.e(
                    "TASK_COUNT_CHAPTER",
                    "Error fetching lessons for chapter: ${e.localizedMessage}"
                )
                return@withContext 0
            }
        }

    /*
    private suspend fun calculateTaskCountForChapter(chapterId: Int): Int = withContext(Dispatchers.IO) {
        try {
            val lessonsResponse = RetrofitClientLocal.instance.getLessons(chapterId).execute()
            val lessons = lessonsResponse.body().orEmpty()

            Log.d("TASK_COUNT_CHAPTER", "Lessons for chapter $chapterId: $lessons")

            val taskCounts = lessons.map { lesson ->
                async {
                    try {
                        val response = RetrofitClientLocal.instance.getTaskCount(lesson.id).execute()
                        response.body()?.count ?: 0
                    } catch (e: Exception) {
                        Log.e("TASK_COUNT_CHAPTER", "Error fetching task count for lesson ${lesson.id}: ${e.localizedMessage}")
                        0
                    }
                }
            }

            val totalTaskCount = taskCounts.awaitAll().sum()
            Log.d("TASK_COUNT_CHAPTER", "Total task count for chapter $chapterId: $totalTaskCount")

            return@withContext totalTaskCount
        } catch (e: Exception) {
            Log.e("TASK_COUNT_CHAPTER", "Error fetching lessons for chapter: ${e.localizedMessage}")
            return@withContext 0
        }
    }
    */


    private fun setIconsTintColor(color: Int) {
        val icons = listOf(
            findViewById<ImageView>(R.id.lightbulb_icon),
            findViewById<ImageView>(R.id.category_icon),
            findViewById<ImageView>(R.id.chapters_count_icon),
            findViewById<ImageView>(R.id.content_count_icon),
            findViewById<ImageView>(R.id.program_count_icon),
            findViewById<ImageView>(R.id.task_count_icon),
        )

        icons.forEach { icon ->
            icon.setColorFilter(color)
        }
    }

    private suspend fun getCourseDetails(courseId: Int): CourseLocal {
        return try {
            RetrofitClientLocal.instance.getCourse(courseId).await()
        } catch (e: Exception) {
            Toast.makeText(
                this@ChaptersLocalActivity,
                "Error loading course details: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
            throw e
        }
    }

    private suspend fun getCourseChapters(courseId: Int): List<ChapterLocal> {
        return try {
            RetrofitClientLocal.instance.getChapters(courseId).await()
        } catch (e: Exception) {
            Toast.makeText(
                this@ChaptersLocalActivity,
                "Error loading chapters: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
            emptyList()
        }
    }

    private suspend fun createCourseInfoLayout(course: CourseLocal) {
        findViewById<TextView>(R.id.course_title).apply {
            text = course.name
            setTextColor(Color.parseColor(course.area_color ?: "#000000"))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }

        setIconsTintColor(Color.parseColor(course.area_color ?: "#000000"))

        findViewById<TextView>(R.id.course_category).apply {
            text = "Language courses"
            textSize = 16f
            tooltipText = "Language courses"
        }

        val courseId = intent.getIntExtra("COURSE_ID", -1)
        val taskCount = calculateTaskCount(courseId)

        val player = getPlayerFromPreferences()
        val taskPassed = if (player != null) {
            withContext(Dispatchers.IO) { calculateTaskPassed(player.id, courseId) }
        } else {
            0
        }

        findViewById<TextView>(R.id.content_count_text).text = "0/0"
        findViewById<TextView>(R.id.program_count_text).text = "0/0"
        findViewById<TextView>(R.id.task_count_text).text = "$taskPassed/$taskCount"

    }

    private fun getPlayerFromPreferences(): PlayerLocal? {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("player_local_data", null)
        return json?.let { gson.fromJson(it, PlayerLocal::class.java) }
    }

    private suspend fun calculateTaskPassed(playerId: Int?, courseId: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClientLocal.instance.getPlayerScores(playerId).execute()
                val playerScores = response.body() ?: emptyList()

                val completedTaskIds = playerScores.map { it.task_id }.toSet()

                val chaptersResponse = RetrofitClientLocal.instance.getChapters(courseId).execute()
                val visibleChapters =
                    chaptersResponse.body()?.filter { it.visible != 0 } ?: emptyList()

                val visibleLessons = visibleChapters.flatMap { chapter ->
                    val lessonsResponse =
                        RetrofitClientLocal.instance.getLessons(chapter.id).execute()
                    lessonsResponse.body()?.filter { it.visible != 0 } ?: emptyList()
                }

                val visibleTasks = visibleLessons.flatMap { lesson ->
                    val tasksResponse = RetrofitClientLocal.instance.getTasks(lesson.id).execute()
                    tasksResponse.body()?.filter { it.visible != 0 } ?: emptyList()
                }

                val visibleTaskIds = visibleTasks.map { it.id }.toSet()

                val passedVisibleTasks = completedTaskIds.intersect(visibleTaskIds)

                return@withContext passedVisibleTasks.size
            } catch (e: Exception) {
                Log.e("TASK_PASSED", "Error fetching player scores: ${e.localizedMessage}")
                return@withContext 0
            }
        }
    }


    /*

    private suspend fun calculateTaskPassed(playerId: Int?): Int {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClientLocal.instance.getPlayerScores(playerId).execute()
                val playerScores = response.body() ?: emptyList()

                val uniqueTasksPassed = playerScores.map { it.task_id }.toSet()

                return@withContext uniqueTasksPassed.size
            } catch (e: Exception) {
                Log.e("TASK_PASSED", "Error fetching player scores: ${e.localizedMessage}")
                return@withContext 0
            }
        }
    }

     */

    private suspend fun calculateTaskCount(courseId: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                val chaptersResponse = RetrofitClientLocal.instance.getChapters(courseId).execute()
                val visibleChapters =
                    chaptersResponse.body()?.filter { it.visible != 0 } ?: emptyList()

                val visibleLessons = visibleChapters.flatMap { chapter ->
                    val lessonsResponse =
                        RetrofitClientLocal.instance.getLessons(chapter.id).execute()
                    lessonsResponse.body()?.filter { it.visible != 0 } ?: emptyList()
                }

                val visibleTasks = visibleLessons.flatMap { lesson ->
                    val tasksResponse = RetrofitClientLocal.instance.getTasks(lesson.id).execute()
                    tasksResponse.body()?.filter { it.visible != 0 } ?: emptyList()
                }

                return@withContext visibleTasks.size
            } catch (e: Exception) {
                Log.e("TASK_COUNT", "Error fetching task count: ${e.localizedMessage}")
                return@withContext 0
            }
        }
    }


    /*
    private suspend fun calculateTaskCount(courseId: Int): Int {
        var totalTaskCount = 0

        return withContext(Dispatchers.IO) {
            try {
                val chaptersResponse = RetrofitClientLocal.instance.getChapters(courseId).execute()
                val chapters = chaptersResponse.body() ?: emptyList()

                for (chapter in chapters) {
                    val lessonsResponse = RetrofitClientLocal.instance.getLessons(chapter.id).execute()
                    val lessons = lessonsResponse.body() ?: emptyList()

                    for (lesson in lessons) {
                        val taskCountResponse = RetrofitClientLocal.instance.getTaskCount(lesson.id).execute()
                        totalTaskCount += taskCountResponse.body()?.count ?: 0
                    }
                }
            } catch (e: Exception) {
                Log.e("TASK_COUNT", "Error fetching task count: ${e.localizedMessage}")
            }

            totalTaskCount
        }
    }
    */


    private fun fetchUserDetails() {
        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val accessToken = TokenManager.getAccessToken(sharedPreferences)

                if (accessToken != null) {
                    val authHeader = "Bearer $accessToken"
                    val response = RetrofitClient.apiService.getFullUserParameters(authHeader)

                    findViewById<TextView>(R.id.level_text).text =
                        "Level: ${response.performance.level}"
                    findViewById<TextView>(R.id.xp_text).text = "XP: ${response.performance.xp}"
                    findViewById<TextView>(R.id.welcome_message).apply {
                        text = "Welcome back ${response.name}!"
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChaptersLocalActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleTokenInvalidation() {
        TokenManager.clearTokens(getSharedPreferences("user_prefs", MODE_PRIVATE))
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun handleHttpException(e: HttpException) {
        if (e.code() == 401) {
            handleTokenInvalidation()
        } else {
            Toast.makeText(this, "HTTP error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
