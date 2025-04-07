package com.example.myapplication

import ApiServiceLocal
import PlayerScoreLocal
import TaskLocal
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import displayMatchingDefinitions

fun displayTask(
    taskContainer: LinearLayout,
    task: TaskLocal,
    playerId: Int,
    apiService: ApiServiceLocal,
    displayFunction: (LinearLayout, TaskLocal, Int, ApiServiceLocal, MutableList<View>, MutableList<String>, MutableList<Double>) -> Unit,
    calculateScoreFunction: (List<View>, List<String>, List<Double>) -> Double
) {
    Log.d("TaskCreationLog", "Úloha s ID ${task.id} bola vytvorená.")
    taskContainer.removeAllViews()

    val inputFields = mutableListOf<View>()
    val correctAnswers = mutableListOf<String>()
    val pointsList = mutableListOf<Double>()

    displayFunction(
        taskContainer,
        task,
        playerId,
        apiService,
        inputFields,
        correctAnswers,
        pointsList
    )

    if (inputFields.isEmpty() && correctAnswers.isEmpty()) {
        val helperTextView = TextView(taskContainer.context).apply {
            text = "This task is currently empty. Its content is likely still in progress."
            textSize = 18f
            setTextColor(Color.GRAY)
            setPadding(16, 8, 16, 8)
        }
        taskContainer.addView(helperTextView)
        return
    }

    apiService.getPlayerScores(playerId).enqueue(object : Callback<List<PlayerScoreLocal>> {
        override fun onResponse(
            call: Call<List<PlayerScoreLocal>>,
            response: Response<List<PlayerScoreLocal>>
        ) {
            if (response.isSuccessful) {
                val playerScores = response.body() ?: emptyList()
                val attempts = playerScores.count { it.task_id == task.id }
                val bestScore = playerScores
                    .filter { it.task_id == task.id && (it.attempt_count == 1 || it.attempt_count == 2) }
                    .maxByOrNull { it.attempt_count }?.score ?: 0.0

                if (attempts < 2) {
                    showCheckButton(
                        taskContainer,
                        inputFields,
                        correctAnswers,
                        pointsList,
                        task.data?.let { JSONObject(it.toString()).getDouble("totalPoints") }
                            ?: 0.0,
                        playerId,
                        apiService,
                        task,
                        attempts,
                        calculateScoreFunction)
                }

                if (attempts > 0) {
                    displayScore(
                        taskContainer.context,
                        taskContainer,
                        bestScore,
                        task.data?.let { JSONObject(it.toString()).getDouble("totalPoints") }
                            ?: 0.0)
                }

                if (attempts >= 2) {
                    displayWarning(taskContainer.context, taskContainer)
                }
            }
        }

        override fun onFailure(call: Call<List<PlayerScoreLocal>>, t: Throwable) {
            Log.e("TaskUtils", "Error fetching player scores: ${t.message}")
        }
    })
}


fun displayScore(
    context: Context,
    taskContainer: LinearLayout,
    bestScore: Double,
    totalPoints: Double
) {
    var infoBox = taskContainer.findViewWithTag<LinearLayout>("infoBoxContainer")
    var totalPointsTextView = infoBox?.findViewWithTag<TextView>("totalPointsTextView")

    if (infoBox == null) {
        infoBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            background = GradientDrawable().apply {
                if (bestScore == totalPoints) {
                    setColor(Color.parseColor("#DFF0D8"))
                    setStroke(4, Color.parseColor("#4CAF50"))
                } else {
                    setColor(Color.parseColor("#E3F2FD"))
                    setStroke(4, Color.parseColor("#2196F3"))
                }
                cornerRadius = 20f
            }
            elevation = 8f
            tag = "infoBoxContainer"
        }

        totalPointsTextView = TextView(context).apply {
            textSize = 18f
            setTypeface(null, Typeface.BOLD_ITALIC)
            setPadding(16, 8, 16, 8)
            gravity = Gravity.CENTER
            tag = "totalPointsTextView"
        }

        infoBox.addView(totalPointsTextView)
        taskContainer.addView(infoBox, 0)
    }

    totalPointsTextView?.let {
        it.text = "Total points: ${
            if (bestScore % 1.0 == 0.0) bestScore.toInt() else "%.2f".format(bestScore)
        } / ${if (totalPoints % 1.0 == 0.0) totalPoints.toInt() else "%.2f".format(totalPoints)}"
        it.setTextColor(
            if (bestScore == totalPoints) Color.parseColor("#006400") else Color.parseColor(
                "#1565C0"
            )
        )
    }

    infoBox?.background = GradientDrawable().apply {
        if (bestScore == totalPoints) {
            setColor(Color.parseColor("#DFF0D8"))
            setStroke(4, Color.parseColor("#4CAF50"))
        } else {
            setColor(Color.parseColor("#E3F2FD"))
            setStroke(4, Color.parseColor("#2196F3"))
        }
        cornerRadius = 20f
    }
}

fun displayWarning(context: Context, taskContainer: LinearLayout) {
    val infoBox = taskContainer.findViewWithTag<LinearLayout>("infoBoxContainer")

    if (infoBox != null) {
        var warningTextView = infoBox.findViewWithTag<TextView>("warningTextView")

        if (warningTextView == null) {
            warningTextView = TextView(context).apply {
                text = "You’ve reached the attempt limit for this task!"
                textSize = 18f
                setTextColor(Color.RED)
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                tag = "warningTextView"
            }
            infoBox.addView(warningTextView)
        }
    }
}

fun showCheckButton(
    taskContainer: LinearLayout,
    inputFields: List<View>,
    correctAnswers: List<String>,
    pointsList: List<Double>,
    totalPoints: Double,
    playerId: Int,
    apiService: ApiServiceLocal,
    task: TaskLocal,
    playerAttempts: Int,
    calculateScoreFunction: (List<View>, List<String>, List<Double>) -> Double
) {
    val checkButtonToRemove = taskContainer.findViewWithTag<MaterialButton>("checkButton")
    if (checkButtonToRemove != null) {
        taskContainer.removeView(checkButtonToRemove)
    }

    val checkButton = MaterialButton(taskContainer.context).apply {
        text = "Validate Task"
        tag = "checkButton"
        setTextColor(ContextCompat.getColor(taskContainer.context, R.color.red))
        setBackgroundColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
        setCornerRadius(16)
        setOnClickListener {
            if (!isInternetAvailable(taskContainer.context)) {
                val noInternetDialog = AlertDialog.Builder(taskContainer.context)
                    .setTitle("No Internet Connection")
                    .setMessage("Please connect to the internet to validate the task.")
                    .setPositiveButton("OK", null)
                    .show()

                val positiveButton =
                    noInternetDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                val negativeButton =
                    noInternetDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)

                positiveButton.setTextColor(ContextCompat.getColor(this.context, R.color.red))
                negativeButton.setTextColor(ContextCompat.getColor(this.context, R.color.red))

                positiveButton.backgroundTintList =
                    ContextCompat.getColorStateList(this.context, R.color.button_color_selector)
                negativeButton.backgroundTintList =
                    ContextCompat.getColorStateList(this.context, R.color.button_color_selector)

                return@setOnClickListener
            }


            this.visibility = MaterialButton.GONE

            val userPoints = calculateScoreFunction(inputFields, correctAnswers, pointsList)
            val adjustedPoints = if (playerAttempts == 1) userPoints * 0.8 else userPoints

            val existingTextView = taskContainer.findViewWithTag<TextView>("totalPointsTextView")
            existingTextView?.let { textView ->
                textView.text = "Total points: ${
                    if (adjustedPoints % 1.0 == 0.0) adjustedPoints.toInt() else "%.2f".format(
                        adjustedPoints
                    )
                } / ${
                    if (totalPoints % 1.0 == 0.0) totalPoints.toInt() else "%.2f".format(
                        totalPoints
                    )
                }"
                val existingInfoBox = textView.parent as? LinearLayout
                textView.setTextColor(
                    if (adjustedPoints == totalPoints) Color.parseColor("#006400") else Color.parseColor(
                        "#1565C0"
                    )
                )
                existingInfoBox?.background = GradientDrawable().apply {
                    val color =
                        if (adjustedPoints == totalPoints) Color.parseColor("#DFF0D8") else Color.parseColor(
                            "#E3F2FD"
                        )
                    setColor(color)
                    setStroke(
                        4,
                        if (adjustedPoints == totalPoints) Color.parseColor("#4CAF50") else Color.parseColor(
                            "#2196F3"
                        )
                    )
                    cornerRadius = 20f
                }
            }

            val playerScore = PlayerScoreLocal(
                player_id = playerId,
                task_id = task.id,
                score = adjustedPoints,
                attempt_count = playerAttempts + 1
            )

            apiService.addPlayerScore(playerScore).enqueue(object : Callback<PlayerScoreLocal> {
                override fun onResponse(
                    call: Call<PlayerScoreLocal>,
                    response: Response<PlayerScoreLocal>
                ) {
                    if (response.isSuccessful) {
                        if (playerAttempts == 0) {
                            val alertDialog = AlertDialog.Builder(taskContainer.context)
                                .setTitle("Result")
                                .setMessage(
                                    Html.fromHtml(
                                        "You have earned ${
                                            if (adjustedPoints % 1.0 == 0.0) adjustedPoints.toInt() else "%.2f".format(
                                                adjustedPoints
                                            )
                                        } points. You have one more try, but <font color='#FF0000'> you will lose 20% of the points</font> you earned.",
                                        Html.FROM_HTML_MODE_LEGACY
                                    )
                                )
                                .setPositiveButton("OK") { _, _ ->
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

                                        displayTask(
                                            taskContainer,
                                            task,
                                            playerId,
                                            apiService,
                                            { cont, t, p, api, input, correct, point ->
                                                if (task.type == "translation") {
                                                    displayTranslation(
                                                        cont,
                                                        t,
                                                        p,
                                                        api,
                                                        input,
                                                        correct,
                                                        point
                                                    )
                                                } else if (task.type == "matching_definitions") {
                                                    displayMatchingDefinitions(
                                                        cont,
                                                        t,
                                                        p,
                                                        api,
                                                        input,
                                                        correct,
                                                        point
                                                    )
                                                } else if (task.type == "matching_images") {
                                                    displayMatchingImages(
                                                        cont,
                                                        t,
                                                        p,
                                                        api,
                                                        input,
                                                        correct,
                                                        point
                                                    )
                                                } else if (task.type == "categorization") {
                                                    displayCategorizationTask(
                                                        cont,
                                                        t,
                                                        p,
                                                        api,
                                                        input,
                                                        correct,
                                                        point
                                                    )
                                                } else if (task.type == "context_choice") {
                                                    displayContextChoiceTask(
                                                        cont as LinearLayout,
                                                        t,
                                                        p,
                                                        api,
                                                        input,
                                                        mutableListOf(),
                                                        mutableListOf()
                                                    )
                                                } else if (t.type == "sentence_building") {
                                                    displaySentenceBuildingTask(
                                                        cont,
                                                        t,
                                                        p,
                                                        api,
                                                        input,
                                                        correct,
                                                        point
                                                    )
                                                } else if (t.type == "gap_filling") {
                                                    displayGapFillingTask(
                                                        cont as LinearLayout,
                                                        t,
                                                        p,
                                                        api,
                                                        input as MutableList<View>,
                                                        correct as MutableList<String>,
                                                        point as MutableList<Double>
                                                    )
                                                }
                                            },
                                            calculateScoreFunction
                                        )

                                        taskContainer.startAnimation(fadeIn)
                                    }, 300)
                                }
                                .setCancelable(false)
                                .show()

                            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            positiveButton.backgroundTintList = ContextCompat.getColorStateList(
                                taskContainer.context,
                                R.color.button_color_selector
                            )
                            positiveButton.setTextColor(
                                ContextCompat.getColor(
                                    taskContainer.context,
                                    R.color.red
                                )
                            )
                        } else {
                            val alertDialog = AlertDialog.Builder(taskContainer.context)
                                .setTitle("Result")
                                .setMessage(
                                    "You have earned ${
                                        if (adjustedPoints % 1.0 == 0.0) adjustedPoints.toInt() else "%.2f".format(
                                            adjustedPoints
                                        )
                                    } points. Keep going and collect even more points!"
                                )
                                .setPositiveButton("OK", null)
                                .show()

                            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            positiveButton.backgroundTintList = ContextCompat.getColorStateList(
                                taskContainer.context,
                                R.color.button_color_selector
                            )
                            positiveButton.setTextColor(
                                ContextCompat.getColor(
                                    taskContainer.context,
                                    R.color.red
                                )
                            )
                        }
                    }
                }

                override fun onFailure(call: Call<PlayerScoreLocal>, t: Throwable) {
                    val failureTextView = TextView(taskContainer.context).apply {
                        text = "Error communicating with the server."
                        textSize = 18f
                        setTextColor(Color.RED)
                        setPadding(16, 0, 16, 16)
                    }
                    taskContainer.addView(failureTextView, 0)
                }
            })
        }
    }

    taskContainer.addView(checkButton)
}

fun dpToPx(dp: Int, context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (dp * density).toInt()
}

@SuppressLint("ServiceCast")
fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}