package com.example.myapplication

import ApiServiceLocal
import CourseLocal
import PlayerLocal
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import checkConnectivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import isConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.await

class CoursesActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var apiServiceLocal: ApiServiceLocal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses)

        val coursesContainer = findViewById<LinearLayout>(R.id.courses_container)
        coursesContainer.visibility = View.INVISIBLE

        apiServiceLocal = RetrofitClientLocal.instance

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val logoutIcon = findViewById<ImageView>(R.id.logout_icon)
        logoutIcon.setOnClickListener {
            TokenManager.clearTokens(sharedPreferences)
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        /*
        val goToLeaderboardButton = findViewById<ImageView>(R.id.go_to_leaderboard_button)
        goToLeaderboardButton.setOnClickListener {
            val intent = Intent(this, LeadersActivity::class.java)
            startActivity(intent)
            //finish()
        }
        */

        val goToProfileButton = findViewById<ImageView>(R.id.profile_icon)
        goToProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            //finish()
        }

        swipeRefreshLayout.setOnRefreshListener {
            checkConnectivity()
            refreshData()
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.white, R.color.white)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.courses

        bottomNavigationView.menu.findItem(R.id.courses).isEnabled = false

        bottomNavigationView.menu.findItem(R.id.leaders).isEnabled = true

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.courses -> {
                }

                R.id.allcourses -> {
                    val intent = Intent(this, AllCoursesActivity::class.java)
                    startActivity(intent)
                }

                R.id.leaders -> {
                    val intent = Intent(this, LeadersActivity::class.java)
                    startActivity(intent)
                }

                else -> false
            }
            true
        }

        checkConnectivity()

        /*
        val splashDialog = SplashDialogFragment()
        splashDialog.show(supportFragmentManager, "splash")
        */
    }

    override fun onResume() {
        super.onResume()
        checkConnectivity()
        refreshData()
    }

    private fun refreshData() {
        if (!isConnected()) {
            Toast.makeText(
                this@CoursesActivity,
                "No internet connection.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val apiServiceLocal = RetrofitClientLocal.instance

        lifecycleScope.launch {
            try {
                val response: List<PlayerLocal> = apiServiceLocal.getPlayers().await()

                if (response != null) {
                    val players = response
                    checkIfPlayerExists()
                } else {
                    Log.e("CoursesActivity", "Žiadni hráči v odpovedi.")
                }

            } catch (e: Exception) {
                Log.e("CoursesActivity", "Chyba požiadavky: ${e.localizedMessage}")
            }
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.courses

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        lifecycleScope.launch {
            val accessToken = TokenManager.getAccessToken(sharedPreferences)

            if (accessToken != null) {
                val coursesContainer = findViewById<LinearLayout>(R.id.courses_container)
                coursesContainer.removeAllViews()

                fetchUserDetails(accessToken)
                fetchActiveCourses(accessToken)
            } else {
                handleTokenInvalidation()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun fetchUserDetails(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getFullUserParameters(authHeader)

                findViewById<TextView>(R.id.level_text).text =
                    "Level: ${response.performance.level}"
                findViewById<TextView>(R.id.xp_text).text = "XP: ${response.performance.xp}"

                findViewById<TextView>(R.id.welcome_message).apply {
                    text = "Welcome back ${response.name}!"
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CoursesActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkIfPlayerExists() {
        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val accessToken = TokenManager.getAccessToken(sharedPreferences)

                if (accessToken != null) {
                    val authHeader = "Bearer $accessToken"
                    val userDetails = RetrofitClient.apiService.getFullUserParameters(authHeader)

                    val response = apiServiceLocal.getPlayers().await()
                    val existingPlayer = response.find { it.user_id == userDetails.user_id }

                    if (existingPlayer == null) {
                        createPlayer(userDetails)
                    } else {
                        savePlayerToPreferences(existingPlayer)
                        Toast.makeText(
                            this@CoursesActivity,
                            "Player already exists",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: HttpException) {
                handleHttpException(e)
            }
        }
    }

    private fun createPlayer(userDetails: UserDetailsResponse) {
        lifecycleScope.launch {
            try {
                val newPlayer = PlayerLocal(
                    id = null,
                    user_id = userDetails.user_id,
                    name = "${userDetails.name} ${userDetails.surname}",
                    email = userDetails.email,
                    score = 0
                )

                apiServiceLocal.createPlayer(newPlayer).await()

                savePlayerToPreferences(newPlayer)

                Toast.makeText(
                    this@CoursesActivity,
                    "Player created successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: HttpException) {
                handleHttpException(e)
            }
        }
    }

    private fun savePlayerToPreferences(player: PlayerLocal) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val json = gson.toJson(player)
        editor.putString("player_local_data", json)
        editor.apply()
    }

    private fun getPlayerFromPreferences(): PlayerLocal? {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("player_local_data", null)
        return json?.let { gson.fromJson(it, PlayerLocal::class.java) }
    }


    private fun fetchActiveCourses(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"

                val response = RetrofitClient.apiService.getActiveUserCourses(authHeader)
                val remoteCourses = response.list ?: emptyList()

                var localCourses = emptyList<CourseLocal>()

                try {
                    val localCoursesResponse = withContext(Dispatchers.IO) {
                        RetrofitClientLocal.instance.getCourses().execute()
                    }
                    localCourses = localCoursesResponse.body() ?: emptyList()
                } catch (e: Exception) {
                    Log.d(
                        "CoursesActivity",
                        "Nepodarilo sa načítať lokálne kurzy: ${e.localizedMessage}"
                    )
                }

                val coursesContainer = findViewById<LinearLayout>(R.id.courses_container)
                coursesContainer.removeAllViews()

                for (localCourse in localCourses) {

                    if (localCourse.visible != 1) continue

                    val totalTaskCount =
                        withContext(Dispatchers.IO) { calculateTaskCount(localCourse.id) }

                    val player = getPlayerFromPreferences()
                    val taskPassed = if (player != null) {
                        withContext(Dispatchers.IO) {
                            calculateTaskPassed(
                                player.id,
                                localCourse.id
                            )
                        }
                    } else {
                        0
                    }

                    val progress = if (totalTaskCount > 0) {
                        ((taskPassed.toFloat() / totalTaskCount) * 100).toInt()
                    } else {
                        0
                    }

                    val course = Course(
                        course_id = localCourse.id,
                        name = localCourse.name,
                        area_color = localCourse.area_color ?: "",
                        description = localCourse.description,
                        score = "0",
                        max_score = "0",
                        passed = 0,
                        all = 0,
                        content_count = 0,
                        program_count = 0,
                        task_count = totalTaskCount,
                        content_passed = 0,
                        program_passed = 0,
                        task_passed = taskPassed,
                        progress = progress
                    )

                    val courseBubble =
                        CourseBubbleUtil.createCourseBubble(course, this@CoursesActivity)
                    courseBubble.alpha = 0f
                    courseBubble.setOnClickListener {
                        val intent =
                            Intent(this@CoursesActivity, ChaptersLocalActivity::class.java).apply {
                                putExtra("COURSE_ID", localCourse.id)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        startActivity(intent)
                    }
                    coursesContainer.addView(courseBubble)
                }

                val visibleLocalCourses = localCourses.filter { it.visible == 1 }

                if (visibleLocalCourses.isNotEmpty() && remoteCourses.isNotEmpty()) {
                    val separatorView = View(this@CoursesActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            10
                        ).also { params ->
                            params.setMargins(0, 16, 0, 16)
                        }
                        setBackgroundColor(ContextCompat.getColor(context, R.color.priscilla))
                    }
                    coursesContainer.addView(separatorView)
                }

                for (course in remoteCourses) {
                    val courseBubble =
                        CourseBubbleUtil.createCourseBubble(course, this@CoursesActivity)

                    courseBubble.setOnClickListener {
                        val intent =
                            Intent(this@CoursesActivity, ChaptersActivity::class.java).apply {
                                putExtra("COURSE_ID", course.course_id)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        startActivity(intent)
                    }

                    coursesContainer.addView(courseBubble)
                }

                coursesContainer.visibility = View.VISIBLE

                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 1000
                    addUpdateListener { animation ->
                        val alphaValue = animation.animatedValue as Float
                        for (i in 0 until coursesContainer.childCount) {
                            val child = coursesContainer.getChildAt(i)
                            child.alpha = alphaValue
                        }
                    }
                }
                animator.start()

            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                Log.d("TEST", "TEST", e)
                Toast.makeText(
                    this@CoursesActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun calculateTaskCount(courseId: Int): Int {
        var totalTaskCount = 0

        return withContext(Dispatchers.IO) {
            try {
                val chaptersResponse = RetrofitClientLocal.instance.getChapters(courseId).execute()
                val chapters = chaptersResponse.body()?.filter { it.visible != 0 } ?: emptyList()

                for (chapter in chapters) {
                    val lessonsResponse =
                        RetrofitClientLocal.instance.getLessons(chapter.id).execute()
                    val lessons = lessonsResponse.body()?.filter { it.visible != 0 } ?: emptyList()

                    for (lesson in lessons) {
                        val tasksResponse =
                            RetrofitClientLocal.instance.getTasks(lesson.id).execute()
                        val tasks = tasksResponse.body()?.filter { it.visible != 0 } ?: emptyList()

                        totalTaskCount += tasks.size
                    }
                }
            } catch (e: Exception) {
                Log.e("TASK_COUNT", "Error fetching task count: ${e.localizedMessage}")
            }

            totalTaskCount
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


    /*

    private fun fetchActiveCourses(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"

                val response = RetrofitClient.apiService.getActiveUserCourses(authHeader)
                val remoteCourses = response.list ?: emptyList()

                val localCoursesResponse = withContext(Dispatchers.IO) {
                    RetrofitClientLocal.instance.getCourses().execute()
                }
                val localCourses = localCoursesResponse.body() ?: emptyList()

                val coursesContainer = findViewById<LinearLayout>(R.id.courses_container)
                coursesContainer.removeAllViews()

                for (localCourse in localCourses) {
                    val course = Course(
                        course_id = localCourse.id,
                        name = localCourse.name,
                        area_color = localCourse.areaColor ?: "",
                        description = null,
                        score = "0",
                        max_score = "0",
                        passed = 0,
                        all = 0,
                        content_count = 0,
                        program_count = 0,
                        task_count = 0,
                        content_passed = 0,
                        program_passed = 0,
                        task_passed = 0,
                        progress = 0
                    )

                    val courseBubble = CourseBubbleUtil.createCourseBubble(course, this@CoursesActivity)

                    courseBubble.alpha = 0.6f

                    coursesContainer.addView(courseBubble)
                }

                if (localCourses.isNotEmpty() && remoteCourses.isNotEmpty()) {
                    val separatorView = View(this@CoursesActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            5
                        ).also { params ->
                            params.setMargins(0, 16, 0, 16)
                        }
                        setBackgroundColor(ContextCompat.getColor(context, R.color.priscilla))
                    }
                    coursesContainer.addView(separatorView)
                }

                for (course in remoteCourses) {
                    val courseBubble = CourseBubbleUtil.createCourseBubble(course, this@CoursesActivity)

                    courseBubble.setOnClickListener {
                        val intent = Intent(this@CoursesActivity, ChaptersActivity::class.java).apply {
                            putExtra("COURSE_ID", course.course_id)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }

                    coursesContainer.addView(courseBubble)
                }

                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 1000
                    addUpdateListener { animation ->
                        coursesContainer.alpha = animation.animatedValue as Float
                    }
                }
                animator.start()

                coursesContainer.visibility = View.VISIBLE

            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                Log.d("TEST", "TEST", e)
                Toast.makeText(this@CoursesActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }


     */

    /*

    private fun fetchActiveCourses(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getActiveUserCourses(authHeader)

                val coursesContainer = findViewById<LinearLayout>(R.id.courses_container)
                coursesContainer.removeAllViews()

                for (course in response.list) {
                    val courseBubble = CourseBubbleUtil.createCourseBubble(course, this@CoursesActivity)

                    courseBubble.setOnClickListener {
                        val intent = Intent(this@CoursesActivity, ChaptersActivity::class.java).apply {
                            putExtra("COURSE_ID", course.course_id)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }



                    coursesContainer.addView(courseBubble)

                    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 1000
                        addUpdateListener { animation ->
                            val alpha = animation.animatedValue as Float
                            coursesContainer.alpha = alpha
                        }
                    }
                    animator.start()

                    coursesContainer.visibility = View.VISIBLE
                }

            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                Toast.makeText(this@CoursesActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
     */


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
