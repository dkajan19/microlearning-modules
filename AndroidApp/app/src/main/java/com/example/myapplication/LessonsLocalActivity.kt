package com.example.myapplication

import ApiServiceLocal
import LessonLocal
import PlayerLocal
import TaskLocal
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.forEachIndexed
import androidx.core.view.iterator
import androidx.lifecycle.lifecycleScope
import calculateMatchingDefinitionsScore
import checkConnectivity
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import displayMatchingDefinitions
import isConnected
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.await

class LessonsLocalActivity : AppCompatActivity() {

    private lateinit var apiServiceLocal: ApiServiceLocal
    private var lessons = listOf<LessonLocal>()
    private var tasks = listOf<TaskLocal>()
    private var currentLessonIndex = 0
    private var currentTaskIndex = 0

    private lateinit var taskButtonsContainer: FlexboxLayout
    private lateinit var taskJsonOutput: TextView
    private lateinit var prevTaskButton: ImageButton
    private lateinit var nextTaskButton: ImageButton
    private lateinit var taskNavButtons: LinearLayout
    private lateinit var taskContainer: LinearLayout


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lessons_local)

        val mainContentTask: LinearLayout = findViewById(R.id.main_content_task)
        mainContentTask.visibility = View.INVISIBLE

        taskContainer = findViewById(R.id.taskContainer)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

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

        fetchUserDetails()


        val chapterId = intent.getIntExtra("CHAPTER_ID", -1)
        if (chapterId == -1) {
            Log.e("LessonsLocalActivity", "Neplatné chapterId: $chapterId")
            return
        }

        apiServiceLocal = RetrofitClientLocal.instance

        taskButtonsContainer = findViewById(R.id.task_buttons_container)
        taskJsonOutput = findViewById(R.id.task_json_output)
        prevTaskButton = findViewById(R.id.prev_task_button)
        nextTaskButton = findViewById(R.id.next_task_button)
        taskNavButtons = findViewById(R.id.task_nav_buttons)

        fetchLessons(chapterId)

        prevTaskButton.setOnClickListener {
            Log.d(
                "LessonsActivity",
                "prevTaskButton clicked. currentLessonIndex: $currentLessonIndex, currentTaskIndex: $currentTaskIndex, tasks.size: ${tasks.size}"
            )

            if (currentTaskIndex > 0) {
                currentTaskIndex--
                Log.d(
                    "LessonsActivity",
                    "Moving to previous task. currentTaskIndex: $currentTaskIndex"
                )
                taskContainer.removeAllViews()
                displayTask(tasks.getOrNull(currentTaskIndex))
                updateButtonColors()
            } else if (currentLessonIndex > 0) {
                currentLessonIndex--
                Log.d(
                    "LessonsActivity",
                    "Moving to previous lesson. currentLessonIndex: $currentLessonIndex"
                )

                lifecycleScope.launch {
                    try {
                        val newTasks = apiServiceLocal.getTasks(lessons[currentLessonIndex].id)
                            .await()
                            .filter { it.visible == 1 }
                            .sortedBy { it.position }

                        tasks = newTasks
                        currentTaskIndex = tasks.lastIndex

                        taskContainer.removeAllViews()
                        displayTask(tasks.getOrNull(currentTaskIndex))
                        generateTaskButtons()
                        updateButtonColors()
                    } catch (e: HttpException) {
                        handleHttpException(e)
                    } catch (e: Exception) {
                        handleUnexpectedException(e)
                    }
                }
            }
        }

        nextTaskButton.setOnClickListener {
            if (currentTaskIndex < tasks.size - 1) {
                currentTaskIndex++
                taskContainer.removeAllViews()
                displayTask(tasks.getOrNull(currentTaskIndex))
                updateButtonColors()
            } else if (currentLessonIndex < lessons.size - 1) {
                currentLessonIndex++

                lifecycleScope.launch {
                    try {
                        tasks = apiServiceLocal.getTasks(lessons[currentLessonIndex].id)
                            .await()
                            .filter { it.visible == 1 }
                            .sortedBy { it.position }

                        currentTaskIndex = 0

                        if (tasks.isNotEmpty()) {
                            taskContainer.removeAllViews()
                            displayTask(tasks.getOrNull(currentTaskIndex))
                            generateTaskButtons()
                            updateButtonColors()
                        } else {
                            taskContainer.removeAllViews()
                            showNoTasksMessage()
                            showTaskNavigation()
                            updateButtonColors()
                        }
                    } catch (e: HttpException) {
                        handleHttpException(e)
                    } catch (e: Exception) {
                        handleUnexpectedException(e)
                    }
                }
            } else {
                Toast.makeText(this, "You've reached the end of the chapter.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }


        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 3000
            fillAfter = true
        }

        mainContentTask.startAnimation(fadeIn)

        mainContentTask.visibility = View.VISIBLE

    }


    private fun updateButtonColors() {
        if (!isConnected()) {
            val mainContentTask: LinearLayout = findViewById(R.id.main_content_task)
            mainContentTask.visibility = View.GONE
            val taskNav: LinearLayout = findViewById(R.id.task_nav)
            taskNav.visibility = View.GONE
        }
        checkConnectivity()

        val borderWidth = 10

        taskButtonsContainer.forEachIndexed { index, view ->
            if (view is MaterialButton) {
                view.strokeWidth = borderWidth
                view.strokeColor = ColorStateList.valueOf(
                    if (index == currentTaskIndex)
                        ContextCompat.getColor(this@LessonsLocalActivity, R.color.red)
                    else
                        Color.TRANSPARENT
                )
            }
        }

        val lessonsNavContainer = findViewById<FlexboxLayout>(R.id.lessons_nav)
        lessonsNavContainer.forEachIndexed { index, view ->
            if (view is MaterialButton) {
                view.strokeWidth = borderWidth
                view.strokeColor = ColorStateList.valueOf(
                    if (index == currentLessonIndex)
                        ContextCompat.getColor(this@LessonsLocalActivity, R.color.colorPrimary)
                    else
                        Color.TRANSPARENT
                )
                view.setTextColor(
                    if (index == currentLessonIndex)
                        ContextCompat.getColor(this@LessonsLocalActivity, R.color.colorPrimary)
                    else
                        Color.WHITE
                )
            }
        }
    }


    private fun fetchLessons(chapterId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiServiceLocal.getLessons(chapterId).await()
                lessons = response
                    .filter { it.visible == 1 }
                    .sortedBy { it.position }

                if (lessons.isEmpty()) {
                    showNoLessonsMessage()
                } else {
                    showLessonsUI()
                    displayLessons(lessons)
                    fetchTasksForLesson(lessons[0].id)
                }
            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                handleUnexpectedException(e)
            }
        }
    }


    private fun fetchTasksForLesson(lessonId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiServiceLocal.getTasks(lessonId).await()
                tasks = response
                    .filter { it.visible == 1 }
                    .sortedBy { it.position }

                currentTaskIndex = 0

                if (tasks.isEmpty()) {
                    showNoTasksMessage()
                    showTaskNavigation()
                    updateButtonColors()
                } else {
                    taskContainer.removeAllViews()
                    displayTask(tasks.getOrNull(currentTaskIndex))
                    generateTaskButtons()
                    showTaskNavigation()
                    updateButtonColors()
                }
            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                handleUnexpectedException(e)
            }
        }
    }


    private fun generateTaskButtons() {
        taskButtonsContainer.removeAllViews()

        val desiredHeightDp = 40f
        val desiredHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            desiredHeightDp,
            resources.displayMetrics
        ).toInt()

        val marginRightDp = 8f
        val marginRightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginRightDp,
            resources.displayMetrics
        ).toInt()

        if (tasks.isNotEmpty()) {
            tasks.forEachIndexed { index, task ->
                val taskButton = MaterialButton(this).apply {
                    text = ""
                    setOnClickListener {
                        currentTaskIndex = index
                        taskContainer.removeAllViews()
                        displayTask(tasks.getOrNull(currentTaskIndex))
                        updateButtonColors()
                    }

                    cornerRadius = 12
                    setTextColor(Color.WHITE)
                    setBackgroundColor(
                        ContextCompat.getColor(
                            this@LessonsLocalActivity,
                            R.color.colorPrimary
                        )
                    )

                    layoutParams = FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        desiredHeightPx
                    ).apply {
                        rightMargin = marginRightPx
                    }

                    minWidth = 50
                    minimumWidth = 0
                    //maxWidth = 75
                }
                taskButtonsContainer.addView(taskButton)
            }
        }
    }


    private fun displayLessons(lessons: List<LessonLocal>) {
        val lessonsNavContainer = findViewById<FlexboxLayout>(R.id.lessons_nav)
        lessonsNavContainer.removeAllViews()

        val desiredHeightDp = 40f
        val desiredHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            desiredHeightDp,
            resources.displayMetrics
        ).toInt()

        val marginRightDp = 8f
        val marginRightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginRightDp,
            resources.displayMetrics
        ).toInt()

        lessons.forEachIndexed { index, lesson ->
            val lessonButton = MaterialButton(this).apply {
                text = lesson.name
                setOnClickListener {
                    currentLessonIndex = index
                    taskButtonsContainer.removeAllViews()
                    taskJsonOutput.text = ""
                    fetchTasksForLesson(lesson.id)
                    taskContainer.removeAllViews()
                }

                cornerRadius = 12
                setTextColor(Color.WHITE)
                setBackgroundColor(ContextCompat.getColor(this@LessonsLocalActivity, R.color.red))

                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    desiredHeightPx
                ).apply {
                    rightMargin = marginRightPx
                }

                minWidth = 0
                minimumWidth = 0
            }
            lessonsNavContainer.addView(lessonButton)
        }
    }


    private fun displayTask(task: TaskLocal?) {
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 1000
            fillAfter = true
        }

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            fillAfter = true
        }

        taskContainer.startAnimation(fadeOut)

        lifecycleScope.launch {
            try {
                taskContainer.removeAllViews()

                Log.d("LessonsActivity", "displayTask: Začiatok načítavania hráča.")
                val player = getPlayerFromPreferences()
                Log.d("LessonsActivity", "displayTask: Hráč načítaný: $player")

                if (player != null) {
                    when (task?.type) {
                        "translation" -> {
                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie prekladovej úlohy. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displayTranslation: taskID: ${t.id}"
                                        )
                                        displayTranslation(cont, t, p, api, input, correct, point)
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateTranslationScore(input, correct, point)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        "matching_definitions" -> {
                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie definícií. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displayMatchingDefinitions: taskID: ${t.id}"
                                        )
                                        displayMatchingDefinitions(
                                            cont,
                                            t,
                                            p,
                                            api,
                                            input,
                                            correct,
                                            point
                                        )
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateMatchingDefinitionsScore(input, correct, point)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        "matching_images" -> {
                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie párovania obrázkov. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displayMatchingImages: taskID: ${t.id}"
                                        )
                                        displayMatchingImages(
                                            cont,
                                            t,
                                            p,
                                            api,
                                            input,
                                            correct,
                                            point
                                        )
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateMatchingImagesScore(input, correct, point)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        "categorization" -> {
                            val currentTask = task

                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie úlohy kategorizácie. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displayCategorizationTask: taskID: ${t.id}"
                                        )
                                        displayCategorizationTask(
                                            cont,
                                            t,
                                            p,
                                            api,
                                            input,
                                            correct,
                                            point
                                        )
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateCategorizationScore(currentTask, input)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        "context_choice" -> {
                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie úlohy voľby kontextu. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displayContextChoiceTask: taskID: ${t.id}"
                                        )
                                        displayContextChoiceTask(
                                            cont as LinearLayout,
                                            t,
                                            p,
                                            api,
                                            input,
                                            correct,
                                            point
                                        )
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateContextChoiceScore(input)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        "sentence_building" -> {
                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie úlohy na stavbu viet. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displaySentenceBuildingTask: taskID: ${t.id}"
                                        )
                                        displaySentenceBuildingTask(
                                            cont,
                                            t,
                                            p,
                                            api,
                                            input,
                                            correct,
                                            point
                                        )
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateSentenceBuildingScore(input, correct, point)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        "gap_filling" -> {
                            player.id?.let { playerId ->
                                Log.d(
                                    "LessonsActivity",
                                    "displayTask: Zobrazenie úlohy na dopĺňanie slov. taskID: ${task.id}"
                                )
                                displayTask(
                                    taskContainer = taskContainer,
                                    task = task,
                                    playerId = playerId,
                                    apiService = apiServiceLocal,
                                    displayFunction = { cont, t, p, api, input, correct, point ->
                                        Log.d(
                                            "LessonsActivity",
                                            "displayGapFillingTask: taskID: ${t.id}"
                                        )
                                        displayGapFillingTask(
                                            cont as LinearLayout,
                                            t,
                                            p,
                                            api,
                                            input as MutableList<View>,
                                            correct as MutableList<String>,
                                            point as MutableList<Double>
                                        )
                                    },
                                    calculateScoreFunction = { input, correct, point ->
                                        calculateGapFillingScore(input, correct, point)
                                    }
                                )
                            }
                            hideJSONui()
                        }

                        else -> {
                            Log.d(
                                "LessonsActivity",
                                "displayTask: Zobrazenie JSON úlohy. taskID: ${task?.id}"
                            )
                            showJSONui()
                            taskJsonOutput.text = task?.data?.toString() ?: "No task to display!"
                        }
                    }
                } else {
                    Log.e("LessonsActivity", "displayTask: Hráč nenájdený v SharedPreferences")
                }
            } catch (e: HttpException) {
                Log.e("LessonsActivity", "displayTask: HttpException: ${e.message}")
                handleHttpException(e)
            } catch (e: Exception) {
                Log.e("LessonsActivity", "displayTask: Exception: ${e.message}")
                handleUnexpectedException(e)
            }

            taskContainer.startAnimation(fadeIn)
        }
    }

    /*
    private fun displayTask(task: TaskLocal?) {
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 300
            fillAfter = true
        }

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 300
            fillAfter = true
        }

        taskContainer.startAnimation(fadeOut)

        taskContainer.postDelayed({
            taskContainer.removeAllViews()

            lifecycleScope.launch {
                try {
                    val player = getPlayerFromPreferences()

                    if (player != null) {
                        when (task?.type) {
                            "translation" -> {
                                player.id?.let {
                                    displayTranslation(taskContainer, task, it, apiServiceLocal)
                                }
                                hideJSONui()
                            }
                            else -> {
                                showJSONui()
                                taskJsonOutput.text = task?.data?.toString() ?: "Žiadna úloha na zobrazenie."
                            }
                        }
                    } else {
                        Log.e("LessonsLocalActivity", "Player not found in SharedPreferences")
                    }
                } catch (e: HttpException) {
                    handleHttpException(e)
                } catch (e: Exception) {
                    handleUnexpectedException(e)
                }
            }

            taskContainer.startAnimation(fadeIn)
        }, 300)
    }

    */

    private fun getPlayerFromPreferences(): PlayerLocal? {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("player_local_data", null)
        return json?.let { gson.fromJson(it, PlayerLocal::class.java) }
    }


    private fun showNoLessonsMessage() {
        val lessonsNavContainer = findViewById<FlexboxLayout>(R.id.lessons_nav)
        val noLessonsTextView = TextView(this).apply {
            text = "No lessons are available for this chapter."
            setPadding(16, 8, 16, 8)
            textSize = 18f
        }
        lessonsNavContainer.removeAllViews()
        lessonsNavContainer.addView(noLessonsTextView)
        hideTaskUI()
        val viewNav = findViewById<View>(R.id.view_nav)
        viewNav?.visibility = View.GONE
    }

    private fun showNoTasksMessage() {
        showJSONui()
        taskJsonOutput.text = "The lesson has no tasks."
        taskButtonsContainer.removeAllViews()
        hideTaskNavigation()
    }

    private fun showLessonsUI() {
        findViewById<FlexboxLayout>(R.id.task_buttons_container)?.visibility = View.VISIBLE
        findViewById<TextView>(R.id.task_json_output)?.visibility = View.VISIBLE
        showTaskNavigation()
    }

    private fun showTaskNavigation() {
        /*
        prevTaskButton.visibility = View.VISIBLE
        nextTaskButton.visibility = View.VISIBLE
        */
        taskNavButtons.visibility = View.VISIBLE
    }

    private fun hideTaskNavigation() {
        /*
        prevTaskButton.visibility = View.GONE
        nextTaskButton.visibility = View.GONE
        */
        taskNavButtons.visibility = View.GONE
    }

    private fun hideTaskUI() {
        findViewById<FlexboxLayout>(R.id.task_buttons_container)?.visibility = View.GONE
        findViewById<TextView>(R.id.task_json_output)?.visibility = View.GONE
        hideTaskNavigation()
    }

    private fun hideJSONui() {
        findViewById<TextView>(R.id.task_json_output)?.visibility = View.GONE
    }

    private fun showJSONui() {
        findViewById<TextView>(R.id.task_json_output)?.visibility = View.VISIBLE
    }

    private fun handleUnexpectedException(exception: Exception) {
        Log.e("LessonsLocalActivity", "Neočakávaná chyba: ${exception.localizedMessage}", exception)
        Toast.makeText(this, "Chyba: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
    }

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
                    this@LessonsLocalActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onBackClick(view: View) {
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (!isConnected()) {
            hideJSONui()
            hideTaskNavigation()
            val viewNav = findViewById<View>(R.id.view_nav)
            viewNav?.visibility = View.GONE
        }
        checkConnectivity()
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
