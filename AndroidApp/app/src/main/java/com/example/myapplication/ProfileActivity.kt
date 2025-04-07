package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.lifecycle.lifecycleScope
import checkConnectivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {

    private var contentTypeId: Int = -1
    private var themeId: Int = -1
    private var langId: Int = -1

    private lateinit var groupsSpinner: Spinner
    private lateinit var countriesSpinner: Spinner

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        /*
        val containerMain = findViewById<LinearLayout>(R.id.main_content)

        containerMain.visibility = View.INVISIBLE
        */

        val rootLayout = findViewById<ScrollView>(R.id.scrollView)

        val nameInput = findViewById<TextInputEditText>(R.id.name_input)
        val surnameInput = findViewById<TextInputEditText>(R.id.surname_input)
        val nicknameInput = findViewById<TextInputEditText>(R.id.nickname_input)

        rootLayout.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focusedView = currentFocus
                if (focusedView is EditText) {
                    focusedView.clearFocus()
                }

                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            false
        }

        groupsSpinner = findViewById(R.id.groups_spinner)
        countriesSpinner = findViewById(R.id.country_spinner)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        lifecycleScope.launch {
            val accessToken = TokenManager.getAccessToken(sharedPreferences)

            if (accessToken != null) {
                fetchUserDetails(accessToken)
                fetchProfileData(accessToken)
                fetchRegistrationData(accessToken)
            } else {
                handleTokenInvalidation()
            }
        }

        val logoutIcon = findViewById<ImageView>(R.id.logout_icon)
        logoutIcon.setOnClickListener {
            TokenManager.clearTokens(sharedPreferences)
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        val nameEditText = findViewById<TextInputEditText>(R.id.name_input)
        val surnameEditText = findViewById<TextInputEditText>(R.id.surname_input)
        val nickEditText = findViewById<TextInputEditText>(R.id.nickname_input)

        val yobSlider = findViewById<Slider>(R.id.yob_slider)
        val yobValueText = findViewById<TextView>(R.id.yob_value_text)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        yobSlider.valueTo = currentYear.toFloat()

        yobSlider.addOnChangeListener { slider, value, fromUser ->
            yobValueText.text = value.toInt().toString()
        }

        val editProfileButton = findViewById<Button>(R.id.edit_profile_button)
        editProfileButton.setOnClickListener {
            val selectedGroupPosition = groupsSpinner.selectedItemPosition
            val groupNames =
                groupsSpinner.adapter as? ArrayAdapter<String> ?: return@setOnClickListener
            val selectedGroupName = groupNames.getItem(selectedGroupPosition)

            val selectedCountryPosition = countriesSpinner.selectedItemPosition
            val countryIds = countriesSpinner.tag as? List<Int> ?: listOf(-1)
            val selectedCountryId =
                if (selectedCountryPosition in countryIds.indices) countryIds[selectedCountryPosition] else -1

            if (selectedGroupName != null) {
                if (selectedGroupName.isEmpty() || selectedCountryId == -1) {
                    Toast.makeText(
                        this,
                        "Please select valid group and country",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
            }

            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val nick = nickEditText.text.toString().trim()
            val yob = yobSlider.value.toInt()

            if (name.isEmpty() || surname.isEmpty() || nick.isEmpty() || yob <= 0) {
                Toast.makeText(
                    this,
                    "Please fill in all required fields correctly",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Confirm Profile Update")
                .setMessage("Are you sure you want to update your profile information?")
                .setPositiveButton("Yes") { dialog, which ->
                    val registrationRequest = RegistrationRequest(
                        name = name,
                        surname = surname,
                        country = selectedCountryId,
                        nick = nick,
                        group = selectedGroupName.toString(),
                        age = yob,
                        content_type_id = contentTypeId,
                        theme_id = themeId,
                        lang = langId
                    )

                    lifecycleScope.launch {
                        try {
                            val accessToken = TokenManager.getAccessToken(sharedPreferences)

                            if (accessToken != null) {
                                val authHeader = "Bearer $accessToken"
                                RetrofitClient.apiService.profileChange(
                                    authHeader,
                                    registrationRequest
                                )

                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                fetchProfileData(accessToken)

                                val intent =
                                    Intent(this@ProfileActivity, CoursesActivity::class.java)
                                startActivity(intent)
                                finish()

                            } else {
                                handleTokenInvalidation()
                            }

                        } catch (e: HttpException) {
                            if (e.code() == 401) {
                                handleTokenInvalidation()
                            } else {
                                Toast.makeText(
                                    this@ProfileActivity,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("No") { dialog, which -> }
                .create()

            alertDialog.show()
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_color_selector)
            negativeButton.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.button_color_selector)

            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.red))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.red))
        }

        /*
        val goToLeaderboardButton = findViewById<ImageView>(R.id.go_to_leaderboard_button)
        goToLeaderboardButton.setOnClickListener {
            val intent = Intent(this, LeadersActivity::class.java)
            startActivity(intent)
            finish()
        }

        val goToCoursesdButton = findViewById<ImageView>(R.id.go_to_courses_button)
        goToCoursesdButton.setOnClickListener {
            val intent = Intent(this, CoursesActivity::class.java)
            startActivity(intent)
            finish()
        }
        */

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
                    this@ProfileActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun fetchRegistrationData(accessToken: String) {
        try {
            val authHeader = "Bearer $accessToken"
            val response = RetrofitClient.apiService.getRegistrationData(authHeader)

            Log.d("ProfileActivity", "Registration Data Response: $response")

            val groupNames = response.groups?.map { it.group_name } ?: listOf("No Groups Available")
            val groupIds = response.groups?.map { it.id } ?: listOf(-1)

            groupsSpinner.tag = groupIds

            val groupAdapter = object :
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, groupNames) {
                override fun isEnabled(position: Int): Boolean {
                    return true
                }

                override fun getView(
                    position: Int,
                    convertView: android.view.View?,
                    parent: android.view.ViewGroup
                ): android.view.View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.setTextColor(Color.BLACK)
                    view.setBackgroundColor(Color.WHITE)
                    view.ellipsize = TextUtils.TruncateAt.END
                    view.maxLines = 1
                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: android.view.View?,
                    parent: android.view.ViewGroup
                ): android.view.View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setTextColor(Color.BLACK)
                    view.setBackgroundColor(Color.WHITE)
                    view.ellipsize = TextUtils.TruncateAt.END
                    view.maxLines = 1
                    return view
                }
            }
            groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            groupsSpinner.adapter = groupAdapter

            val countryNames =
                response.countries?.map { it.country_name } ?: listOf("No Countries Available")
            val countryIds = response.countries?.map { it.id } ?: listOf(-1)

            countriesSpinner.tag = countryIds

            val countryAdapter = object :
                ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, countryNames) {
                override fun isEnabled(position: Int): Boolean {
                    return true
                }

                override fun getView(
                    position: Int,
                    convertView: android.view.View?,
                    parent: android.view.ViewGroup
                ): android.view.View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.setTextColor(Color.BLACK)
                    view.setBackgroundColor(Color.WHITE)
                    view.ellipsize = TextUtils.TruncateAt.END
                    view.maxLines = 1
                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: android.view.View?,
                    parent: android.view.ViewGroup
                ): android.view.View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setTextColor(Color.BLACK)
                    view.setBackgroundColor(Color.WHITE)
                    view.ellipsize = TextUtils.TruncateAt.END
                    view.maxLines = 1
                    return view
                }
            }
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            countriesSpinner.adapter = countryAdapter

            response.groups?.find { it.group_name == "Default Group Name" }?.let {
                groupsSpinner.setSelection(groupNames.indexOf(it.group_name))
            }

            response.countries?.find { it.country_name == "Default Country Name" }?.let {
                countriesSpinner.setSelection(countryNames.indexOf(it.country_name))
            }

        } catch (e: HttpException) {
            if (e.code() == 401) {
                handleTokenInvalidation()
            } else {
                handleHttpException(e)
            }
        } catch (e: Exception) {
            Toast.makeText(this@ProfileActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun fetchProfileData(accessToken: String) {
        lifecycleScope.launch {
            try {
                val authHeader = "Bearer $accessToken"
                val profileResponse = RetrofitClient.apiService.getProfileData(authHeader)
                val registrationDataResponse =
                    RetrofitClient.apiService.getRegistrationData(authHeader)

                Log.d("ProfileActivity", "Profile Data Response: $profileResponse")
                Log.d("ProfileActivity", "Registration Data Response: $registrationDataResponse")

                contentTypeId = profileResponse.content_type_id ?: -1
                themeId = profileResponse.theme_id ?: -1
                langId = profileResponse.pref_lang_id ?: -1

                findViewById<TextInputEditText>(R.id.name_input).setText(
                    profileResponse.name ?: "N/A"
                )
                findViewById<TextInputEditText>(R.id.surname_input).setText(
                    profileResponse.surname ?: "N/A"
                )
                findViewById<TextInputEditText>(R.id.nickname_input).setText(
                    profileResponse.nickname ?: "N/A"
                )

                val yobSlider = findViewById<Slider>(R.id.yob_slider)
                yobSlider.value = profileResponse.yob?.toFloat() ?: 1990f

                registrationDataResponse.groups?.let { groups ->
                    val groupNames = groups.map { it.group_name }
                    profileResponse.groups?.let { groupName ->
                        val selectedGroupPosition = groupNames.indexOf(groupName)
                        if (selectedGroupPosition >= 0) {
                            groupsSpinner.setSelection(selectedGroupPosition)
                        }
                    }
                }

                registrationDataResponse.countries?.let { countries ->
                    val countryNames = countries.map { it.country_name }
                    val countryIds = countries.map { it.id }
                    profileResponse.country_id?.let { countryId ->
                        val selectedCountryPosition = countryIds.indexOf(countryId)
                        if (selectedCountryPosition >= 0) {
                            countriesSpinner.setSelection(selectedCountryPosition)
                        }
                    }
                }

                /*
                val containerMain = findViewById<LinearLayout>(R.id.main_content)

                containerMain.visibility = View.VISIBLE

                val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 3000
                    addUpdateListener { animation ->
                        val alpha = animation.animatedValue as Float
                        containerMain.alpha = alpha
                    }
                }
                animator.start()
                */


            } catch (e: HttpException) {
                if (e.code() == 401) {
                    handleTokenInvalidation()
                } else {
                    handleHttpException(e)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        checkConnectivity()

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        lifecycleScope.launch {
            val accessToken = TokenManager.getAccessToken(sharedPreferences)

            if (accessToken != null) {

                fetchUserDetails(accessToken)
                fetchProfileData(accessToken)
                fetchRegistrationData(accessToken)
            } else {
                handleTokenInvalidation()
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
