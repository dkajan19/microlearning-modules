package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val accessToken = TokenManager.getAccessToken(sharedPreferences)
        if (accessToken != null) {
            navigateToCourses()
        }

        val btnLogin = findViewById<Button>(R.id.btn_login)
        val usernameInput = findViewById<TextInputEditText>(R.id.txtInput_username)
        val passwordInput = findViewById<TextInputEditText>(R.id.txt_pass)

        btnLogin.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password, sharedPreferences)
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        val registerPrompt: TextView = findViewById(R.id.txt_register_prompt)

        val text =
            "Don't have an account? <u><a href='https://priscilla.fitped.eu/register'>Register</a></u>"
        registerPrompt.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)

        registerPrompt.movementMethod = LinkMovementMethod.getInstance()

        val rootLayout = findViewById<ConstraintLayout>(R.id.layoutLogin)

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

        val guideline = findViewById<Guideline>(R.id.guideline)

        rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootLayout.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootLayout.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            val constraintSet = ConstraintSet()
            constraintSet.clone(rootLayout)

            if (keypadHeight > screenHeight * 0.15) {
                constraintSet.setGuidelinePercent(guideline.id, 0f)
            } else {
                constraintSet.setGuidelinePercent(guideline.id, 0.15f)
            }

            constraintSet.applyTo(rootLayout)
        }


    }

    private fun navigateToCourses() {
        val intent = Intent(this, CoursesActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun loginUser(
        username: String,
        password: String,
        sharedPreferences: SharedPreferences
    ) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    grantType = "password",
                    clientId = "2",
                    clientSecret = "iQuGUAzqc187j7IKQ94tTVJAywHCAzYBGAMTxEtr",
                    username = username,
                    password = password
                )

                TokenManager.saveTokens(response, sharedPreferences)

                Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                navigateToCourses()

            } catch (e: HttpException) {
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${e.response()?.errorBody()?.string()}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}