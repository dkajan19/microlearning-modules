package com.example.myapplication

import ApiServiceLocal
import TaskLocal
import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.Random
import androidx.core.view.children
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.flexbox.FlexboxLayout.LayoutParams as FlexboxLayoutParams

fun displayCategorizationTask(
    taskContainer: LinearLayout,
    task: TaskLocal,
    playerId: Int,
    apiService: ApiServiceLocal,
    inputFields: MutableList<View>,
    correctAnswers: MutableList<String>,
    pointsList: MutableList<Double>
) {
    taskContainer.removeAllViews()
    correctAnswers.clear()
    pointsList.clear()
    inputFields.clear()

    val json = task.data.toString()

    try {
        val jsonObject = JSONObject(json)
        val categoriesObject = jsonObject.getJSONObject("categories")
        val categoriesMap = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        val allWordsList = mutableListOf<Pair<String, Double>>()

        val categoryNames = categoriesObject.keys().asSequence().toList()

        for (categoryName in categoryNames) {
            val categoryData = categoriesObject.getJSONObject(categoryName)
            val name = categoryData.getString("name")
            val wordsArray = categoryData.getJSONArray("words")
            val wordsInCategory = mutableListOf<Pair<String, Double>>()
            for (i in 0 until wordsArray.length()) {
                val wordObject = wordsArray.getJSONObject(i)
                val text = wordObject.getString("text")
                val points = wordObject.getDouble("points")
                wordsInCategory.add(Pair(text, points))
                allWordsList.add(Pair(text, points))
                correctAnswers.add(categoryName)
                pointsList.add(points)
            }
            categoriesMap[name] = wordsInCategory
        }

        val helperTextView = TextView(taskContainer.context).apply {
            text = "Sort the words into the correct categories."
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 32, 16, 16)
        }
        taskContainer.addView(helperTextView)

        val categoriesLayout = LinearLayout(taskContainer.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16, taskContainer.context)
            }
        }
        taskContainer.addView(categoriesLayout)

        val categoryBoxes = mutableListOf<LinearLayout>()
        //val categoryKeysList = categoriesObject.keys().asSequence().toList()
        val categoryKeysList = categoriesObject.keys().asSequence().toList()
            .shuffled(Random(System.currentTimeMillis()))

        for (i in categoryKeysList.indices) {
            if (i % 2 == 0) {
                val horizontalLayout = LinearLayout(taskContainer.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (i > 0) {
                            topMargin = dpToPx(16, taskContainer.context)
                        }
                    }
                    weightSum = 2f
                }
                categoriesLayout.addView(horizontalLayout)
            }

            val categoryKey = categoryKeysList[i]
            val categoryData = categoriesObject.getJSONObject(categoryKey)
            val categoryName = categoryData.getString("name")

            val categoryBox = LinearLayout(taskContainer.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    if (categoryKeysList.size % 2 != 0 && i == categoryKeysList.size - 1)
                        LinearLayout.LayoutParams.MATCH_PARENT
                    else
                        0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    if (categoryKeysList.size % 2 != 0 && i == categoryKeysList.size - 1)
                        0f
                    else
                        1f
                ).apply {
                    val margin = dpToPx(8, taskContainer.context)
                    marginStart = if (i % 2 == 0) margin else margin / 2
                    marginEnd = if (i % 2 != 0) margin else margin / 2
                }
                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
                    cornerRadius = 8f
                    //setStroke(2, Color.GRAY)
                }
                setPadding(16, 16, 16, 16)
                tag = categoryName
            }

            val categoryTitle = TextView(taskContainer.context).apply {
                text = categoryName
                textSize = 18f
                setTextColor(ContextCompat.getColor(taskContainer.context, R.color.red))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(8, taskContainer.context)
                }
            }
            categoryBox.addView(categoryTitle)

            val finalTaskContainer = taskContainer

            categoryBox.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        val draggedView = event.localState as TextView
                        draggedView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = dpToPx(8, taskContainer.context)
                        }

                        val draggedWord = draggedView.text.toString()

                        android.util.Log.d(
                            "DragDrop",
                            "Dropped word: $draggedWord into category: ${categoryBox.tag}"
                        )

                        (draggedView.parent as? ViewGroup)?.removeView(draggedView)

                        (v as LinearLayout).addView(draggedView)

                        draggedView.setOnClickListener { clickedView ->
                            if (clickedView.parent is ViewGroup) {
                                val wordText = (clickedView as TextView).text.toString()

                                val wordsLayoutLocal =
                                    finalTaskContainer.children.find { it is FlexboxLayout } as? FlexboxLayout

                                wordsLayoutLocal?.let {
                                    val newWordTextView = TextView(taskContainer.context).apply {
                                        text = wordText
                                        textSize = 16f
                                        setTextColor(
                                            ContextCompat.getColor(
                                                taskContainer.context,
                                                R.color.colorPrimary
                                            )
                                        )
                                        setPadding(16, 16, 16, 16)
                                        tag = wordText
                                        background = GradientDrawable().apply {
                                            setColor(
                                                ContextCompat.getColor(
                                                    taskContainer.context,
                                                    R.color.red
                                                )
                                            )
                                            cornerRadius = 8f
                                        }
                                        val params = FlexboxLayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                        val margin = dpToPx(4, taskContainer.context)
                                        params.setMargins(margin, margin, margin, margin)
                                        layoutParams = params
                                        minWidth = dpToPx(70, taskContainer.context)
                                        setOnLongClickListener { view ->
                                            val clipData = ClipData.newPlainText("", "")
                                            val dragShadowBuilder = View.DragShadowBuilder(view)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                view.startDragAndDrop(
                                                    clipData,
                                                    dragShadowBuilder,
                                                    view,
                                                    0
                                                )
                                            } else {
                                                view.startDrag(clipData, dragShadowBuilder, view, 0)
                                            }
                                            view.visibility = View.INVISIBLE
                                            true
                                        }
                                    }

                                    it.addView(newWordTextView)
                                }

                                (clickedView.parent as ViewGroup).removeView(clickedView)
                                android.util.Log.d(
                                    "ClickToRemove",
                                    "Clicked and returned word: $wordText to the word list."
                                )
                            }
                        }

                        draggedView.visibility = View.VISIBLE
                        true
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        (event.localState as View).visibility = View.VISIBLE
                        true
                    }

                    DragEvent.ACTION_DRAG_STARTED -> {
                        (event.localState as View).visibility = View.INVISIBLE
                        true
                    }

                    else -> true
                }
            }

            (categoriesLayout.getChildAt(categoriesLayout.childCount - 1) as LinearLayout).addView(
                categoryBox
            )
            categoryBoxes.add(categoryBox)
            inputFields.add(categoryBox)
        }

        val wordsLayout = FlexboxLayout(taskContainer.context).apply {
            layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.MATCH_PARENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16, taskContainer.context)
                bottomMargin = dpToPx(16, taskContainer.context)
            }
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }

        taskContainer.addView(wordsLayout)

        val shuffledWords = allWordsList.shuffled(Random(System.currentTimeMillis()))

        for (i in shuffledWords.indices) {
            val wordPair = shuffledWords[i]
            val wordTextView = TextView(taskContainer.context).apply {
                text = wordPair.first
                textSize = 16f
                setTextColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
                setPadding(16, 16, 16, 16)
                tag = wordPair.first
                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(taskContainer.context, R.color.red))
                    cornerRadius = 8f
                }

                val params = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val margin = dpToPx(4, taskContainer.context)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
                minWidth = dpToPx(70, taskContainer.context)
                isSingleLine = false
            }

            wordTextView.setOnLongClickListener { view ->
                val clipData = ClipData.newPlainText("", "")
                val dragShadowBuilder = View.DragShadowBuilder(view)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    view.startDragAndDrop(clipData, dragShadowBuilder, view, 0)
                } else {
                    view.startDrag(clipData, dragShadowBuilder, view, 0)
                }
                view.visibility = View.INVISIBLE
                true
            }

            wordTextView.setOnClickListener { clickedView ->
                if (clickedView.parent is ViewGroup) {
                    val wordText = (clickedView as TextView).text.toString()

                    val wordsLayoutLocal =
                        taskContainer.children.find { it is FlexboxLayout } as? FlexboxLayout

                    wordsLayoutLocal?.let {
                        val newWordTextView = TextView(taskContainer.context).apply {
                            text = wordText
                            textSize = 16f
                            setTextColor(
                                ContextCompat.getColor(
                                    taskContainer.context,
                                    R.color.colorPrimary
                                )
                            )
                            setPadding(16, 16, 16, 16)
                            tag = wordText
                            background = GradientDrawable().apply {
                                setColor(ContextCompat.getColor(taskContainer.context, R.color.red))
                                cornerRadius = 8f
                            }
                            val params = FlexboxLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            val margin = dpToPx(4, taskContainer.context)
                            params.setMargins(margin, margin, margin, margin)
                            layoutParams = params
                            minWidth = dpToPx(70, taskContainer.context)
                            isSingleLine = false
                            setOnLongClickListener { view ->
                                val clipData = ClipData.newPlainText("", "")
                                val dragShadowBuilder = View.DragShadowBuilder(view)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    view.startDragAndDrop(clipData, dragShadowBuilder, view, 0)
                                } else {
                                    view.startDrag(clipData, dragShadowBuilder, view, 0)
                                }
                                view.visibility = View.INVISIBLE
                                true
                            }
                        }

                        it.addView(newWordTextView)
                    }

                    (clickedView.parent as ViewGroup).removeView(clickedView)
                    android.util.Log.d(
                        "ClickToRemove",
                        "Clicked and returned word: $wordText to the word list."
                    )
                }
            }

            wordsLayout.addView(wordTextView)
        }


    } catch (e: Exception) {
        val errorTextView = TextView(taskContainer.context).apply {
            text = "Error processing the task."
            textSize = 18f
            setPadding(16, 16, 16, 0)
        }
        taskContainer.addView(errorTextView)
    }
}

fun calculateCategorizationScore(
    task: TaskLocal,
    inputFields: List<View>
): Double {
    Log.d("CategorizationScore", "Začiatok calculateCategorizationScore")
    Log.d("CategorizationScore", "Počet inputFields: ${inputFields.size}")
    inputFields.forEachIndexed { index, view ->
        Log.d(
            "CategorizationScore",
            "InputField [$index]: Tag=${view.tag}, Class=${view.javaClass.simpleName}"
        )
        if (view is LinearLayout) {
            Log.d("CategorizationScore", "  Počet detí v LinearLayout [$index]: ${view.childCount}")
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                Log.d(
                    "CategorizationScore",
                    "    Dieťa [$i]: Class=${child.javaClass.simpleName}, Text=${(child as? TextView)?.text}, Tag=${child.tag}"
                )
            }
        }
    }

    var totalScore = 0.0

    val json = task.data.toString()
    Log.d("CategorizationScore", "JSON dáta z taskLocal: $json")

    if (json == null) {
        Log.e("CategorizationScore", "JSON dáta sú null, skóre bude 0.")
        return 0.0
    }

    try {
        val jsonObject = JSONObject(json)
        val categoriesObject = jsonObject.getJSONObject("categories")
        val expectedCategorization = mutableMapOf<String, MutableList<Pair<String, Double>>>()

        val categoryNames = categoriesObject.keys()
        while (categoryNames.hasNext()) {
            val categoryName = categoryNames.next()
            val categoryData = categoriesObject.getJSONObject(categoryName)
            val name = categoryData.getString("name")
            val wordsArray = categoryData.getJSONArray("words")
            val wordPointsList = mutableListOf<Pair<String, Double>>()
            for (i in 0 until wordsArray.length()) {
                val wordObject = wordsArray.getJSONObject(i)
                val text = wordObject.getString("text")
                val points = wordObject.getDouble("points")
                wordPointsList.add(text to points)
            }
            expectedCategorization[name] = wordPointsList
            Log.d(
                "CategorizationScore",
                "Načítaná kategória: $categoryName, názov: $name, slová: $wordPointsList"
            )
        }
        Log.d("CategorizationScore", "expectedCategorization: $expectedCategorization")

        val userCategorizationMap = mutableMapOf<String, MutableList<String>>()
        inputFields.forEach { categoryLayout ->
            if (categoryLayout is LinearLayout) {
                val categoryName = categoryLayout.tag as? String ?: ""
                Log.d("CategorizationScore", "Spracovávam kategóriu používateľa: $categoryName")
                val wordsInCategory = mutableListOf<String>()
                for (i in 0 until categoryLayout.childCount) {
                    val child = categoryLayout.getChildAt(i)
                    if (child is TextView) {
                        val userWord = child.text.toString()
                        wordsInCategory.add(userWord)
                        Log.d(
                            "CategorizationScore",
                            "  Používateľ priradil slovo '$userWord' do kategórie '$categoryName'"
                        )
                    }
                }
                userCategorizationMap[categoryName] = wordsInCategory
            }
        }
        Log.d("CategorizationScore", "userCategorizationMap: $userCategorizationMap")

        expectedCategorization.forEach { (correctCategoryName, expectedWordPoints) ->
            val userWordsInCategory =
                userCategorizationMap[correctCategoryName]?.toMutableList() ?: mutableListOf()
            val expectedWordsInCategory = expectedWordPoints.toMutableList()

            expectedWordsInCategory.forEach { (expectedWord, points) ->
                val index = userWordsInCategory.indexOf(expectedWord)
                if (index != -1) {
                    totalScore += points
                    userWordsInCategory.removeAt(index)
                    Log.d(
                        "CategorizationScore",
                        "Slovo '$expectedWord' bolo správne priradené do kategórie '$correctCategoryName', získané body: $points, aktuálne skóre: $totalScore"
                    )
                } else {
                    Log.d(
                        "CategorizationScore",
                        "Slovo '$expectedWord' malo byť v kategórii '$correctCategoryName', ale nebolo tam nájdené."
                    )
                }
            }
        }

        Log.d("CategorizationScore", "Celkové vypočítané skóre: $totalScore")

    } catch (e: Exception) {
        Log.e("CategorizationScore", "Chyba pri spracovaní JSON pre bodovanie: ${e.message}", e)
    }

    Log.d("CategorizationScore", "Koniec calculateCategorizationScore, vraciam skóre: $totalScore")
    return totalScore
}

