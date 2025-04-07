package com.example.myapplication

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import checkConnectivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChaptersActivity : AppCompatActivity() {

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
            checkConnectivity()
            refreshData()
        }

        swipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.white, R.color.white)

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
            finish()
        }
         */
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

        checkConnectivity()
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


    private fun refreshData() {

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        lifecycleScope.launch {
            val accessToken = TokenManager.getAccessToken(sharedPreferences)

            if (accessToken != null) {
                val containerMain = findViewById<LinearLayout>(R.id.main_content) ?: return@launch
                containerMain.visibility = View.INVISIBLE

                val container = findViewById<LinearLayout>(R.id.chapters_container)
                container?.removeAllViews()

                fetchCourseDetails(accessToken, intent.getIntExtra("COURSE_ID", -1))
                fetchUserDetails(accessToken)
                fetchActiveUserCourses(accessToken)
            } else {
                handleTokenInvalidation()
            }

            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun fetchCourseDetails(accessToken: String, courseId: Int) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getCourseChapters(authHeader, courseId)

                val container = findViewById<LinearLayout>(R.id.chapters_container) ?: return@launch
                container.removeAllViews()

                createCourseInfoLayout(response)

                val courseColor = Color.parseColor(response.area.area_color)
                setIconsTintColor(courseColor)

                ChapterBubbleUtil.createChapterBubbles(
                    response.chapter_list,
                    this@ChaptersActivity,
                    response.area.area_color,
                    false
                )

                val totalChaptersTextView = findViewById<TextView>(R.id.total_chapters)
                totalChaptersTextView.text = "${response.chapter_list.size}"


                val containerMain = findViewById<LinearLayout>(R.id.main_content) ?: return@launch

                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 3000
                    addUpdateListener { animation ->
                        val alpha = animation.animatedValue as Float
                        containerMain.alpha = alpha
                    }
                }
                animator.start()

                containerMain.visibility = View.VISIBLE


            } catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChaptersActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                    this@ChaptersActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun fetchActiveUserCourses(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getActiveUserCourses(authHeader)

                updateCourseCounts(response)

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChaptersActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateCourseCounts(response: CoursesResponse) {
        val course =
            response.list.firstOrNull { it.course_id == intent.getIntExtra("COURSE_ID", -1) }
        course?.let {
            findViewById<TextView>(R.id.content_count_text).text =
                "${it.content_passed}/${it.content_count}"
            findViewById<TextView>(R.id.program_count_text).text =
                "${it.program_passed}/${it.program_count}"
            findViewById<TextView>(R.id.task_count_text).text = "${it.task_passed}/${it.task_count}"
        }
    }


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

    private fun createCourseInfoLayout(response: CourseChaptersResponse) {
        val courseTitleTextView = findViewById<TextView>(R.id.course_title).apply {
            text = response.title
            setTextColor(Color.parseColor(response.area.area_color))
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }

        val courseCategoryTextView = findViewById<TextView>(R.id.course_category).apply {
            text = "${response.category.name}"
            textSize = 16f
            tooltipText = "${response.category.name}"
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
