package com.example.myapplication.floating

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageView
import com.example.myapplication.R
import org.json.JSONArray

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var floatingImageView: ImageView
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var petType: String = "書摘"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("FloatingService", "onCreate called")
        setupFloatingWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("FloatingService", "onStartCommand called")
        petType = intent?.getStringExtra("pet_type") ?: "書摘"
        updateFloatingImage()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_widget, null)
        floatingImageView = floatingView.findViewById(R.id.floating_image)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 100

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (isClick(deltaX, deltaY)) {
                        Log.d("FloatingService", "Detected click - returning to app")
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    true
                }

                else -> false
            }
        }

        windowManager.addView(floatingView, layoutParams)
    }

    private fun isClick(dx: Float, dy: Float): Boolean {
        // 視移動距離小於 10px 為點擊（平方距離 < 100）
        return dx * dx + dy * dy < 100
    }

    private fun updateFloatingImage() {
        if (!::floatingImageView.isInitialized) return

        val sharedPref = getSharedPreferences("upload_records", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("records", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)

        val subtypeMap = mapOf(
            "書摘" to listOf("文學", "自然", "歷史", "藝術"),
            "景點" to listOf("人文景", "自然景", "餐廳"),
            "食譜" to listOf("中式", "西式", "東南亞"),
            "電影" to listOf("喜劇", "悲劇", "恐怖片", "愛情片")
        )

        val validSubtypes = subtypeMap[petType] ?: emptyList()
        val counts = mutableMapOf<String, Int>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val subtype = obj.optString("subtype")
            if (subtype in validSubtypes) {
                counts[subtype] = counts.getOrDefault(subtype, 0) + 1
            }
        }

        val drawableName = when {
            counts.count { it.value >= 4 } >= 2 -> {
                val topTwo = counts.entries.sortedByDescending { it.value }.take(2)
                    .map { convertSubtypeToCode(it.key) }
                "${getPrefix(petType)}_${topTwo[0]}_${topTwo[1]}"
            }

            counts.any { it.value >= 2 } -> {
                val topOne = counts.maxByOrNull { it.value }?.key
                "${getPrefix(petType)}_${convertSubtypeToCode(topOne ?: "")}"
            }

            else -> "${getPrefix(petType)}0"
        }

        val resId = resources.getIdentifier(drawableName, "drawable", packageName)
        Log.d("FloatingService", "Setting image: $drawableName (resId=$resId)")
        floatingImageView.setImageResource(if (resId != 0) resId else R.drawable.bear0)
    }

    private fun getPrefix(petType: String): String {
        return when (petType) {
            "書摘" -> "book"
            "食譜" -> "recipe"
            "電影" -> "movie"
            "景點" -> "landmark"
            else -> "book"
        }
    }

    private fun convertSubtypeToCode(subtype: String): String {
        return when (subtype) {
            "文學" -> "lit"
            "自然" -> "nat"
            "歷史" -> "hist"
            "藝術" -> "art"
            "人文景" -> "cult"
            "自然景" -> "land"
            "餐廳" -> "food"
            "中式" -> "chi"
            "西式" -> "wes"
            "東南亞" -> "sea"
            "喜劇" -> "com"
            "悲劇" -> "trag"
            "恐怖片" -> "horr"
            "愛情片" -> "rom"
            else -> "unk"
        }
    }

    companion object {
        fun updateImage(context: Context, petType: String) {
            val intent = Intent(context, FloatingService::class.java)
            intent.putExtra("pet_type", petType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Settings.canDrawOverlays(context)) {
                Log.w("FloatingService", "Overlay permission not granted")
                return
            }
            context.startService(intent)
        }
    }
}
