package com.example.myapplication

import ApiServiceLocal
import TaskLocal
import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.JustifyContent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun displayGapFillingTask(
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

    if (json.isEmpty() || json == "{}" || json == "null") {
        Log.e("TaskUtils", "Empty or invalid JSON data for gap filling task.")
        showErrorMessage(taskContainer, "Invalid task data.")
        return
    }

    try {
        val jsonObject = JSONObject(json)
        //val sentencesArray = jsonObject.optJSONArray("sentences")

        var sentencesArray = jsonObject.optJSONArray("sentences")

        if (sentencesArray == null || sentencesArray.length() == 0) {
            Log.e("TaskUtils", "No sentences found in JSON data for gap filling task.")
            showErrorMessage(taskContainer, "No sentences available for this task.")
            return
        }

        val shuffledSentencesList = mutableListOf<JSONObject>()

        for (i in 0 until sentencesArray.length()) {
            shuffledSentencesList.add(sentencesArray.getJSONObject(i))
        }

        shuffledSentencesList.shuffle()

        val shuffledJSONArray = JSONArray()
        shuffledSentencesList.forEach {
            shuffledJSONArray.put(it)
        }

        sentencesArray = shuffledJSONArray


        if (sentencesArray == null || sentencesArray.length() == 0) {
            Log.e("TaskUtils", "No sentences found in JSON data for gap filling task.")
            showErrorMessage(taskContainer, "No sentences available for this task.")
            return
        }

        addStyledTextView(
            taskContainer,
            "Drag the correct word to fill in the blank in each sentence.",
            16f,
            Color.DKGRAY
        )

        val allWords = mutableListOf<String>()
        val dropTargets = mutableListOf<TextView>()
        val sentenceContainers = mutableListOf<LinearLayout>()

        for (i in 0 until sentencesArray.length()) {
            val sentenceObject = sentencesArray.getJSONObject(i)
            val sentenceWithGap = sentenceObject.optString("sentence", "").trim()
            val correctAnswer = sentenceObject.optString("correct", "").trim()
            val points = sentenceObject.optDouble("points", 0.0)

            if (sentenceWithGap.isEmpty() || !sentenceWithGap.contains("_") || correctAnswer.isEmpty()) {
                Log.w("TaskUtils", "Skipping sentence due to missing or invalid data.")
                continue
            }

            correctAnswers.add(correctAnswer)
            pointsList.add(points)
            allWords.add(correctAnswer)

            val sentenceContainer = LinearLayout(taskContainer.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 8, 16, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(12, taskContainer.context)
                }
            }
            taskContainer.addView(sentenceContainer)
            sentenceContainers.add(sentenceContainer)

            val sentenceParts = sentenceWithGap.split("_")
            val dropTargetTextView = TextView(taskContainer.context).apply {
                text = ""
                textSize = 17f
                setTextColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
                setPadding(8, 16, 8, 16)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.LTGRAY)
                    cornerRadius = 8f
                }
                minWidth = dpToPx(100, taskContainer.context)
                tag = Pair(i, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(8, taskContainer.context)
                    marginEnd = dpToPx(8, taskContainer.context)
                }
            }
            dropTargets.add(dropTargetTextView)
            inputFields.add(dropTargetTextView)

            sentenceParts.forEachIndexed { index, part ->
                val textViewPart = TextView(taskContainer.context).apply {
                    text = part.trim()
                    textSize = 17f
                    setTextColor(Color.BLACK)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                sentenceContainer.addView(textViewPart)
                if (index < sentenceParts.size - 1) {
                    sentenceContainer.addView(dropTargetTextView)
                }
            }
        }

        val draggableWordsLayout = FlexboxLayout(taskContainer.context).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
            setPadding(16, 32, 16, 16)
        }
        taskContainer.addView(draggableWordsLayout)

        val shuffledWords = allWords.shuffled()

        dropTargets.forEach { dropTarget ->
            dropTarget.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        val draggedView = event.localState as View
                        val draggedText = (draggedView as TextView).text.toString()

                        if ((v as TextView).text.isEmpty()) {
                            (v as TextView).text = draggedText
                            v.setTextColor(ContextCompat.getColor(v.context, R.color.red))
                            v.background = GradientDrawable().apply {
                                setColor(ContextCompat.getColor(v.context, R.color.colorPrimary))
                                cornerRadius = 8f
                            }
                            draggedView.visibility = View.GONE
                            return@setOnDragListener true
                        }
                        return@setOnDragListener false
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        val draggedView = event.localState as? View
                        if (draggedView?.visibility != View.GONE && (v as TextView).text.isEmpty()) {
                            draggedView?.visibility = View.VISIBLE
                        }
                        return@setOnDragListener true
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        (v as TextView).background = GradientDrawable().apply {
                            setColor(ContextCompat.getColor(v.context, R.color.colorPrimary))
                            cornerRadius = 8f
                        }
                        return@setOnDragListener true
                    }

                    DragEvent.ACTION_DRAG_EXITED -> {
                        val tv = v as TextView
                        tv.background = GradientDrawable().apply {
                            setColor(
                                if (tv.text.isEmpty()) Color.LTGRAY else ContextCompat.getColor(
                                    tv.context,
                                    R.color.colorPrimary
                                )
                            )
                            cornerRadius = 8f
                        }
                        return@setOnDragListener true
                    }

                    else -> return@setOnDragListener true
                }
            }

            dropTarget.setOnClickListener { clickedView ->
                val currentText = (clickedView as TextView).text.toString()
                if (currentText.isNotEmpty()) {
                    val newDraggableWord = createDraggableWordTextView(
                        clickedView.context,
                        currentText
                    ) { draggedWord, view ->
                        var dropped = false
                        dropTargets.forEach { target ->
                            if (target.text.isEmpty()) {
                                target.text = draggedWord
                                view.visibility = View.GONE
                                dropped = true
                                return@forEach
                            }
                        }
                        dropped
                    }
                    draggableWordsLayout.addView(newDraggableWord)

                    clickedView.text = ""
                    clickedView.setTextColor(
                        ContextCompat.getColor(
                            clickedView.context,
                            R.color.colorPrimary
                        )
                    )
                    clickedView.background = GradientDrawable().apply {
                        setColor(Color.LTGRAY)
                        cornerRadius = 8f
                    }
                }
            }
        }

        shuffledWords.forEach { word ->
            val wordTextView =
                createDraggableWordTextView(taskContainer.context, word) { draggedWord, view ->
                    var dropped = false
                    dropTargets.forEach { dropTarget ->
                        if (dropTarget.text.isEmpty()) {
                            dropTarget.text = draggedWord
                            view.visibility = View.GONE
                            dropped = true
                            return@forEach
                        }
                    }
                    dropped
                }
            draggableWordsLayout.addView(wordTextView)
        }

        val spacer = View(taskContainer.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )
        }
        taskContainer.addView(spacer)

    } catch (e: JSONException) {
        Log.e("TaskUtils", "Error parsing JSON for gap filling task: ${e.message}")
        showErrorMessage(taskContainer, "Error processing the task.")
    }
}

fun calculateGapFillingScore(
    inputFields: List<View>,
    correctAnswers: List<String>,
    pointsList: List<Double>
): Double {
    var totalScore = 0.0
    inputFields.forEachIndexed { index, view ->
        if (view is TextView) {
            val userAnswer = view.text.toString().trim()
            val correctAnswer = correctAnswers.getOrNull(index)?.trim()
            if (userAnswer.equals(correctAnswer, ignoreCase = true) && correctAnswer != null) {
                totalScore += pointsList.getOrNull(index) ?: 0.0
            }
        }
    }
    return totalScore
}

private fun createDraggableWordTextView(
    context: Context,
    word: String,
    onWordDropped: (String, TextView) -> Boolean
): TextView {
    return TextView(context).apply {
        text = word
        textSize = 17f
        setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        setPadding(16, 16, 16, 16)
        tag = word
        background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.red))
            cornerRadius = 8f
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            //marginStart = dpToPx(8, context)
            marginEnd = dpToPx(8, context)
            bottomMargin = dpToPx(8, context)
        }
        setOnLongClickListener { view ->
            val clipData = ClipData.newPlainText("", "")
            val dragShadowBuilder = View.DragShadowBuilder(view)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                (view as TextView).startDragAndDrop(clipData, dragShadowBuilder, view, 0)
            } else {
                (view as TextView).startDrag(clipData, dragShadowBuilder, view, 0)
            }
            view.visibility = View.INVISIBLE
            true
        }
    }
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