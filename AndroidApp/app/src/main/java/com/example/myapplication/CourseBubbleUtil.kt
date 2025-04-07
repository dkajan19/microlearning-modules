import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import androidx.cardview.widget.CardView
import com.example.myapplication.Course
import com.example.myapplication.R

object CourseBubbleUtil {

    fun createCourseBubble(course: Course, context: Context): CardView {
        val courseCardView = CardView(context).apply {
            radius = 16f
            setCardBackgroundColor(Color.WHITE)
            cardElevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(40, 16, 40, 16) }

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(8, Color.parseColor(course.area_color))
                cornerRadius = 16f
            }
        }

        val courseLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val courseNameTextView = TextView(context).apply {
            text = course.name.ifEmpty { "Name not available" }
            textSize = 18f
            setTextColor(Color.parseColor(course.area_color))
            setTypeface(null, Typeface.BOLD)
        }

        val courseDescriptionTextView = TextView(context).apply {
            text = course.description?.ifEmpty { "Description not available" }
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.ITALIC)
        }

        val statsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 16) }
        }

        statsContainer.addView(
            createStatBlock(
                R.drawable.baseline_article_24,
                course.content_passed,
                course.content_count,
                course.area_color,
                context
            )
        )
        statsContainer.addView(
            createStatBlock(
                R.drawable.baseline_question_mark_24,
                course.task_passed,
                course.task_count,
                course.area_color,
                context
            )
        )
        statsContainer.addView(
            createStatBlock(
                R.drawable.outline_code_24,
                course.program_passed,
                course.program_count,
                course.area_color,
                context
            )
        )

        val progressContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
        }

        val progressBar =
            ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                layoutParams = LinearLayout.LayoutParams(0, 30, 2f).apply { setMargins(0, 0, 8, 0) }
                progressTintList = ColorStateList.valueOf(Color.parseColor(course.area_color))
                progressBackgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(course.area_color))
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(4, Color.BLACK)
                    cornerRadius = 5f
                }
                clipToOutline = true
            }

        val progressPercentageTextView = TextView(context).apply {
            text = "${course.progress}%"
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        progressContainer.addView(progressBar)
        progressContainer.addView(progressPercentageTextView)

        animateProgressBar(progressBar, course.progress)

        courseLayout.addView(courseNameTextView)
        courseLayout.addView(courseDescriptionTextView)
        courseLayout.addView(statsContainer)
        courseLayout.addView(progressContainer)
        courseCardView.addView(courseLayout)

        return courseCardView
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

    private fun createStatBlock(
        iconResId: Int,
        passed: Int,
        total: Int,
        color: String,
        context: Context
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val icon = ImageView(context).apply {
                setImageResource(iconResId)
                setColorFilter(Color.parseColor(color))
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { gravity = Gravity.CENTER }
            }
            val statText = TextView(context).apply {
                text = "$passed / $total"
                textSize = 14f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            addView(icon)
            addView(statText)
        }
    }
}
