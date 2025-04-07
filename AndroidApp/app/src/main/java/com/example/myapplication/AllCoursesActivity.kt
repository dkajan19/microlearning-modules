package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import checkConnectivity
import com.caverock.androidsvg.SVG
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AllCoursesActivity : AppCompatActivity() {

    private lateinit var expandableListView: ExpandableListView
    private lateinit var noDataText: TextView

    private val categoryTitles = mutableListOf<String>()
    private val categoryColors = mutableListOf<String>()
    private val categoryIds = mutableListOf<Int>()
    private val areasMap = mutableMapOf<Int, List<AreaDetailed>>()

    private var lastExpandedPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_courses)

        expandableListView = findViewById(R.id.expandable_list_view)

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

        refreshData()
        checkConnectivity()

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.allcourses

        bottomNavigationView.menu.findItem(R.id.allcourses).isEnabled = false

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
                }

                else -> false
            }
            true
        }

        expandableListView.setOnGroupExpandListener { groupPosition ->
            if (lastExpandedPosition != -1 && lastExpandedPosition != groupPosition) {
                expandableListView.collapseGroup(lastExpandedPosition)
            }
            lastExpandedPosition = groupPosition
        }
    }

    override fun onResume() {
        super.onResume()
        checkConnectivity()
        refreshData()
    }


    private fun refreshData() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.allcourses

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val accessToken = TokenManager.getAccessToken(sharedPreferences)

        if (accessToken != null) {
            fetchUserDetails(accessToken)
            fetchCategories(accessToken)
        } else {
            handleTokenInvalidation()
        }
    }

    private fun fetchUserDetails(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getFullUserParameters(authHeader)

                val userId = response.user_id
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putInt("user_id", userId)
                editor.apply()

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
                    this@AllCoursesActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val categoryDetailsMap = mutableMapOf<Int, CategoryDetailed>()


    private fun fetchCategories(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getCategories(authHeader)

                categoryTitles.clear()
                categoryColors.clear()
                categoryIds.clear()
                areasMap.clear()
                categoryDetailsMap.clear()

                response.list.forEach { category ->
                    categoryTitles.add(category.title)
                    categoryColors.add(category.color)
                    categoryIds.add(category.category_id)
                    categoryDetailsMap[category.category_id] = category
                }

                setupExpandableListView(accessToken)

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AllCoursesActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun setupExpandableListView(accessToken: String) {
        val expandableListAdapter = object : BaseExpandableListAdapter() {
            override fun getGroupCount(): Int = categoryTitles.size

            override fun getChildrenCount(groupPosition: Int): Int {
                val categoryId = categoryIds[groupPosition]
                return areasMap[categoryId]?.size ?: 0
            }

            override fun getGroup(groupPosition: Int): Any = categoryTitles[groupPosition]

            override fun getChild(groupPosition: Int, childPosition: Int): Any {
                val categoryId = categoryIds[groupPosition]
                return areasMap[categoryId]?.get(childPosition)?.area_name ?: "Neznáma oblasť"
            }

            override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

            override fun getChildId(groupPosition: Int, childPosition: Int): Long =
                (groupPosition * 100 + childPosition).toLong()

            override fun hasStableIds(): Boolean = false


            override fun getGroupView(
                groupPosition: Int,
                isExpanded: Boolean,
                convertView: View?,
                parent: ViewGroup?
            ): View {
                val layout = LinearLayout(this@AllCoursesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val textColor = Color.parseColor(categoryColors[groupPosition])
                val arrow = if (isExpanded) "▼" else "▶"

                val titleLayout = LinearLayout(this@AllCoursesActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.START
                }

                val arrowView = TextView(this@AllCoursesActivity).apply {
                    text = arrow
                    textSize = 18f
                    setTextColor(textColor)
                }

                val textView = TextView(this@AllCoursesActivity).apply {
                    text = categoryTitles[groupPosition]
                    textSize = 18f
                    setTextColor(textColor)
                    setPadding(10, 0, 0, 0)
                }

                titleLayout.addView(arrowView)
                titleLayout.addView(textView)
                layout.addView(titleLayout)

                if (!isExpanded) {
                    val categoryId = categoryIds[groupPosition]
                    categoryDetailsMap[categoryId]?.let { category ->
                        val bubbleLayout = LinearLayout(this@AllCoursesActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.START
                            setPadding(50, 10, 10, 10)
                        }

                        val bubbles = listOf(
                            "Areas" to category.areas.value,
                            "Courses" to category.courses.value,
                            "Lessons" to category.lessons.value,
                            "Programs" to category.codes.value
                        )

                        bubbles.forEach { (label, value) ->
                            val bubble = TextView(this@AllCoursesActivity).apply {
                                text = SpannableString("$label\n$value").apply {
                                    setSpan(
                                        StyleSpan(Typeface.ITALIC),
                                        0,
                                        label.length,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                    setSpan(
                                        StyleSpan(Typeface.BOLD),
                                        label.length + 1,
                                        length,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                                textSize = 14f
                                setTextColor(textColor)
                                setPadding(20, 10, 20, 10)
                                gravity = Gravity.CENTER
                                background = GradientDrawable().apply {
                                    setStroke(4, textColor)
                                    cornerRadius = 10f
                                    setColor(Color.TRANSPARENT)
                                }
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    setMargins(10, 0, 10, 20)
                                }
                            }
                            bubbleLayout.addView(bubble)
                        }
                        layout.addView(bubbleLayout)
                    }
                }

                return layout
            }


            private var expandedAreaView: LinearLayout? = null
            private var expandedCoursesContainer: LinearLayout? = null
            private var expandedArrowView: TextView? = null
            private var expandedCoursesBubble: TextView? = null

            override fun getChildView(
                groupPosition: Int,
                childPosition: Int,
                isLastChild: Boolean,
                convertView: View?,
                parent: ViewGroup?
            ): View {
                val categoryId = categoryIds[groupPosition]
                val area = areasMap[categoryId]?.get(childPosition)
                val areaColor = Color.parseColor(area?.area_color ?: "#000000")
                val hasCourses = (area?.number_of_courses ?: 0) > 0

                val iconSize = 60
                val iconMargin = 20

                val mainLayout = LinearLayout(this@AllCoursesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(100, 15, 20, 15)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val areaLayout = LinearLayout(this@AllCoursesActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(10, 0, 10, 0)
                    gravity = Gravity.CENTER_VERTICAL
                }

                val arrowView = TextView(this@AllCoursesActivity).apply {
                    text = "▶"
                    textSize = 20f
                    setTextColor(if (hasCourses) areaColor else Color.LTGRAY)
                }

                val iconView = ImageView(this@AllCoursesActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                        setMargins(iconMargin, 0, iconMargin, 0)
                    }
                }

                if (!area?.area_icon.isNullOrEmpty()) {
                    try {
                        val pureBase64Encoded = area?.area_icon!!.substringAfter("base64,")
                        val decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT)
                        val svgString = String(decodedBytes, Charsets.UTF_8)

                        val svg = SVG.getFromString(svgString)
                        val bitmap =
                            Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        svg.renderToCanvas(canvas)

                        iconView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        iconView.setImageBitmap(generateNoIcon())
                    }
                } else {
                    iconView.setImageBitmap(generateNoIcon())
                }

                val textView = TextView(this@AllCoursesActivity).apply {
                    text = area?.area_name ?: "Neznáma oblasť"
                    textSize = 17f
                    setTextColor(areaColor)
                    layoutParams =
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val coursesBubble = TextView(this@AllCoursesActivity).apply {
                    text = SpannableString("Courses\n${area?.number_of_courses ?: 0}").apply {
                        setSpan(
                            StyleSpan(Typeface.ITALIC),
                            0,
                            7,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        setSpan(
                            StyleSpan(Typeface.BOLD),
                            8,
                            length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    textSize = 14f
                    setTextColor(areaColor)
                    setPadding(20, 10, 20, 10)
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setStroke(4, areaColor)
                        cornerRadius = 10f
                        setColor(Color.TRANSPARENT)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(10, 0, 10, 0)
                    }
                }

                areaLayout.addView(arrowView)
                areaLayout.addView(iconView)
                areaLayout.addView(textView)
                areaLayout.addView(coursesBubble)
                mainLayout.addView(areaLayout)

                val coursesContainer = LinearLayout(this@AllCoursesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 5, 5, 5)
                    visibility = View.GONE
                }
                mainLayout.addView(coursesContainer)

                if (hasCourses) {
                    areaLayout.setOnClickListener {
                        val isExpanded = coursesContainer.visibility == View.VISIBLE

                        if (expandedAreaView != null && expandedAreaView != mainLayout) {
                            expandedCoursesContainer?.visibility = View.GONE
                            expandedArrowView?.text = "▶"

                            expandedCoursesBubble?.let { bubble ->
                                bubble.alpha = 0f
                                bubble.visibility = View.VISIBLE

                                ObjectAnimator.ofFloat(bubble, "alpha", 0f, 1f).apply {
                                    duration = 300
                                    start()
                                }
                            }
                        }


                        if (isExpanded) {
                            val fadeInBubble =
                                ObjectAnimator.ofFloat(coursesBubble, "alpha", 0f, 1f).apply {
                                    duration = 300
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationStart(animation: Animator) {
                                            coursesBubble.visibility = View.VISIBLE
                                        }
                                    })
                                }

                            coursesContainer.visibility = View.GONE
                            arrowView.text = "▶"

                            fadeInBubble.start()

                            expandedAreaView = null
                            expandedCoursesContainer = null
                            expandedArrowView = null
                            expandedCoursesBubble = null
                        } else {
                            fetchCoursesForArea(area!!.id, coursesContainer, areaColor)

                            arrowView.text = "▼"

                            val fadeOut =
                                ObjectAnimator.ofFloat(coursesBubble, "alpha", 1f, 0f).apply {
                                    duration = 300
                                    addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            coursesBubble.visibility = View.GONE
                                        }
                                    })
                                }

                            coursesContainer.alpha = 0f
                            coursesContainer.translationY = 30f
                            coursesContainer.visibility = View.VISIBLE

                            val fadeIn = ObjectAnimator.ofFloat(coursesContainer, "alpha", 0f, 1f)
                                .apply { duration = 300 }
                            val translateY =
                                ObjectAnimator.ofFloat(coursesContainer, "translationY", 30f, 0f)
                                    .apply { duration = 300 }

                            AnimatorSet().apply {
                                playSequentially(
                                    fadeOut,
                                    AnimatorSet().apply { playTogether(fadeIn, translateY) })
                                start()
                            }

                            expandedAreaView = mainLayout
                            expandedCoursesContainer = coursesContainer
                            expandedArrowView = arrowView
                            expandedCoursesBubble = coursesBubble
                        }
                    }
                } else {
                    areaLayout.isClickable = false
                    areaLayout.isFocusable = false
                }

                val fadeIn =
                    ObjectAnimator.ofFloat(mainLayout, "alpha", 0f, 1f).apply { duration = 600 }
                val translateY = ObjectAnimator.ofFloat(mainLayout, "translationY", 30f, 0f)
                    .apply { duration = 600 }
                AnimatorSet().apply {
                    playTogether(fadeIn, translateY)
                    start()
                }

                return mainLayout
            }


            override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false
        }

        expandableListView.setAdapter(expandableListAdapter)

        categoryIds.forEach { categoryId ->
            fetchAreasForCategory(accessToken, categoryId)
        }
    }

    private fun fetchCoursesForArea(areaId: Int, parentLayout: LinearLayout, areaColor: Int) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer ${
                    TokenManager.getAccessToken(
                        getSharedPreferences(
                            "user_prefs",
                            MODE_PRIVATE
                        )
                    )
                }"
                val response = RetrofitClient.apiService.getAreaCourses(authHeader, areaId)

                parentLayout.removeAllViews()
                response.list.forEach { course ->
                    val courseLayout = LinearLayout(this@AllCoursesActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(20, 5, 20, 5)
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val courseTitle = TextView(this@AllCoursesActivity).apply {
                        val bullet = " ● "
                        text =
                            bullet + (if (course.title.isNullOrEmpty()) "Unnamed Course" else course.title)
                        textSize = 16f
                        setTextColor(areaColor)
                        layoutParams =
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val statusText = course.course_status ?: "new"
                    val statusBubble = TextView(this@AllCoursesActivity).apply {
                        text = statusText
                        textSize = 14f
                        setPadding(15, 5, 15, 5)
                        gravity = Gravity.CENTER

                        if (statusText == "new") {
                            setTextColor(Color.WHITE)
                            background = GradientDrawable().apply {
                                cornerRadius = 10f
                                setColor(areaColor)
                            }
                        } else {
                            setTextColor(areaColor)
                            background = GradientDrawable().apply {
                                setStroke(3, areaColor)
                                cornerRadius = 10f
                                setColor(Color.WHITE)
                            }
                        }

                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(10, 0, 0, 0)
                        }
                    }

                    if (statusText == "new") {
                        courseTitle.setOnClickListener {
                            val alertDialog = AlertDialog.Builder(
                                this@AllCoursesActivity,
                                R.style.AlertDialogTheme
                            )
                                .setTitle("Confirm Enrollment")
                                .setMessage("Do you really want to enroll in the course '${course.title}'?")
                                .setPositiveButton("Yes") { dialog, which ->
                                    enrollInCourse(course.id)
                                    dialog.dismiss()
                                }
                                .setNegativeButton("No") { dialog, which ->
                                    dialog.dismiss()
                                }
                                .create()

                            alertDialog.show()

                            val messageTextView =
                                alertDialog.findViewById<TextView>(android.R.id.message)
                            messageTextView?.setTextColor(Color.BLACK)

                            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                            positiveButton.backgroundTintList = ContextCompat.getColorStateList(
                                this@AllCoursesActivity,
                                R.color.button_color_selector
                            )
                            negativeButton.backgroundTintList = ContextCompat.getColorStateList(
                                this@AllCoursesActivity,
                                R.color.button_color_selector
                            )

                            positiveButton.setTextColor(
                                ContextCompat.getColor(
                                    this@AllCoursesActivity,
                                    R.color.red
                                )
                            )
                            negativeButton.setTextColor(
                                ContextCompat.getColor(
                                    this@AllCoursesActivity,
                                    R.color.red
                                )
                            )

                        }
                    }

                    courseLayout.addView(courseTitle)
                    courseLayout.addView(statusBubble)
                    parentLayout.addView(courseLayout)
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AllCoursesActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun enrollInCourse(courseId: Int) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val allPrefs = sharedPreferences.all

        for ((key, value) in allPrefs) {
            Log.d("SharedPreferences", "$key = $value")
        }

        val accessToken = TokenManager.getAccessToken(sharedPreferences)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (accessToken != null && userId != -1) {
            lifecycleScope.launch {
                try {
                    val authHeader = "Bearer $accessToken"
                    val requestBody = mapOf(
                        "course_id" to courseId.toString(),
                        "course_status" to "opened",
                        "user_id" to userId.toString()
                    )

                    val response =
                        RetrofitClient.apiService.enrollInCourse(authHeader, courseId, requestBody)

                    if (response.isSuccessful) {
                        val courseEnrollmentResponse = response.body()

                        if (courseEnrollmentResponse != null) {
                            Log.d(
                                "CourseEnrollment",
                                "Response body: ${courseEnrollmentResponse.toString()}"
                            )

                            Toast.makeText(
                                this@AllCoursesActivity,
                                "Successfully enrolled in course ${courseEnrollmentResponse.course_id}.",
                                Toast.LENGTH_SHORT
                            ).show()

                            refreshData()
                        } else {
                            Toast.makeText(
                                this@AllCoursesActivity,
                                "Course details not found.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@AllCoursesActivity,
                            "Failed to enroll: ${response.errorBody()?.string()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        handleTokenInvalidation()
                    } else {
                        Toast.makeText(
                            this@AllCoursesActivity,
                            "HTTP error during enrollment: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AllCoursesActivity,
                        "Error during enrollment: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            if (userId == -1) {
                Toast.makeText(
                    this@AllCoursesActivity,
                    "User ID not found. Please log in again.",
                    Toast.LENGTH_SHORT
                ).show()
                handleTokenInvalidation()
            } else {
                handleTokenInvalidation()
            }
        }
    }


    private fun generateNoIcon(): Bitmap {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = Color.LTGRAY
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText("X", size / 2f, textY, textPaint)

        return bitmap
    }


    private fun fetchAreasForCategory(accessToken: String, categoryId: Int) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val response = RetrofitClient.apiService.getAreas(authHeader, categoryId)

                areasMap[categoryId] = response.areas
                (expandableListView.expandableListAdapter as? BaseExpandableListAdapter)?.notifyDataSetChanged()

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AllCoursesActivity,
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
