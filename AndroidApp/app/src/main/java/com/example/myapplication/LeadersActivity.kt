package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import checkConnectivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException

class LeadersActivity : AppCompatActivity() {

    private lateinit var leadersTable: TableLayout
    private lateinit var progressBar: ProgressBar
    private var leadersList: List<Leader> = emptyList()
    private var userDetails: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaders)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        progressBar = findViewById(R.id.progressBar)
        leadersTable = findViewById(R.id.leaders_table)

        progressBar.visibility = ProgressBar.VISIBLE
        leadersTable.visibility = TableLayout.GONE

        val userName = sharedPreferences.getString("userName", "")
        val userLevel = sharedPreferences.getInt("userLevel", 0)
        val userXp = sharedPreferences.getInt("userXp", 0)

        if (userName != null && userLevel != 0 && userXp != 0) {
            val levelTextView = findViewById<TextView>(R.id.level_text)
            levelTextView.text = "Level: $userLevel"

            val xpTextView = findViewById<TextView>(R.id.xp_text)
            xpTextView.text = "XP: $userXp"

            val welcomeMessage = findViewById<TextView>(R.id.welcome_message)
            welcomeMessage.apply {
                text = "Welcome back $userName!"
            }
        }


        if (savedInstanceState != null) {
            val leadersJson = savedInstanceState.getString("leadersList", "[]")
            leadersList = Gson().fromJson(leadersJson, object : TypeToken<List<Leader>>() {}.type)
            userDetails = savedInstanceState.getString("userDetails", "")
            if (leadersList.isNotEmpty()) {
                displayLeaders()
            }
        } else {
            lifecycleScope.launch {
                val accessToken = TokenManager.getAccessToken(sharedPreferences)

                if (accessToken != null) {
                    fetchUserDetails(accessToken)
                    fetchLeaders(accessToken)
                } else {
                    handleTokenInvalidation()
                }
            }
        }


        val logoutIcon = findViewById<ImageView>(R.id.logout_icon)
        logoutIcon.setOnClickListener {
            TokenManager.clearTokens(sharedPreferences)
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        /*
        val goToCoursesdButton = findViewById<ImageView>(R.id.go_to_courses_button)
        goToCoursesdButton.setOnClickListener {
            val intent = Intent(this, CoursesActivity::class.java)
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

        bottomNavigationView.selectedItemId = R.id.leaders

        bottomNavigationView.menu.findItem(R.id.leaders).isEnabled = false

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

    private suspend fun fetchUserDetails(accessToken: String) {
        try {
            val authHeader = "Bearer $accessToken"
            val response = RetrofitClient.apiService.getFullUserParameters(authHeader)

            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("userName", response.name)
                response.performance.level?.let { putInt("userLevel", it) }
                response.performance.xp?.let { putInt("userXp", it) }
                apply()
            }

            val levelTextView = findViewById<TextView>(R.id.level_text)
            levelTextView.text = "Level: ${response.performance.level ?: "N/A"}"

            val xpTextView = findViewById<TextView>(R.id.xp_text)
            xpTextView.text = "XP: ${response.performance.xp ?: "N/A"}"

            val welcomeMessage = findViewById<TextView>(R.id.welcome_message)
            welcomeMessage.apply {
                text = "Welcome back ${response.name}!"
            }
        } catch (e: HttpException) {
            handleHttpException(e)
        } catch (e: Exception) {
            Log.e("LeadersActivity", "Error while fetching user details", e)
            Toast.makeText(this@LeadersActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun addHeadersRow() {
        val headerRow = TableRow(this)

        val headers = listOf("Pos", "Nickname", "XP", "Country", "Level", "Groups")
        headers.forEach { header ->
            val headerTextView = TextView(this)
            headerTextView.text = header
            headerTextView.textSize = 18f
            headerTextView.setPadding(8, 8, 8, 8)
            headerTextView.setTextColor(Color.WHITE)
            headerTextView.setBackgroundColor(Color.parseColor("#6200EE"))
            headerTextView.gravity = android.view.Gravity.CENTER
            headerRow.addView(headerTextView)
        }
        leadersTable.addView(headerRow)
    }

    private fun addLeaderRow(position: Int, leader: Leader) {
        val row = TableRow(this)

        val params = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        row.layoutParams = params

        val positionParams = TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.1f
        }

        val xpParams = TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.15f
        }

        val lvlParams = TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.15f
        }

        val textParams = TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.2f
        }

        val nickParams = TableRow.LayoutParams(
            0,
            TableRow.LayoutParams.WRAP_CONTENT
        ).apply {
            weight = 0.3f
        }

        val commonPadding = 8

        val positionTextView = TextView(this).apply {
            text = position.toString()
            setPadding(commonPadding, commonPadding, commonPadding, commonPadding)
            layoutParams = positionParams
            gravity = android.view.Gravity.CENTER
        }
        row.addView(positionTextView)

        val nicknameTextView = TextView(this).apply {
            text = leader.nickname.ifEmpty { "No nickname" }
            setPadding(commonPadding, commonPadding, commonPadding, commonPadding)
            layoutParams = nickParams
            gravity = android.view.Gravity.CENTER
        }
        row.addView(nicknameTextView)

        val xpTextView = TextView(this).apply {
            text = leader.xp.ifEmpty { "0" }
            setPadding(commonPadding, commonPadding, commonPadding, commonPadding)
            layoutParams = xpParams
            gravity = android.view.Gravity.CENTER
        }
        row.addView(xpTextView)

        val countryTextView = TextView(this).apply {
            text = leader.country.ifEmpty { "Unknown" }
            setPadding(commonPadding, commonPadding, commonPadding, commonPadding)
            layoutParams = textParams
            gravity = android.view.Gravity.CENTER
        }
        row.addView(countryTextView)

        val levelTextView = TextView(this).apply {
            text = leader.level_id.toString()
            setPadding(commonPadding, commonPadding, commonPadding, commonPadding)
            layoutParams = lvlParams
            gravity = android.view.Gravity.CENTER
        }
        row.addView(levelTextView)

        val groupsTextView = TextView(this).apply {
            text = leader.groups.ifEmpty { "No group" }
            setPadding(commonPadding, commonPadding, commonPadding, commonPadding)
            layoutParams = textParams
            gravity = android.view.Gravity.CENTER
        }
        row.addView(groupsTextView)

        leadersTable.addView(row)
    }

    /*
    private fun displayLeaders() {
        if (leadersTable.childCount == 0) {
            addHeadersRow()
        }
        leadersList.forEachIndexed { index, leader ->
            addLeaderRow(index + 1, leader)
        }
        progressBar.visibility = ProgressBar.GONE
        leadersTable.visibility = TableLayout.VISIBLE
    }
    */

    private fun displayLeaders() {
        while (leadersTable.childCount > 1) {
            leadersTable.removeViewAt(1)
        }

        if (leadersTable.childCount == 0) {
            addHeadersRow()
        }

        leadersList.forEachIndexed { index, leader ->
            addLeaderRow(index + 1, leader)
        }
        progressBar.visibility = ProgressBar.GONE
        leadersTable.visibility = TableLayout.VISIBLE
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
        if (e.code() == 405) {
            Log.e("LeadersActivity", "HTTP 405: Method Not Allowed", e)
            Toast.makeText(this, "HTTP 405: Method Not Allowed", Toast.LENGTH_SHORT).show()
        } else if (e.code() == 401) {
            Log.e("LeadersActivity", "HTTP error occurred", e)
            Toast.makeText(this, "HTTP error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            handleTokenInvalidation()
        } else {
            Log.e("LeadersActivity", "HTTP error occurred", e)
            Toast.makeText(this, "HTTP error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun fetchLeaders(accessToken: String) {
        try {
            val authHeader = "Bearer $accessToken"

            val requestBody = JsonObject().apply {
                addProperty("areas", "")
                addProperty("courses", "")
                addProperty("countries", "")
                addProperty("groups", "")
                addProperty("start", 0)
                addProperty("end", 100)
            }

            withTimeout(30000) {
                val response = RetrofitClient.apiService.getLeaders(authHeader, requestBody)

                Log.d("LeadersActivity", "Raw API Response: $response")

                leadersList = response.list
                if (leadersList.isNotEmpty()) {
                    displayLeaders()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("LeadersActivity", "Timeout occurred while fetching leaders", e)
            Toast.makeText(
                this@LeadersActivity,
                "Request timed out. Please try again later.",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: HttpException) {
            handleHttpException(e)
        } catch (e: Exception) {
            Log.e("LeadersActivity", "Error while fetching leaders", e)
            Toast.makeText(this@LeadersActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val leadersJson = Gson().toJson(leadersList)
        outState.putString("leadersList", leadersJson)
        outState.putString("userDetails", userDetails)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val leadersJson = savedInstanceState.getString("leadersList", "[]")
        leadersList = Gson().fromJson(leadersJson, object : TypeToken<List<Leader>>() {}.type)
        userDetails = savedInstanceState.getString("userDetails", "")
        if (leadersList.isNotEmpty()) {
            displayLeaders()
        }
    }

    override fun onResume() {
        super.onResume()
        checkConnectivity()
    }

    /*
    override fun onResume() {
        super.onResume()
        checkConnectivity()

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        lifecycleScope.launch {
            val accessToken = TokenManager.getAccessToken(sharedPreferences)

            if (accessToken != null) {
                fetchUserDetails(accessToken)
                fetchLeaders(accessToken)
            } else {
                handleTokenInvalidation()
            }
        }
    }
    */

}
