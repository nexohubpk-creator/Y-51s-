package com.nexo.hondalauncher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.io.File
import kotlin.math.abs

class MainActivity : Activity() {

    class AppItem(val label: String, val pkg: String, val cls: String, val icon: Bitmap)

    private lateinit var root: FrameLayout
    private lateinit var wallpaper: ImageView
    private lateinit var drawer: FrameLayout
    private lateinit var grid: GridView
    private lateinit var detector: GestureDetector
    private val pickCode = 11

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = loadApps()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val match = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )

        root = FrameLayout(this)
        wallpaper = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        root.addView(wallpaper, match)

        // Clock + date
        val clockBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(70), dp(28), 0)
        }
        clockBox.addView(TextClock(this).apply {
            format12Hour = "hh:mm"
            format24Hour = "HH:mm"
            textSize = 64f
            setTextColor(Color.WHITE)
        })
        clockBox.addView(TextClock(this).apply {
            format12Hour = "EEEE, d MMMM"
            format24Hour = "EEEE, d MMMM"
            textSize = 16f
            setTextColor(0xCCFFFFFF.toInt())
        })
        root.addView(clockBox)

        // "Apps" button at bottom
        val btn = TextView(this).apply {
            text = "⌃   APPS"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(26), dp(12), dp(26), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(30).toFloat()
                setColor(0x66000000)
                setStroke(dp(1), 0xFFFF2D2D.toInt())
            }
            setOnClickListener { openDrawer() }
        }
        root.addView(btn, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = dp(44) })

        // App drawer
        drawer = FrameLayout(this).apply {
            setBackgroundColor(0xEE0B0B10.toInt())
            visibility = View.GONE
            isClickable = true
        }
        grid = GridView(this).apply {
            numColumns = 4
            verticalSpacing = dp(4)
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            setPadding(dp(12), dp(70), dp(12), dp(24))
            clipToPadding = false
            setOnItemClickListener { _, _, pos, _ ->
                val item = (adapter as AppAdapter).items[pos]
                val i = Intent(Intent.ACTION_MAIN)
                    .setClassName(item.pkg, item.cls)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { startActivity(i) } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "App nahi khul saki", Toast.LENGTH_SHORT).show()
                }
                closeDrawer()
            }
        }
        drawer.addView(grid, match)
        root.addView(drawer, match)
        setContentView(root)

        // Gestures: double tap = lock, swipe up = apps, long press = wallpaper
        detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                LockHelper.lock(this@MainActivity); return true
            }
            override fun onLongPress(e: MotionEvent) { pickWallpaper() }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null && e1.y - e2.y > dp(60) && abs(vy) > 800) {
                    openDrawer(); return true
                }
                return false
            }
        })
        root.setOnTouchListener { _, ev -> detector.onTouchEvent(ev) }

        loadWallpaper()
        loadApps()

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(pkgReceiver, f)
    }

    override fun onDestroy() {
        unregisterReceiver(pkgReceiver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        closeDrawer() // Home button dabane par drawer band
    }

    @Deprecated("Launcher: back se kuch nahi hota, sirf drawer band hota hai")
    override fun onBackPressed() {
        if (drawer.visibility == View.VISIBLE) closeDrawer()
    }

    private fun openDrawer() {
        drawer.alpha = 0f
        drawer.visibility = View.VISIBLE
        drawer.animate().alpha(1f).setDuration(180).start()
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        drawer.animate().alpha(0f).setDuration(150).withEndAction {
            drawer.visibility = View.GONE
        }.start()
    }

    // ---------- Wallpaper ----------
    private fun loadWallpaper() {
        val f = File(filesDir, "wallpaper.jpg")
        if (f.exists()) {
            val dm = resources.displayMetrics
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, o)
            var s = 1
            while (o.outWidth / s > dm.widthPixels * 2) s *= 2
            val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
            if (bmp != null) { wallpaper.setImageBitmap(bmp); return }
        }
        wallpaper.setImageResource(R.drawable.wallpaper)
    }

    private fun pickWallpaper() {
        Toast.makeText(this, "Apni Honda 125 ki photo chunein", Toast.LENGTH_SHORT).show()
        val i = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        startActivityForResult(i, pickCode)
    }

    @Deprecated("simple picker")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data
        if (req == pickCode && res == RESULT_OK && uri != null) {
            contentResolver.openInputStream(uri)?.use { input ->
                File(filesDir, "wallpaper.jpg").outputStream().use { input.copyTo(it) }
            }
            loadWallpaper()
        }
    }

    // ---------- Apps ----------
    private fun loadApps() {
        Thread {
            val pm = packageManager
            val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(q, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map {
                    AppItem(
                        it.loadLabel(pm).toString(),
                        it.activityInfo.packageName,
                        it.activityInfo.name,
                        IconStyler.style(it.loadIcon(pm), it.activityInfo.packageName)
                    )
                }
                .sortedBy { it.label.lowercase() }
            runOnUiThread { grid.adapter = AppAdapter(list) }
        }.start()
    }

    inner class AppAdapter(val items: List<AppItem>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
            val ctx = this@MainActivity
            val v = (cv as? LinearLayout) ?: LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(10), 0, dp(10))
                addView(ImageView(ctx).apply {
                    id = 1001
                    layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
                })
                addView(TextView(ctx).apply {
                    id = 1002
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(dp(4), dp(6), dp(4), 0)
                })
            }
            val item = items[p]
            v.findViewById<ImageView>(1001).setImageBitmap(item.icon)
            v.findViewById<TextView>(1002).text = item.label
            return v
        }
    }
}
