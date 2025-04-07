package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashLayout = LinearLayout(this)
        splashLayout.orientation = LinearLayout.VERTICAL
        splashLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL
        splashLayout.setBackgroundResource(R.drawable.splash_background)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(0, (resources.displayMetrics.heightPixels * 0.20).toInt(), 0, 0)
        splashLayout.layoutParams = layoutParams

        val faviconImageView = ImageView(this)
        faviconImageView.setImageResource(R.drawable.priscilla_logo)
        faviconImageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT * 10,
            LinearLayout.LayoutParams.WRAP_CONTENT * 10
        )

        val widthInDp = 250
        val heightInDp = 80

        val widthInPx = resources.displayMetrics.density * widthInDp
        val heightInPx = resources.displayMetrics.density * heightInDp

        faviconImageView.layoutParams = LinearLayout.LayoutParams(
            widthInPx.toInt(),
            heightInPx.toInt()
        )

        val textView1 = TextView(this)
        textView1.text = "Táto aplikácia bola vyvinutá ako súčasť diplomovej práce."
        textView1.textSize = 14f
        textView1.setTypeface(null, android.graphics.Typeface.BOLD)
        textView1.setTextColor(resources.getColor(android.R.color.black))
        textView1.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        val layoutParams1 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams1.setMargins(
            (resources.displayMetrics.heightPixels * 0.1).toInt(),
            (resources.displayMetrics.heightPixels * 0.05).toInt(),
            (resources.displayMetrics.heightPixels * 0.1).toInt(),
            20
        )
        textView1.layoutParams = layoutParams1

        val textView2 = TextView(this)
        textView2.text =
            "Aplikácia je určená výhradne na akademické a študijné účely. Nie je určená na komerčné využitie."
        textView2.textSize = 14f
        textView2.setTextColor(resources.getColor(android.R.color.black))
        textView2.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        val layoutParams2 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams2.setMargins(
            (resources.displayMetrics.heightPixels * 0.05).toInt(),
            0,
            (resources.displayMetrics.heightPixels * 0.05).toInt(),
            20
        )
        textView2.layoutParams = layoutParams2

        val copyrightTextView = TextView(this)
        copyrightTextView.text = "© Denis Kajan 2025. Všetky práva vyhradené."
        copyrightTextView.textSize = 12f
        copyrightTextView.setTextColor(resources.getColor(android.R.color.darker_gray))
        copyrightTextView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        progressBar.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        progressBar.setPadding(0, (resources.displayMetrics.heightPixels * 0.20).toInt(), 0, 0)

        progressBar.indeterminateDrawable.setColorFilter(
            resources.getColor(R.color.red),
            android.graphics.PorterDuff.Mode.SRC_IN
        )


        splashLayout.addView(faviconImageView)
        splashLayout.addView(textView1)
        splashLayout.addView(textView2)
        splashLayout.addView(copyrightTextView)
        splashLayout.addView(progressBar)

        setContentView(splashLayout)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 0)
    }
}
