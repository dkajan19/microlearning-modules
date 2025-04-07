import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.DragEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.dpToPx
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import android.view.Gravity
import android.widget.LinearLayout.LayoutParams
import java.util.Collections

fun displayMatchingDefinitions(
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
        val jsonObject = JSONObject(json)

        /*
        val pairs = jsonObject.getJSONArray("pairs")
        val words = mutableListOf<String>()
        val definitions = mutableListOf<String>()
        val originalWordsViews = mutableListOf<TextView>()

        for (i in 0 until pairs.length()) {
            val pair = pairs.getJSONObject(i)
            words.add(pair.getString("word"))
            definitions.add(pair.getString("definition"))
            pointsList.add(pair.getDouble("points"))
            correctAnswers.add(pair.getString("word"))
        }
        */

        val pairsJsonArray = jsonObject.getJSONArray("pairs")
        val pairsList = mutableListOf<JSONObject>()
        for (i in 0 until pairsJsonArray.length()) {
            pairsList.add(pairsJsonArray.getJSONObject(i))
        }
        Collections.shuffle(pairsList)

        val words = mutableListOf<String>()
        val definitions = mutableListOf<String>()
        val originalWordsViews = mutableListOf<TextView>()
        pointsList.clear()
        correctAnswers.clear()

        for (i in 0 until pairsList.size) {
            val pair = pairsList[i]
            words.add(pair.getString("word"))
            definitions.add(pair.getString("definition"))
            pointsList.add(pair.getDouble("points"))
            correctAnswers.add(pair.getString("word"))
        }

        val helperTextView = TextView(taskContainer.context).apply {
            text = "Match the correct words with their definitions."
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 32, 16, 16)
        }
        taskContainer.addView(helperTextView)

        val mainLayout = LinearLayout(taskContainer.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16, taskContainer.context)
            }
        }

        val definitionsLayout = LinearLayout(taskContainer.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }

        val wordsLayout = FlexboxLayout(taskContainer.context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(16, taskContainer.context)
                bottomMargin = dpToPx(16, taskContainer.context)
            }
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }

        val shuffledWords = words.shuffled()

        for (i in definitions.indices) {
            val definitionRow = LinearLayout(taskContainer.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(16, taskContainer.context)
                }
                gravity = Gravity.CENTER_VERTICAL
                weightSum = 100f
            }

            val definitionTextView = TextView(taskContainer.context).apply {
                text = "${definitions[i]}"
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(16, 16, 0, 16)
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    40f
                )
                gravity = Gravity.LEFT
            }

            val hyphenTextView = TextView(taskContainer.context).apply {
                text = " - "
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(0, 16, 0, 16)
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    20f
                )
                gravity = Gravity.CENTER
            }

            val dropTargetTextView = TextView(taskContainer.context).apply {
                text = ""
                textSize = 18f
                setTextColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
                setPadding(8, 16, 16, 16)
                tag = i
                layoutParams = LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    40f
                )
                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
                    cornerRadius = 8f
                }
                gravity = Gravity.CENTER_VERTICAL and Gravity.LEFT
                minWidth = dpToPx(80, taskContainer.context)
                setSingleLine(false)
                setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DROP -> {
                            val draggedView = event.localState as View
                            val draggedText = (draggedView.tag as String)
                            val targetIndex = (v.tag as Int)

                            val previousText = (v as TextView).text.toString()
                            if (previousText.isNotEmpty()) {
                                originalWordsViews.find { it.tag == previousText }?.visibility =
                                    View.VISIBLE
                            }

                            (v as TextView).text = draggedText
                            v.setTextColor(
                                ContextCompat.getColor(
                                    taskContainer.context,
                                    R.color.colorPrimary
                                )
                            )
                            v.background = GradientDrawable().apply {
                                setColor(ContextCompat.getColor(taskContainer.context, R.color.red))
                                cornerRadius = 8f
                            }
                            draggedView.visibility = View.GONE
                            true
                        }

                        DragEvent.ACTION_DRAG_ENDED -> {
                            val draggedView = event.localState as? View
                            if (draggedView?.visibility != View.GONE) {
                                draggedView?.visibility = View.VISIBLE
                            }
                            true
                        }

                        DragEvent.ACTION_DRAG_ENTERED -> {
                            v.background = GradientDrawable().apply {
                                setColor(ContextCompat.getColor(taskContainer.context, R.color.red))
                                cornerRadius = 8f
                            }
                            true
                        }

                        DragEvent.ACTION_DRAG_EXITED -> {
                            val currentText = (v as TextView).text.toString()
                            if (currentText.isEmpty()) {
                                v.background = GradientDrawable().apply {
                                    setColor(
                                        ContextCompat.getColor(
                                            taskContainer.context,
                                            R.color.colorPrimary
                                        )
                                    )
                                    cornerRadius = 8f
                                }
                            }
                            true
                        }

                        else -> true
                    }
                }

                setOnClickListener { clickedView ->
                    val currentText = (clickedView as TextView).text.toString()
                    if (currentText.isNotEmpty()) {
                        originalWordsViews.find { it.tag == currentText }?.visibility = View.VISIBLE
                        clickedView.text = ""
                        clickedView.background = GradientDrawable().apply {
                            setColor(
                                ContextCompat.getColor(
                                    taskContainer.context,
                                    R.color.colorPrimary
                                )
                            )
                            cornerRadius = 8f
                        }
                        clickedView.setTextColor(
                            ContextCompat.getColor(
                                taskContainer.context,
                                R.color.colorPrimary
                            )
                        )
                    }
                }
            }

            definitionRow.addView(definitionTextView)
            definitionRow.addView(hyphenTextView)
            definitionRow.addView(dropTargetTextView)
            definitionsLayout.addView(definitionRow)
            inputFields.add(dropTargetTextView)
        }

        for (word in shuffledWords) {
            val wordTextView = TextView(taskContainer.context).apply {
                text = word
                textSize = 18f
                setTextColor(ContextCompat.getColor(taskContainer.context, R.color.colorPrimary))
                setPadding(16, 16, 16, 16)
                tag = word
                background = GradientDrawable().apply {
                    setColor(ContextCompat.getColor(taskContainer.context, R.color.red))
                    cornerRadius = 8f
                }
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8, taskContainer.context)
                    bottomMargin = dpToPx(8, taskContainer.context)
                    minWidth = dpToPx(70, taskContainer.context)
                }
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
            wordsLayout.addView(wordTextView)
            originalWordsViews.add(wordTextView)
        }

        mainLayout.addView(definitionsLayout)
        mainLayout.addView(wordsLayout)
        taskContainer.addView(mainLayout)
    } catch (e: Exception) {
        val errorTextView = TextView(taskContainer.context).apply {
            text = "Error processing the task."
            textSize = 18f
            setPadding(16, 16, 16, 0)
        }
        taskContainer.addView(errorTextView)
    }
}

fun calculateMatchingDefinitionsScore(
    inputFields: List<View>,
    correctAnswers: List<String>,
    pointsList: List<Double>
): Double {
    var userPoints = 0.0
    for (i in inputFields.indices) {
        val definitionTextView = inputFields[i] as TextView
        val userWord = definitionTextView.text.toString()

        if (userWord.isNotEmpty() && userWord == correctAnswers[i]) {
            userPoints += pointsList[i]
        }
    }
    return userPoints
}