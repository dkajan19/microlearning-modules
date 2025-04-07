package com.example.myapplication

import ApiServiceLocal
import TaskLocal
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONException
import org.json.JSONObject
import androidx.appcompat.view.ContextThemeWrapper
import java.util.Collections

fun displayTranslation(
    taskContainer: LinearLayout,
    task: TaskLocal,
    playerId: Int,
    apiService: ApiServiceLocal,
    inputFields: MutableList<View>,
    correctAnswers: MutableList<String>,
    pointsList: MutableList<Double>
) {
    taskContainer.removeAllViews()

    val json = task.data.toString()

    try {
        if (json.isBlank() || json == "{}") {
            return
        }

        val jsonObject = JSONObject(json)

        /*
        val pairs = jsonObject.getJSONArray("pairs")

        if (!jsonObject.has("pairs") || jsonObject.getJSONArray("pairs").length() == 0) {
            return
        }

        val totalPoints = jsonObject.getDouble("totalPoints")

        val helperTextView = TextView(taskContainer.context).apply {
            text = "Enter the correct translation for each word."
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 32, 16, 16)
        }
        taskContainer.addView(helperTextView)

        for (i in 0 until pairs.length()) {
            val pair = pairs.getJSONObject(i)
            val word = pair.getString("word")
            val translation = pair.getString("translation")
            val points = pair.getDouble("points")
        */

        val pairsJsonArray = jsonObject.getJSONArray("pairs")

        if (!jsonObject.has("pairs") || pairsJsonArray.length() == 0) {
            return
        }

        val totalPoints = jsonObject.getDouble("totalPoints")

        val helperTextView = TextView(taskContainer.context).apply {
            text = "Enter the correct translation for each word."
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 32, 16, 16)
        }
        taskContainer.addView(helperTextView)

        val pairsList = mutableListOf<JSONObject>()
        for (i in 0 until pairsJsonArray.length()) {
            pairsList.add(pairsJsonArray.getJSONObject(i))
        }

        Collections.shuffle(pairsList)

        for (pair in pairsList) {
            val word = pair.getString("word")
            val translation = pair.getString("translation")
            val points = pair.getDouble("points")
            val linearLayout = LinearLayout(taskContainer.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 16, 0, 100)
            }

            val wordTextView = TextView(taskContainer.context).apply {
                text = "$word - "
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(16, 0, 8, 0)
            }

            val textInputLayout = TextInputLayout(taskContainer.context).apply {
                setPadding(16, 8, 16, 8)
                isHintEnabled = false
            }

            val themedContext: Context = ContextThemeWrapper(
                taskContainer.context,
                R.style.ThemeOverlay_AppTheme_TextInputEditText_Outlined
            )

            val translationEditText = TextInputEditText(themedContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
                onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        textInputLayout.boxStrokeColor =
                            ContextCompat.getColor(taskContainer.context, R.color.colorPrimary)
                    } else {
                        textInputLayout.boxStrokeColor = Color.GRAY
                    }
                }
            }

            textInputLayout.addView(translationEditText)
            linearLayout.addView(wordTextView)
            linearLayout.addView(textInputLayout)
            taskContainer.addView(linearLayout)

            inputFields.add(translationEditText)
            correctAnswers.add(translation)
            pointsList.add(points)
        }
    } catch (e: JSONException) {
        val errorTextView = TextView(taskContainer.context).apply {
            text = "Error processing the task."
            textSize = 18f
            setPadding(16, 16, 16, 0)
        }
        taskContainer.addView(errorTextView)
        Log.e("TaskUtils", "Error parsing JSON: ${e.message}")
    }
}

fun calculateTranslationScore(
    inputFields: List<View>,
    correctAnswers: List<String>,
    pointsList: List<Double>
): Double {
    var userPoints = 0.0
    for (i in inputFields.indices) {
        val userTranslation = (inputFields[i] as TextInputEditText).text.toString()
        if (userTranslation.equals(correctAnswers[i], ignoreCase = true)) {
            userPoints += pointsList[i]
        }
    }
    return userPoints
}