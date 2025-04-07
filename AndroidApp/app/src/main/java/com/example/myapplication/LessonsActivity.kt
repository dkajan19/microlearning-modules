package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import checkConnectivity
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class LessonsActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var activeLessonsResponse: ActiveLessonsResponse
    private lateinit var activeTasksResponse: ActiveTasksResponse
    private var currentLessonIndex = 0
    private var currentTaskIndex = 0
    private lateinit var authHeader: String
    private lateinit var containerMain: LinearLayout
    private lateinit var lessonsNavigationLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lessons)

        containerMain = findViewById(R.id.main_content)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        lessonsNavigationLayout = findViewById(R.id.lessons_navigation_layout)

        containerMain.visibility = View.INVISIBLE

        swipeRefreshLayout.setOnRefreshListener {
            checkConnectivity()
            refreshData()
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.white, R.color.white)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        authHeader = "Bearer ${TokenManager.getAccessToken(sharedPreferences)}"

        val lessonID = intent.getIntExtra("lessonID", 0)
        if (lessonID == 0) {
            Toast.makeText(this, "Invalid lesson ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadActiveLessons(lessonID)
    }

    private fun loadActiveLessons(lessonID: Int) {
        lifecycleScope.launch {
            try {
                val lessonsResponse =
                    RetrofitClient.apiService.getActiveLessons(authHeader, lessonID)
                activeLessonsResponse = lessonsResponse

                generateLessonNavigationButtons()

                showLesson(lessonsResponse.lesson_list[currentLessonIndex])
                loadActiveTasks(lessonsResponse.lesson_list[currentLessonIndex])
            } catch (e: Exception) {
                Toast.makeText(this@LessonsActivity, "Error loading lessons", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun generateLessonNavigationButtons() {
        if (lessonsNavigationLayout.childCount > 0) return

        val uniqueLessons = activeLessonsResponse.lesson_list.distinctBy { it.lesson_id }

        uniqueLessons.forEachIndexed { index, lesson ->
            val button = Button(this)
            button.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            button.text = lesson.lesson_name
            button.setOnClickListener {
                currentLessonIndex = index
                currentTaskIndex = 0

                showLesson(lesson)
                loadActiveTasks(lesson)

                updateButtonColors()
            }

            lessonsNavigationLayout.addView(button)
        }

        updateButtonColors()

        val params = lessonsNavigationLayout.layoutParams as LinearLayout.LayoutParams
        params.width = LinearLayout.LayoutParams.MATCH_PARENT
        lessonsNavigationLayout.layoutParams = params
    }


    private fun updateButtonColors() {
        for (i in 0 until lessonsNavigationLayout.childCount) {
            val button = lessonsNavigationLayout.getChildAt(i) as Button
            button.setBackgroundColor(Color.WHITE)
            button.setTextColor(Color.BLACK)
        }

        val currentButton = lessonsNavigationLayout.getChildAt(currentLessonIndex) as Button
        currentButton.setBackgroundColor(Color.RED)
        currentButton.setTextColor(Color.WHITE)
    }

    private fun loadActiveTasks(lesson: Lesson) {
        lifecycleScope.launch {
            try {
                val tasksResponse = RetrofitClient.apiService.getActiveTasks(
                    authHeader,
                    activeLessonsResponse.course.id,
                    activeLessonsResponse.chapter.id,
                    lesson.lesson_id
                )
                activeTasksResponse = tasksResponse
                showTasks(tasksResponse.task_list)
                containerMain.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@LessonsActivity, "Error loading tasks", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showLesson(lesson: Lesson) {
        findViewById<TextView>(R.id.lesson_name).text = lesson.lesson_name
    }

    private fun decodeBase64ToImage(base64String: String): Bitmap? {
        return try {
            val decodedString: ByteArray = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateDefaultBitmap(): Bitmap {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.GRAY
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.WHITE
        paint.textSize = 20f
        canvas.drawText("No Image", 10f, 50f, paint)
        return bitmap
    }

    private fun getImageGetter(): Html.ImageGetter {
        return Html.ImageGetter { source ->
            val base64String = source.substringAfter("base64,")
            val bitmap = decodeBase64ToImage(base64String)
            bitmap?.let {
                val drawable = BitmapDrawable(resources, it)
                drawable.setBounds(0, 0, it.width, it.height)
                drawable
            } ?: run {
                val defaultBitmap = generateDefaultBitmap()
                val defaultDrawable = BitmapDrawable(resources, defaultBitmap)
                defaultDrawable.setBounds(0, 0, defaultBitmap.width, defaultBitmap.height)
                defaultDrawable
            }
        }
    }


    private fun showTasks(tasks: List<Task>) {
        val task = tasks[currentTaskIndex]
        val taskContentTextView = findViewById<TextView>(R.id.task_content)
        val container = findViewById<LinearLayout>(R.id.main_content)

        container.removeAllViews()

        when (task.task_type_id) {
            3 -> {
                val jsonObject = JSONObject(task.content)
                val question = jsonObject.getString("content")
                val answersArray = jsonObject.getJSONArray("answer_list")

                taskContentTextView.apply {
                    text =
                        Html.fromHtml(question, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)
                    movementMethod = LinkMovementMethod.getInstance()
                }

                val radioGroup = RadioGroup(this).apply {
                    orientation = RadioGroup.VERTICAL
                }

                for (i in 0 until answersArray.length()) {
                    val answerObj = answersArray.getJSONObject(i)
                    val answerText = answerObj.getString("answer")

                    val radioButton = RadioButton(this).apply {
                        text = answerText
                        id = View.generateViewId()
                    }
                    radioGroup.addView(radioButton)
                }


                container.addView(radioGroup)

                val checkAnswerButton = Button(this).apply {
                    text = "Skontrolovať odpoveď"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        val selectedId = radioGroup.checkedRadioButtonId
                        if (selectedId != -1) {
                            val selectedRadioButton =
                                radioGroup.findViewById<RadioButton>(selectedId)
                            val selectedAnswer = selectedRadioButton.text.toString()

                            /*
                            Toast.makeText(
                                this@LessonsActivity,
                                "Vybraná odpoveď: $selectedAnswer",
                                Toast.LENGTH_SHORT
                            ).show()
                            */

                            evaluateTask(task, selectedAnswer)
                        } else {
                            Toast.makeText(
                                this@LessonsActivity,
                                "Vyberte odpoveď!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                container.addView(checkAnswerButton)
            }

            1 -> {
                taskContentTextView.apply {
                    text = Html.fromHtml(
                        task.content,
                        Html.FROM_HTML_MODE_LEGACY,
                        getImageGetter(),
                        null
                    )
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }

            else -> {
                taskContentTextView.apply {
                    text = Html.fromHtml(
                        task.content,
                        Html.FROM_HTML_MODE_LEGACY,
                        getImageGetter(),
                        null
                    )
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
    }

    private fun evaluateTask(task: Task, selectedAnswer: String) {
        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val accessToken = TokenManager.getAccessToken(sharedPreferences)

                if (accessToken != null) {
                    val requestBody = JsonObject().apply {
                        addProperty("task_id", task.task_id)
                        addProperty("task_type_id", task.task_type_id)
                        addProperty("time_length", 4)
                        addProperty("answer_list", "[\"$selectedAnswer\"]")
                        addProperty("activity_type", "chapter")
                    }

                    Log.d("Request", "Request Body: ${requestBody.toString()}")

                    val authHeader = "Bearer $accessToken"
                    val response = RetrofitClient.apiService.evaluateTask(authHeader, requestBody)

                    Log.d("Response", "API Response: $response")

                    val result = response.result
                    val rating = result.rating
                    val feedback = result.answers.firstOrNull()?.feedback ?: "No feedback"

                    Toast.makeText(
                        this@LessonsActivity,
                        "Hodnotenie: $rating, Spätná väzba: $feedback",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    handleTokenInvalidation()
                }
            } catch (e: HttpException) {
                Log.e("LessonsActivity", "Error evaluating task", e)
                val errorResponse = e.response()?.errorBody()?.string() ?: "Unknown error"
                Log.e("LessonsActivity", "Error response: $errorResponse")
                Toast.makeText(
                    this@LessonsActivity,
                    "Server error: $errorResponse",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("LessonsActivity", "Unexpected error", e)
                Toast.makeText(
                    this@LessonsActivity,
                    "Unexpected error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun handleTokenInvalidation() {
        Toast.makeText(
            this@LessonsActivity,
            "Token expired or invalid. Please login again.",
            Toast.LENGTH_LONG
        ).show()
    }


    /*

    private fun showTasks(tasks: List<Task>) {
        val task = tasks[currentTaskIndex]
        val taskContentTextView = findViewById<TextView>(R.id.task_content)
        val container = findViewById<LinearLayout>(R.id.main_content)

        container.removeAllViews()

        if (task.task_type_id == 3) {
            val jsonObject = JSONObject(task.content)
            val question = jsonObject.getString("content")
            val answersArray = jsonObject.getJSONArray("answer_list")

            taskContentTextView.apply {
                text = Html.fromHtml(question, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)
                movementMethod = LinkMovementMethod.getInstance()
            }

            val radioGroup = RadioGroup(this)
            radioGroup.orientation = RadioGroup.VERTICAL

            for (i in 0 until answersArray.length()) {
                val answerObj = answersArray.getJSONObject(i)
                val answerText = answerObj.getString("answer")

                val radioButton = RadioButton(this).apply {
                    text = answerText
                    id = View.generateViewId()
                }
                radioGroup.addView(radioButton)
            }

            container.addView(radioGroup)

            val checkAnswerButton = Button(this).apply {
                text = "Skontrolovať odpoveď"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    val selectedId = radioGroup.checkedRadioButtonId
                    if (selectedId != -1) {
                        val selectedRadioButton = radioGroup.findViewById<RadioButton>(selectedId)
                        if (selectedRadioButton != null) {
                            val selectedAnswer = selectedRadioButton.text.toString()
                            Toast.makeText(
                                this@LessonsActivity,
                                "Vybraná odpoveď: $selectedAnswer",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(this@LessonsActivity, "Nastala chyba pri získavaní odpovede!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LessonsActivity, "Vyberte odpoveď!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            container.addView(checkAnswerButton)

        } else if (task.task_type_id == 1) {
            taskContentTextView.apply {
                text = Html.fromHtml(task.content, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)
                movementMethod = LinkMovementMethod.getInstance()
            }
        } else {
            taskContentTextView.apply {
                text = Html.fromHtml(task.content, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)
                movementMethod = LinkMovementMethod.getInstance()
            }

            /*
            val rawJson = Gson().toJson(task)
            taskContentTextView.apply {
                text = rawJson
                movementMethod = LinkMovementMethod.getInstance()
             }
             */
        }
    }
    */

    /*
    private fun showTasks(tasks: List<Task>) {
        val task = tasks[currentTaskIndex]
        val taskContent = task.content


        findViewById<TextView>(R.id.task_content).apply {
            text = Html.fromHtml(taskContent, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)
            movementMethod = LinkMovementMethod.getInstance()
            Log.d("MyAppTag", "Formatted Code: ${text}")
        }
    }
     */


    /*

    private fun showTasks(tasks: List<Task>) {
        val task = tasks[currentTaskIndex]
        val taskContent = task.content

        val formattedContent = parseContent(taskContent)

        findViewById<TextView>(R.id.task_content).apply {
            text = Html.fromHtml(formattedContent, Html.FROM_HTML_MODE_LEGACY, getImageGetter(), null)
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun parseContent(content: String): String {
        return if (content.contains("\"cell_type\":\"code\"")) {
            val codePattern = "\"source\":\\[(.*?)\\]".toRegex()
            val codeMatches = codePattern.findAll(content)

            var formattedCode = ""

            codeMatches.forEach { match ->
                formattedCode += match.groupValues[1]
            }

            if (formattedCode.isEmpty()) {
                return content
            }

            Log.d("MyAppTag", "Formatted Code: $formattedCode")

            return formattedCode
        } else {
            return content
        }
    }

    */

    fun onNextTaskClick(view: View) {
        if (currentTaskIndex < activeTasksResponse.task_list.size - 1) {
            currentTaskIndex++
            showTasks(activeTasksResponse.task_list)
        } else {
            if (currentLessonIndex < activeLessonsResponse.lesson_list.size - 1) {
                currentLessonIndex++
                currentTaskIndex = 0
                loadActiveTasks(activeLessonsResponse.lesson_list[currentLessonIndex])
                showLesson(activeLessonsResponse.lesson_list[currentLessonIndex])

                updateButtonColors()
            } else {
                finish()
            }
        }
    }

    fun onPrevTaskClick(view: View) {
        if (currentTaskIndex > 0) {
            currentTaskIndex--
            showTasks(activeTasksResponse.task_list)
        } else {
            if (currentLessonIndex > 0) {
                currentLessonIndex--
                currentTaskIndex = activeTasksResponse.task_list.size - 1
                loadActiveTasks(activeLessonsResponse.lesson_list[currentLessonIndex])
                showLesson(activeLessonsResponse.lesson_list[currentLessonIndex])

                updateButtonColors()
            }
        }
    }


    fun onBackClick(view: View) {
        finish()
    }

    private fun refreshData() {
        lifecycleScope.launch {
            loadActiveLessons(intent.getIntExtra("lessonID", 0))
        }
    }

    override fun onResume() {
        super.onResume()
        checkConnectivity()
        refreshData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
