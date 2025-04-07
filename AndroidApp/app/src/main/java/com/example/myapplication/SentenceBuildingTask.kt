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
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import org.json.JSONException
import org.json.JSONObject

fun displaySentenceBuildingTask(
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
        Log.e("TaskUtils", "Empty or invalid JSON data for sentence building task.")
        showErrorMessage(taskContainer, "Invalid task data.")
        return
    }

    try {
        val jsonObject = JSONObject(json)
        /*
        val sentencesArray = jsonObject.optJSONArray("sentences")

        if (sentencesArray == null || sentencesArray.length() == 0) {
            Log.e("TaskUtils", "No sentences found in JSON data.")
            showErrorMessage(taskContainer, "No sentences available for this task.")
            return
        }

        addStyledTextView(
            taskContainer,
            "Arrange the words to form the correct sentences.",
            16f,
            Color.DKGRAY
        )

        val sentencesList = mutableListOf<JSONObject>()
        for (i in 0 until sentencesArray.length()) {
            sentencesList.add(sentencesArray.getJSONObject(i))
        }
         */

        val sentencesArray = jsonObject.optJSONArray("sentences")

        if (sentencesArray == null || sentencesArray.length() == 0) {
            Log.e("TaskUtils", "No sentences found in JSON data.")
            showErrorMessage(taskContainer, "No sentences available for this task.")
            return
        }

        addStyledTextView(
            taskContainer,
            "Arrange the words to form the correct sentences.",
            16f,
            Color.DKGRAY
        )

        val sentencesList = mutableListOf<JSONObject>()
        for (i in 0 until sentencesArray.length()) {
            sentencesList.add(sentencesArray.getJSONObject(i))
        }

        sentencesList.shuffle()

        sentencesList.forEachIndexed { sentenceIndex, sentenceObject ->
            val correctSentence = sentenceObject.optString("correct", "").trim()
            correctAnswers.add(correctSentence)
            val wordsArray = sentenceObject.optJSONArray("words")
            val points = sentenceObject.optDouble("points", 0.0)

            if (correctSentence.isEmpty() || wordsArray == null || wordsArray.length() == 0) {
                Log.w("TaskUtils", "Skipping sentence due to missing or invalid data.")
                return@forEachIndexed
            }

            val words = mutableListOf<String>()
            for (j in 0 until wordsArray.length()) {
                words.add(wordsArray.getString(j))
            }
            words.shuffle()

            addStyledTextView(
                taskContainer,
                "Sentence ${sentenceIndex + 1}:",
                18f,
                Color.BLACK
            ).apply {
                setPadding(16, 32, 16, 8)
            }

            val currentSentenceTextView = TextView(taskContainer.context).apply {
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(16, 8, 16, 16)
                gravity = Gravity.CENTER
            }
            taskContainer.addView(currentSentenceTextView)

            val sentenceContainer = LinearLayout(taskContainer.context).apply {
                orientation = LinearLayout.VERTICAL
            }
            taskContainer.addView(sentenceContainer)

            val dropTargetLayout = FlexboxLayout(taskContainer.context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.CENTER
                setPadding(16, 8, 16, 16)
            }
            sentenceContainer.addView(dropTargetLayout)

            val draggableWordsLayout = FlexboxLayout(taskContainer.context).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = com.google.android.flexbox.JustifyContent.CENTER
                setPadding(16, 8, 16, 16)
            }
            sentenceContainer.addView(draggableWordsLayout)


            val dropTargets = mutableListOf<TextView>()
            val placedWords = mutableListOf<String>()
            val originalWordViews = mutableMapOf<String, TextView>()
            val placeholder = "_____"

            for (k in 0 until words.size) {
                val dropTarget = createDropTargetTextView(taskContainer.context, k, sentenceIndex)
                dropTargetLayout.addView(dropTarget)
                dropTargets.add(dropTarget)
                placedWords.add(placeholder)
                inputFields.add(dropTarget)
            }

            currentSentenceTextView.text = placedWords.joinToString(" ")

            words.forEach { word ->
                val wordTextView = createDraggableWordTextView(
                    taskContainer.context,
                    word,
                    sentenceIndex
                ) { draggedWord, view ->
                    var dropped = false
                    dropTargets.forEachIndexed { index, dropTarget ->
                        val targetTag = dropTarget.tag as? Pair<Int, Int>
                        val draggedTag = view.tag as? Pair<Int, String>
                        if (targetTag?.first == sentenceIndex && draggedTag?.first == sentenceIndex && targetTag?.second == index && dropTarget.text.isEmpty()) {
                            dropTarget.text = draggedWord
                            placedWords[index] = draggedWord
                            currentSentenceTextView.text = placedWords.joinToString(" ").trim()
                            view.visibility = View.GONE
                            dropped = true
                            return@forEachIndexed
                        }
                    }
                    dropped
                }
                draggableWordsLayout.addView(wordTextView)
                originalWordViews[word] = wordTextView
            }

            dropTargets.forEachIndexed { index, dropTarget ->
                dropTarget.setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DROP -> {
                            val draggedView = event.localState as View
                            val draggedTag = draggedView.tag as? Pair<Int, String>
                            val targetTag = v.tag as? Pair<Int, Int>

                            if (draggedTag?.first != targetTag?.first) {
                                return@setOnDragListener false
                            }

                            val draggedSentenceId = draggedTag?.first
                            val draggedText = draggedTag?.second
                            val targetSentenceId = targetTag?.first
                            val targetIndex = targetTag?.second

                            val targetTextView = v as TextView
                            val context = targetTextView.context
                            val dropIndex = dropTargets.indexOf(v)
                            val previousWord = targetTextView.text.toString()

                            if (targetTextView.text.isEmpty()) {
                                targetTextView.text = draggedText
                                targetTextView.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.red
                                    )
                                )
                                targetTextView.background = GradientDrawable().apply {
                                    setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                                    //setStroke(2, Color.GRAY)
                                    cornerRadius = 8f
                                }
                                draggedView.visibility = View.GONE
                                if (dropIndex != -1) {
                                    if (draggedText != null) {
                                        placedWords[dropIndex] = draggedText
                                    }
                                    currentSentenceTextView.text =
                                        placedWords.joinToString(" ").trim()
                                }
                                return@setOnDragListener true
                            } else if (previousWord != draggedText) {
                                originalWordViews[previousWord]?.visibility = View.VISIBLE
                                targetTextView.text = draggedText
                                targetTextView.setTextColor(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.red
                                    )
                                )
                                targetTextView.background = GradientDrawable().apply {
                                    setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                                    //setStroke(2, Color.GRAY)
                                    cornerRadius = 8f
                                }
                                draggedView.visibility = View.GONE
                                if (dropIndex != -1) {
                                    if (draggedText != null) {
                                        placedWords[dropIndex] = draggedText
                                    }
                                    currentSentenceTextView.text =
                                        placedWords.joinToString(" ").trim()
                                }
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
                            val draggedView = event.localState as View
                            val draggedTag = draggedView.tag as? Pair<Int, String>
                            val targetTag = v.tag as? Pair<Int, Int>

                            if (draggedTag?.first == targetTag?.first) {
                                (v as TextView).background = GradientDrawable().apply {
                                    setColor(
                                        ContextCompat.getColor(
                                            v.context,
                                            R.color.colorPrimary
                                        )
                                    )
                                    //setStroke(2, Color.GRAY)
                                    cornerRadius = 8f
                                }
                            }
                            return@setOnDragListener true
                        }

                        DragEvent.ACTION_DRAG_EXITED -> {
                            val draggedView = event.localState as View
                            val draggedTag = draggedView.tag as? Pair<Int, String>
                            val targetTag = v.tag as? Pair<Int, Int>
                            val tv = v as TextView

                            if (draggedTag?.first == targetTag?.first) {
                                tv.background = GradientDrawable().apply {
                                    setColor(
                                        if (tv.text.isEmpty()) Color.LTGRAY else ContextCompat.getColor(
                                            tv.context,
                                            R.color.colorPrimary
                                        )
                                    )
                                    //setStroke(2, Color.GRAY)
                                    cornerRadius = 8f
                                }
                            }
                            return@setOnDragListener true
                        }

                        else -> return@setOnDragListener true
                    }
                }
            }

            dropTargets.forEach { dropTarget ->
                dropTarget.setOnClickListener {
                    val clickedTextView = it as TextView
                    val word = clickedTextView.text.toString()
                    val tag = clickedTextView.tag as? Pair<Int, Int>
                    val sentenceId = tag?.first

                    if (word.isNotEmpty() && sentenceId == sentenceIndex) {
                        originalWordViews[word]?.visibility = View.VISIBLE
                        clickedTextView.text = ""
                        clickedTextView.setTextColor(
                            ContextCompat.getColor(
                                clickedTextView.context,
                                R.color.colorPrimary
                            )
                        )
                        clickedTextView.background = GradientDrawable().apply {
                            setColor(Color.LTGRAY)
                            //setStroke(2, Color.GRAY)
                            cornerRadius = 8f
                        }
                        val indexToRemove = placedWords.indexOf(word)
                        if (indexToRemove != -1) {
                            placedWords[indexToRemove] = placeholder
                            currentSentenceTextView.text = placedWords.joinToString(" ").trim()
                        }
                    }
                }
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
        Log.e("TaskUtils", "Error parsing JSON for sentence building task: ${e.message}")
        showErrorMessage(taskContainer, "Error processing the task.")
    }
}

fun calculateSentenceBuildingScore(
    inputFields: List<View>,
    correctAnswers: List<String>,
    pointsList: List<Double>
): Double {
    var totalScore = 0.0
    var inputFieldIndex = 0
    for (i in correctAnswers.indices) {
        val correctAnswerWords = correctAnswers[i].trim().split("\\s+".toRegex())
        if (inputFields.size >= inputFieldIndex + correctAnswerWords.size) {
            for (j in 0 until correctAnswerWords.size) {
                val dropTargetView = inputFields[inputFieldIndex + j] as? TextView
                val droppedWord = dropTargetView?.text?.toString()?.trim()
                if (droppedWord.equals(correctAnswerWords[j], ignoreCase = true)) {
                    totalScore += 1.0
                }
            }
            inputFieldIndex += correctAnswerWords.size
        }
    }
    return totalScore
}

private fun createDraggableWordTextView(
    context: Context,
    word: String,
    sentenceId: Int,
    onWordDropped: (String, TextView) -> Boolean
): TextView {
    return TextView(context).apply {
        text = word
        textSize = 18f
        setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        setPadding(16, 16, 16, 16)
        tag = Pair(sentenceId, word)
        background = GradientDrawable().apply {
            setColor(ContextCompat.getColor(context, R.color.red))
            cornerRadius = 8f
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dpToPx(8, context)
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

private fun createDropTargetTextView(context: Context, index: Int, sentenceId: Int): TextView {
    return TextView(context).apply {
        text = ""
        textSize = 18f
        setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
        setPadding(16, 16, 16, 16)
        tag = Pair(sentenceId, index)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(Color.LTGRAY)
            //setStroke(2, Color.GRAY)
            cornerRadius = 8f
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dpToPx(8, context)
            marginEnd = dpToPx(8, context)
            bottomMargin = dpToPx(8, context)
        }

        minWidth = dpToPx(50, context)
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