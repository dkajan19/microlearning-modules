import android.content.Context
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.R

fun AppCompatActivity.checkConnectivity() {
    val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = manager.activeNetworkInfo

    if (activeNetwork == null || !activeNetwork.isConnected) {
        val titleView = TextView(this)
        titleView.text = "No Internet Connection"
        titleView.gravity = Gravity.CENTER
        titleView.setPadding(20, 20, 20, 20)
        titleView.textSize = 20F
        titleView.setTypeface(Typeface.DEFAULT_BOLD)
        titleView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        titleView.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        val alertDialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setMessage("Make sure that WI-FI or mobile data is turned on, then try again")
            .setCancelable(false)
            .setPositiveButton("Retry") { dialog, which ->
                recreate()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                finish()
            }
            .create()

        alertDialog.show()

        val messageView = alertDialog.findViewById<TextView>(android.R.id.message)
        messageView?.gravity = Gravity.CENTER
        messageView?.setTextColor(ContextCompat.getColor(this, android.R.color.black))

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        positiveButton.setTextColor(ContextCompat.getColor(this, R.color.red))
        negativeButton.setTextColor(ContextCompat.getColor(this, R.color.red))

        positiveButton.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.button_color_selector)
        negativeButton.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.button_color_selector)
    }
}

fun AppCompatActivity.isConnected(): Boolean {
    val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = manager.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}