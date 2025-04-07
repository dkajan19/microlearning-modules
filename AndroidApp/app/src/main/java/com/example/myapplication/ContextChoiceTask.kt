package com.example.myapplication

import ApiServiceLocal
import TaskLocal
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONException
import org.json.JSONObject

fun displayContextChoiceTask(
    taskContainer: LinearLayout,
    task: TaskLocal,
    playerId: Int,
    apiService: ApiServiceLocal,
    inputFields: MutableList<View>,
    correctAnswers: MutableList<String>,
    pointsList: MutableList<Double>
) {
    taskContainer.removeAllViews()
    inputFields.clear()
    correctAnswers.clear()
    pointsList.clear()

    val json = task.data.toString().trim()

    if (json.isEmpty() || json == "{}") {
        Log.e("TaskUtils", "Empty or invalid JSON data.")
        showErrorMessage(taskContainer, "Invalid task data.")
        return
    }

    try {
        val jsonObject = JSONObject(json)

        /*
        val contextsObject = jsonObject.optJSONObject("contexts")
        if (contextsObject == null || contextsObject.length() == 0) {
            Log.e("TaskUtils", "No valid contexts found in JSON.")
            showErrorMessage(taskContainer, "No valid options available.")
            return
        }

        addStyledTextView(
            taskContainer,
            "Select the correct option(s) for each context.",
            16f,
            Color.DKGRAY
        )

        val contextKeys = contextsObject.keys()
        while (contextKeys.hasNext()) {
            val contextKey = contextKeys.next()
            val contextData = contextsObject.optJSONObject(contextKey) ?: continue

            val contextName = contextData.optString("name", "").trim()
            val optionsArray = contextData.optJSONArray("options")

            if (contextName.isEmpty() || optionsArray == null || optionsArray.length() == 0) {
                Log.w("TaskUtils", "Skipping context '$contextKey' due to missing or invalid data.")
                continue
            }

            addStyledTextView(taskContainer, contextName, 18f, Color.BLACK).apply {
                setPadding(16, 50, 16, 8)
            }

        */

        val contextsObject = jsonObject.optJSONObject("contexts")
        if (contextsObject == null || contextsObject.length() == 0) {
            Log.e("TaskUtils", "No valid contexts found in JSON.")
            showErrorMessage(taskContainer, "No valid options available.")
            return
        }

        addStyledTextView(
            taskContainer,
            "Select the correct option(s) for each context.",
            16f,
            Color.DKGRAY
        )

        val contextKeys = contextsObject.keys().asSequence().toMutableList()
        contextKeys.shuffle()

        for (contextKey in contextKeys) {
            val contextData = contextsObject.optJSONObject(contextKey) ?: continue

            val contextName = contextData.optString("name", "").trim()

            /*

            val optionsArray = contextData.optJSONArray("options")

            if (contextName.isEmpty() || optionsArray == null || optionsArray.length() == 0) {
                Log.w("TaskUtils", "Skipping context '$contextKey' due to missing or invalid data.")
                continue
            }

            addStyledTextView(taskContainer, contextName, 18f, Color.BLACK).apply {
                setPadding(16, 50, 16, 8)
            }

            for (i in 0 until optionsArray.length()) {
                val optionObject = optionsArray.optJSONObject(i) ?: continue

                val optionText = optionObject.optString("text", "").trim()
                val optionPoints = optionObject.optDouble("points", 0.0)

                if (optionText.isEmpty()) {
                    Log.w("TaskUtils", "Skipping an option due to missing text.")
                    continue
                }

                val checkBox = CheckBox(taskContainer.context).apply {
                    text = optionText
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    setPadding(16, 8, 16, 8)
                    tag = optionPoints
                }
                taskContainer.addView(checkBox)
                inputFields.add(checkBox)
            }

             */

            val optionsArray = contextData.optJSONArray("options")

            val optionsList = mutableListOf<JSONObject>()
            for (i in 0 until optionsArray.length()) {
                optionsList.add(optionsArray.getJSONObject(i))
            }
            optionsList.shuffle()

            if (contextName.isEmpty() || optionsArray == null || optionsArray.length() == 0) {
                Log.w("TaskUtils", "Skipping context '$contextKey' due to missing or invalid data.")
                continue
            }

            addStyledTextView(taskContainer, contextName, 18f, Color.BLACK).apply {
                setPadding(16, 50, 16, 8)
            }

            for (optionObject in optionsList) {
                val optionText = optionObject.optString("text", "").trim()
                val optionPoints = optionObject.optDouble("points", 0.0)

                if (optionText.isEmpty()) {
                    Log.w("TaskUtils", "Skipping an option due to missing text.")
                    continue
                }

                val checkBox = CheckBox(taskContainer.context).apply {
                    text = optionText
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    setPadding(16, 8, 16, 8)
                    tag = optionPoints
                }
                taskContainer.addView(checkBox)
                inputFields.add(checkBox)
            }
        }

        val spacer = View(taskContainer.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )
        }
        taskContainer.addView(spacer)

    } catch (e: JSONException) {
        Log.e("TaskUtils", "Error parsing JSON: ${e.message}")
        showErrorMessage(taskContainer, "Error processing the task.")
    }
}

fun calculateContextChoiceScore(inputFields: List<View>): Double {
    return inputFields.filterIsInstance<CheckBox>()
        .filter { it.isChecked }
        .sumOf { (it.tag as? Double) ?: 0.0 }
}

private fun addStyledTextView(
    container: LinearLayout,
    text: String,
    textSize: Float,
    color: Int
): TextView {
    val textView = TextView(container.context).apply {
        this.text = text
        this.textSize = textSize
        setTextColor(color)
        setPadding(16, 16, 16, 8)
    }
    container.addView(textView)
    return textView
}

private fun showErrorMessage(container: LinearLayout, message: String) {
    addStyledTextView(container, message, 18f, Color.RED)
}