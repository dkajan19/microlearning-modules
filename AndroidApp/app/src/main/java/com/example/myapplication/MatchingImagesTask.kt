package com.example.myapplication

import ApiServiceLocal
import TaskLocal
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class MatchingImageView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var pairs: List<Pair<String, String>> = emptyList()
    private var points: List<Double> = emptyList()
    private var correctMatches: List<String> = emptyList()
    private val imageBitmaps = mutableListOf<Bitmap>()
    private val scaledImageBitmaps = mutableListOf<Bitmap>()
    private val wordRects = mutableListOf<RectF>()
    private val imageRects = mutableListOf<RectF>()
    private val connections = mutableListOf<Pair<Int, Int>>()
    private var shuffledWords: List<String> = emptyList()

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }

    private val imagePaint = Paint()
    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }
    private val correctLinePaint = Paint(linePaint).apply {
        color = Color.GREEN
    }
    private val incorrectLinePaint = Paint(linePaint).apply {
        color = Color.RED
    }
    private val dotPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.red)
        style = Paint.Style.FILL
    }
    private val dotRadius = 15f
    private val borderThickness = 8f
    private val fixedImageSize = 150

    private var currentPath: Path? = null
    private var startPoint: PointF? = null
    private var selectedImageIndex: Int? = null
    private var selectedWordIndex: Int? = null

    private val touchTolerance = 50f
    private val lineClickTolerance = 40f

    private var connectionMade = false
    private var isDragging = false

    fun setData(
        pairs: List<Pair<String, String>>,
        points: List<Double>,
        correctMatches: List<String>
    ) {
        this.pairs = pairs
        this.points = points
        this.correctMatches = correctMatches
        this.shuffledWords = pairs.map { it.first }.shuffled()
        imageBitmaps.clear()
        scaledImageBitmaps.clear()

        for (pair in pairs) {
            try {
                val imageDataBytes = Base64.decode(
                    pair.second.substring(pair.second.indexOf(",") + 1),
                    Base64.DEFAULT
                )
                val originalBitmap =
                    BitmapFactory.decodeByteArray(imageDataBytes, 0, imageDataBytes.size)
                imageBitmaps.add(originalBitmap)
                scaledImageBitmaps.add(createSquaredBitmapWithBorder(originalBitmap))
            } catch (e: Exception) {
                Log.e("MatchingImageView", "Error decoding base64 image: ${e.message}")
                val placeholderBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                imageBitmaps.add(placeholderBitmap)
                scaledImageBitmaps.add(createSquaredBitmapWithBorder(placeholderBitmap))
            }
        }
        requestLayout()
    }

    private fun createSquaredBitmapWithBorder(sourceBitmap: Bitmap): Bitmap {
        val outputSize = fixedImageSize + 2 * borderThickness
        val scaledBitmap =
            Bitmap.createScaledBitmap(sourceBitmap, fixedImageSize, fixedImageSize, true)

        val roundedBitmap =
            Bitmap.createBitmap(outputSize.toInt(), outputSize.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(roundedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rect = RectF(0f, 0f, outputSize.toFloat(), outputSize.toFloat())
        val cornerRadius = 30f
        paint.color = ContextCompat.getColor(context, R.color.colorPrimary)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        val imageRect = RectF(
            borderThickness,
            borderThickness,
            (outputSize - borderThickness),
            (outputSize - borderThickness)
        )
        val path =
            Path().apply { addRoundRect(imageRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawBitmap(scaledBitmap, borderThickness, borderThickness, null)

        return roundedBitmap
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = (pairs.size * (scaledImageBitmaps.firstOrNull()?.height
            ?: fixedImageSize) * 1.5).toFloat()
        setMeasuredDimension(desiredWidth, desiredHeight.toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        imageRects.clear()
        wordRects.clear()
        val imageStartX = 50f
        val wordStartX = width - 150f
        val verticalSpacing = height / (pairs.size + 1).toFloat()

        for (i in pairs.indices) {
            val bitmap = scaledImageBitmaps[i]
            val imageY = verticalSpacing * (i + 1) - bitmap.height / 2
            imageRects.add(
                RectF(
                    imageStartX,
                    imageY,
                    imageStartX + bitmap.width,
                    imageY + bitmap.height
                )
            )
        }

        for (i in shuffledWords.indices) {
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textY = verticalSpacing * (i + 1) + textHeight / 2
            val textWidth = textPaint.measureText(shuffledWords[i])
            wordRects.add(RectF(wordStartX, textY - textHeight, wordStartX + textWidth, textY))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in scaledImageBitmaps.indices) {
            canvas.drawBitmap(
                scaledImageBitmaps[i],
                imageRects[i].left,
                imageRects[i].top,
                imagePaint
            )
            canvas.drawCircle(
                imageRects[i].right + dotRadius * 2,
                imageRects[i].centerY(),
                dotRadius,
                dotPaint
            )
        }

        for (i in shuffledWords.indices) {
            val textHeight = textPaint.descent() - textPaint.ascent()
            val textY = (height / (pairs.size + 1).toFloat()) * (i + 1) + textHeight / 2
            canvas.drawText(shuffledWords[i], wordRects[i].centerX(), textY, textPaint)
            canvas.drawCircle(
                wordRects[i].left - dotRadius * 2,
                wordRects[i].centerY(),
                dotRadius,
                dotPaint
            )
        }

        currentPath?.let {
            canvas.drawPath(it, linePaint)
        }

        for (connection in connections) {
            val imageIndex = connection.first
            val wordIndex = connection.second
            if (imageIndex in imageRects.indices && wordIndex in wordRects.indices) {
                val startX = imageRects[imageIndex].right + dotRadius * 2
                val startY = imageRects[imageIndex].centerY()
                val endX = wordRects[wordIndex].left - dotRadius * 2
                val endY = wordRects[wordIndex].centerY()

                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startPoint = PointF(event.x, event.y)
                selectedImageIndex = findTouchedImage(startPoint!!)
                selectedWordIndex = findTouchedWord(startPoint!!)

                val touchedConnectionIndex = findTouchedConnection(startPoint!!)
                if (touchedConnectionIndex != -1) {
                    connections.removeAt(touchedConnectionIndex)
                    invalidate()
                    return true
                }

                if (selectedImageIndex != null) {
                    currentPath = Path().apply {
                        moveTo(startPoint!!.x, startPoint!!.y)
                    }
                    invalidate()
                    isDragging = true
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    startPoint?.let {
                        currentPath?.lineTo(event.x, event.y)
                        invalidate()
                    }
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    startPoint?.let {
                        val endPoint = PointF(event.x, event.y)
                        val releasedImageIndex = findTouchedImage(endPoint)
                        val releasedWordIndex = findTouchedWord(endPoint)

                        val isWordAlreadyConnected: (Int) -> Boolean = { wordIndex ->
                            connections.any { it.second == wordIndex }
                        }

                        val isImageAlreadyConnected: (Int) -> Boolean = { imageIndex ->
                            connections.any { it.first == imageIndex }
                        }

                        if (selectedImageIndex != null && releasedWordIndex != null) {
                            if (!isWordAlreadyConnected(releasedWordIndex) && !isImageAlreadyConnected(
                                    selectedImageIndex!!
                                )
                            ) {
                                connections.add(Pair(selectedImageIndex!!, releasedWordIndex!!))
                                connectionMade = true
                            }
                        } else if (selectedWordIndex != null && releasedImageIndex != null) {
                            if (!isWordAlreadyConnected(selectedWordIndex!!) && !isImageAlreadyConnected(
                                    releasedImageIndex!!
                                )
                            ) {
                                connections.add(Pair(releasedImageIndex!!, selectedWordIndex!!))
                                connectionMade = true
                            }
                        }

                        currentPath = null
                        startPoint = null
                        selectedImageIndex = null
                        selectedWordIndex = null
                        invalidate()
                        isDragging = false
                        parent.requestDisallowInterceptTouchEvent(false)
                        return true
                    }
                }
                isDragging = false
                parent.requestDisallowInterceptTouchEvent(false)
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    currentPath = null
                    startPoint = null
                    selectedImageIndex = null
                    selectedWordIndex = null
                    invalidate()
                    isDragging = false
                    parent.requestDisallowInterceptTouchEvent(false)
                    return true
                }
                isDragging = false
                parent.requestDisallowInterceptTouchEvent(false)
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedImage(point: PointF): Int? {
        for (i in imageRects.indices) {
            val dotX = imageRects[i].right + dotRadius * 2
            val dotY = imageRects[i].centerY()
            if (abs(point.x - dotX) < touchTolerance && abs(point.y - dotY) < touchTolerance) {
                return i
            }
        }
        return null
    }

    private fun findTouchedWord(point: PointF): Int? {
        for (i in shuffledWords.indices) {
            if (i < wordRects.size) {
                val rect = wordRects[i]
                val dotX = rect.left - dotRadius * 2
                val dotY = rect.centerY()
                if (abs(point.x - dotX) < touchTolerance && abs(point.y - dotY) < touchTolerance) {
                    return i
                }
            }
        }
        return null
    }

    private fun findTouchedConnection(point: PointF): Int {
        for (i in connections.indices) {
            val connection = connections[i]
            val imageIndex = connection.first
            val wordIndex = connection.second

            if (imageIndex in imageRects.indices && wordIndex in wordRects.indices) {
                val startX = imageRects[imageIndex].right + dotRadius * 2
                val startY = imageRects[imageIndex].centerY()
                val endX = wordRects[wordIndex].left - dotRadius * 2
                val endY = wordRects[wordIndex].centerY()

                if (isPointOnLine(
                        point.x,
                        point.y,
                        startX,
                        startY,
                        endX,
                        endY,
                        lineClickTolerance
                    )
                ) {
                    return i
                }
            }
        }
        return -1
    }

    private fun isPointOnLine(
        px: Float, py: Float,
        lineStartX: Float, lineStartY: Float,
        lineEndX: Float, lineEndY: Float,
        tolerance: Float
    ): Boolean {
        val d1 = sqrt((px - lineStartX).pow(2) + (py - lineStartY).pow(2))
        val d2 = sqrt((px - lineEndX).pow(2) + (py - lineEndY).pow(2))
        val lineLength = sqrt((lineEndX - lineStartX).pow(2) + (lineEndY - lineStartY).pow(2))

        return abs(d1 + d2 - lineLength) < tolerance
    }

    fun getScore(): Double {
        var score = 0.0
        for (connection in connections) {
            val imageIndex = connection.first
            val wordIndex = connection.second

            if (imageIndex in imageRects.indices && wordIndex in shuffledWords.indices) {
                Log.d(
                    "EvaluationDetails",
                    "Connection: ImageIndex = $imageIndex, WordIndex (Shuffled) = $wordIndex"
                )
                Log.d("EvaluationDetails", "Connected Word (Shuffled): ${shuffledWords[wordIndex]}")
                Log.d(
                    "EvaluationDetails",
                    "Correct Match for Image at $imageIndex: ${correctMatches[imageIndex]}"
                )

                if (shuffledWords[wordIndex] == correctMatches[imageIndex]) {
                    Log.d(
                        "PairMatched",
                        "Match found! Word: ${shuffledWords[wordIndex]}, Image: ${correctMatches[imageIndex]}"
                    )
                    score += points.getOrNull(imageIndex) ?: 0.0
                } else {
                    Log.d(
                        "PairMismatch",
                        "Mismatch! Word: ${shuffledWords[wordIndex]}, Image: ${correctMatches[imageIndex]}"
                    )
                }
            }
        }
        Log.d("FinalScore", "Score: $score")
        return score
    }
}

data class MatchingItem(
    val word: String,
    val image: String,
    val points: Double
)

fun displayMatchingImages(
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
        val pairsArray = jsonObject.getJSONArray("pairs")
        val matchingItems = mutableListOf<MatchingItem>()

        for (i in 0 until pairsArray.length()) {
            val pairObject = pairsArray.getJSONObject(i)
            val word = pairObject.getString("word")
            val image = pairObject.getString("image")
            val point = pairObject.getDouble("points")
            matchingItems.add(MatchingItem(word, image, point))
        }

        Log.d(
            "MatchingItemsBeforeShuffle",
            matchingItems.joinToString(", ") { "${it.word} - ${it.image}" })

        matchingItems.shuffle()

        Log.d(
            "MatchingItemsAfterShuffle",
            matchingItems.joinToString(", ") { "${it.word} - ${it.image}" })

        val shuffledPairs = matchingItems.map { Pair(it.word, it.image) }
        val shuffledCorrectAnswersList = matchingItems.map { it.word }
        val shuffledPoints = matchingItems.map { it.points }

        Log.d("ShuffledPairs", shuffledPairs.joinToString(", ") { "${it.first} - ${it.second}" })
        Log.d("ShuffledCorrectAnswers", shuffledCorrectAnswersList.joinToString(", ") { it })
        Log.d("ShuffledPoints", shuffledPoints.joinToString(", ") { it.toString() })

        val helperTextView = TextView(taskContainer.context).apply {
            text = "Match the images with the correct words."
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 32, 16, 16)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.bottomMargin = -20
            this.layoutParams = layoutParams
        }
        taskContainer.addView(helperTextView)

        val scrollView = androidx.core.widget.NestedScrollView(taskContainer.context)
        val matchingImageView = MatchingImageView(taskContainer.context)
        matchingImageView.setData(shuffledPairs, shuffledPoints, shuffledCorrectAnswersList)
        scrollView.addView(
            matchingImageView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        taskContainer.addView(
            scrollView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        inputFields.clear()
        inputFields.add(matchingImageView)

        correctAnswers.clear()
        correctAnswers.addAll(shuffledCorrectAnswersList)

        pointsList.clear()
        pointsList.addAll(shuffledPoints)

    } catch (e: Exception) {
        val errorTextView = TextView(taskContainer.context).apply {
            text = "Error processing the task."
            textSize = 18f
            setPadding(16, 16, 16, 0)
        }
        taskContainer.addView(errorTextView)
        Log.e("TaskProcessingError", "Error processing task: ${e.message}")
    }
}

fun calculateMatchingImagesScore(
    inputFields: List<View>,
    correctAnswers: List<String>,
    pointsList: List<Double>
): Double {
    if (inputFields.isNotEmpty() && inputFields[0] is MatchingImageView) {
        return (inputFields[0] as MatchingImageView).getScore()
    }
    return 0.0
}