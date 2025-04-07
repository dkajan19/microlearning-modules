package com.example.myapplication

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView


object ChapterBubbleUtil {

    var local = false

    fun createChapterBubbles(
        chapters: List<Chapter>,
        context: Context,
        areaColor: String,
        local: Boolean
    ) {
        val container =
            (context as? Activity)?.findViewById<LinearLayout>(R.id.chapters_container) ?: return

        this.local = local

        container.orientation = LinearLayout.VERTICAL
        //container.visibility = View.INVISIBLE

        val bubbles = mutableListOf<View>()

        Log.d(
            "DEBUG",
            "Original chapters: " + chapters.joinToString { it.chapter_name + " (" + it.chapter_order + ")" })

        val sortedChapters = chapters.sortedBy { it.chapter_order }

        Log.d(
            "DEBUG",
            "Sorted chapters: " + sortedChapters.joinToString { it.chapter_name + " (" + it.chapter_order + ")" })


        for (i in sortedChapters.indices step 2) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                weightSum = 2f
            }

            val bubble1 = createSingleChapterBubble(sortedChapters[i], context, areaColor, true)
            row.addView(bubble1)
            bubbles.add(bubble1)

            val bubble2 = if (i + 1 < chapters.size) {
                createSingleChapterBubble(sortedChapters[i + 1], context, areaColor, false)
            } else {
                View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
            }
            row.addView(bubble2)

            if (bubble2 is View) bubbles.add(bubble2)

            if (i + 1 >= chapters.size) {
                val params = bubble1.layoutParams as LinearLayout.LayoutParams
                params.width = LinearLayout.LayoutParams.MATCH_PARENT
                //params.height = LinearLayout.LayoutParams.WRAP_CONTENT
                bubble1.layoutParams = params
            }

            container.addView(row)
        }

        container.post {
            val maxHeight = bubbles.filter { it.visibility == View.VISIBLE }.maxOfOrNull { bubble ->
                val rect = Rect()
                bubble.getDrawingRect(rect)
                rect.height()
            } ?: 0

            bubbles.forEachIndexed() { index, bubble ->
                val params = bubble.layoutParams as LinearLayout.LayoutParams
                if (bubble.visibility == View.VISIBLE && (bubble.layoutParams.width != LinearLayout.LayoutParams.MATCH_PARENT)) {
                    params.height = maxHeight
                } else {
                    params.marginEnd = 50
                    params.marginStart = 50
                }
                if (index == 0 || index == 1) {
                    params.setMargins(params.marginStart, 30, params.marginEnd, params.bottomMargin)
                }

                bubble.layoutParams = params
            }
        }

        //container.visibility = View.VISIBLE

    }


    private fun createSingleChapterBubble(
        chapter: Chapter,
        context: Context,
        areaColor: String,
        isFirstBubble: Boolean
    ): CardView {
        val card = CardView(context).apply {
            radius = 16f
            setCardBackgroundColor(Color.WHITE)
            cardElevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                if (isFirstBubble) {
                    setMargins(50, 16, 16, 16)
                } else {
                    setMargins(16, 16, 50, 16)
                }
            }

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(8, Color.parseColor(areaColor))
                cornerRadius = 16f
            }

            if (!local) {
                setOnClickListener {
                    openLessonsActivity(chapter, context)
                }
            } else {
                setOnClickListener {
                    val intent = Intent(context, LessonsLocalActivity::class.java).apply {
                        putExtra("CHAPTER_ID", chapter.chapter_id)
                    }
                    context.startActivity(intent)
                }
            }
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(context).apply {
            text = chapter.chapter_name
            textSize = 18f
            setTextColor(Color.parseColor(areaColor))
            setTypeface(null, Typeface.BOLD)
        }

        layout.addView(title)
        layout.addView(createOverallProgressBar(chapter, context, areaColor))
        layout.addView(createStatsContainer(chapter, context, areaColor))
        card.addView(layout)

        return card
    }

    private fun openLessonsActivity(chapter: Chapter, context: Context) {
        val intent = Intent(context, LessonsActivity::class.java).apply {
            putExtra("lessonID", chapter.chapter_id)
        }
        context.startActivity(intent)
    }


    private fun createOverallProgressBar(
        chapter: Chapter,
        context: Context,
        areaColor: String
    ): LinearLayout {
        val overallProgressContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 16) }
        }

        val overallProgressBar =
            ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                var totalTasks = 0
                var totalPrograms = 0
                var totalCompleted = 0

                if (local) {
                    totalTasks = chapter.tasks_nonfinished
                    totalPrograms = chapter.programs_nonfinished
                    totalCompleted = chapter.tasks_finished
                } else {
                    totalTasks = chapter.tasks_nonfinished + chapter.tasks_finished
                    totalPrograms = chapter.programs_nonfinished + chapter.programs_finished
                    totalCompleted = chapter.tasks_finished + chapter.programs_finished
                }

                val totalItems = totalTasks + totalPrograms
                val progressPercentage = if (totalItems > 0) {
                    (totalCompleted.toFloat() / totalItems) * 100
                } else {
                    0f
                }
                progress = progressPercentage.toInt()
                max = 100
                layoutParams = LinearLayout.LayoutParams(0, 30, 2f).apply { setMargins(0, 0, 8, 0) }
                progressTintList = ColorStateList.valueOf(Color.parseColor(areaColor))
                progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor(areaColor))
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(4, Color.BLACK)
                    cornerRadius = 5f
                }
                clipToOutline = true
            }

        val overallProgressPercentageTextView = TextView(context).apply {
            val progressText = "${overallProgressBar.progress}%"
            text = progressText
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        animateProgressBar(overallProgressBar, overallProgressBar.progress)

        overallProgressContainer.addView(overallProgressBar)
        overallProgressContainer.addView(overallProgressPercentageTextView)

        return overallProgressContainer
    }

    private fun createStatsContainer(
        chapter: Chapter,
        context: Context,
        areaColor: String
    ): LinearLayout {
        val statsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 16) }
        }

        val totalTasks =
            if (local) chapter.tasks_nonfinished else chapter.tasks_nonfinished + chapter.tasks_finished
        val totalPrograms =
            if (local) chapter.programs_nonfinished else chapter.programs_nonfinished + chapter.programs_finished

        statsContainer.addView(
            createBorderedSection(
                context, areaColor,
                createProgressBarWithIcon(
                    R.drawable.baseline_question_mark_24,
                    chapter.tasks_finished,
                    totalTasks,
                    areaColor,
                    context,
                    "${chapter.tasks_finished}/$totalTasks"
                ),
                16
            )
        )

        statsContainer.addView(
            createBorderedSection(
                context, areaColor,
                createProgressBarWithIcon(
                    R.drawable.outline_code_24,
                    chapter.programs_finished,
                    totalPrograms,
                    areaColor,
                    context,
                    "${chapter.programs_finished}/$totalPrograms"
                ),
                16
            )
        )


        return statsContainer
    }


    private fun createBorderedSection(
        context: Context,
        color: String,
        content: LinearLayout,
        margin: Int
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(margin, 0, margin, 0)
                }
            /*
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(8, Color.GRAY)
                cornerRadius = 8f
            }
            */
            //setPadding(32, 32, 32, 32)
            addView(content)
        }
    }

    private fun createProgressBarWithIcon(
        iconResId: Int,
        completed: Int,
        total: Int,
        color: String,
        context: Context,
        label: String
    ): LinearLayout {
        val progressLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = ImageView(context).apply {
            setImageResource(iconResId)
            setColorFilter(Color.parseColor(color))
            layoutParams =
                LinearLayout.LayoutParams(64, 64).apply { gravity = Gravity.CENTER }.apply {
                    topMargin = -50
                }

        }

        val progressPercentage = if (total > 0) {
            ((completed.toFloat() / total) * 100).toInt()
        } else {
            0
        }

        val semiCircleProgressBar =
            me.bastanfar.semicirclearcprogressbar.SemiCircleArcProgressBar(context).apply {
                setPercent(progressPercentage)
                //CoroutineScope(Dispatchers.Main).launch {
                //    delay(500)
                setPercentWithAnimation(progressPercentage)
                //}

                setProgressBarColor(Color.parseColor(color))
                val transparentColor = adjustTransparency(Color.parseColor(color), 128)
                setProgressPlaceHolderColor(transparentColor)
                setProgressBarWidth(30)
                setProgressPlaceHolderWidth(30)

                val progressBarWidth = 180
                val progressBarHeight = 100

                layoutParams = LinearLayout.LayoutParams(progressBarWidth, progressBarHeight)
            }


        val countLabel = TextView(context).apply {
            text = label
            textSize = 14f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }

        progressLayout.addView(semiCircleProgressBar)
        progressLayout.addView(icon)
        progressLayout.addView(countLabel)

        return progressLayout
    }

    fun adjustTransparency(color: Int, alpha: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        return Color.argb(alpha, red, green, blue)
    }

    private fun animateProgressBar(progressBar: ProgressBar, targetProgress: Int) {
        val animator = ValueAnimator.ofInt(0, targetProgress).apply {
            duration = 1000
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
        }
        animator.start()
    }


}