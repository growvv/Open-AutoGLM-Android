package com.example.open_autoglm_android.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    
    // 状态
    private var currentStatus = "空闲"
    private var currentStep = 0
    private var isExpanded = true
    private var isPaused = false
    
    // 拖动相关
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    // 大小调整
    private var currentWidth = 280
    private var currentHeight = 160
    private val minWidth = 150
    private val maxWidth = 400
    private val minHeight = 100
    private val maxHeight = 300

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createFloatingWindow()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        removeFloatingWindow()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 创建悬浮窗视图
        floatingView = createFloatingView()
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        params = WindowManager.LayoutParams(
            dpToPx(currentWidth),
            dpToPx(currentHeight),
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }
        
        windowManager?.addView(floatingView, params)
        setupTouchListener()
    }
    
    @SuppressLint("SetTextI18n")
    private fun createFloatingView(): View {
        val context = this
        
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE6303030.toInt())
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            
            // 标题栏
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                
                // 标题
                addView(TextView(context).apply {
                    text = "任务状态"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                
                // 暂停/继续按钮 (⏸/▶)
                addView(TextView(context).apply {
                    tag = "pause_resume_btn"
                    text = "⏸"
                    setTextColor(0xFFFFFF00.toInt()) // 黄色
                    textSize = 18f
                    setPadding(dpToPx(8), 0, dpToPx(8), 0)
                    visibility = View.GONE
                    setOnClickListener { onPauseResumeClickListener?.invoke() }
                })
                
                // 停止按钮 (⏹)
                addView(TextView(context).apply {
                    tag = "stop_btn"
                    text = "⏹"
                    setTextColor(0xFFFF4444.toInt())
                    textSize = 18f
                    setPadding(dpToPx(8), 0, dpToPx(8), 0)
                    visibility = View.GONE // 默认隐藏，仅在执行中显示
                    setOnClickListener { onStopClickListener?.invoke() }
                })
                
                // 缩小按钮
                addView(TextView(context).apply {
                    text = "−"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 18f
                    setPadding(dpToPx(8), 0, dpToPx(8), 0)
                    setOnClickListener { adjustSize(-30, -20) }
                })
                
                // 放大按钮
                addView(TextView(context).apply {
                    text = "+"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 18f
                    setPadding(dpToPx(8), 0, dpToPx(8), 0)
                    setOnClickListener { adjustSize(30, 20) }
                })
                
                // 折叠/展开按钮
                addView(TextView(context).apply {
                    tag = "toggle"
                    text = "▼"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 14f
                    setPadding(dpToPx(8), 0, 0, 0)
                    setOnClickListener { toggleExpand() }
                })
            })
            
            // 内容区域
            addView(LinearLayout(context).apply {
                tag = "content"
                orientation = LinearLayout.VERTICAL
                setPadding(0, dpToPx(8), 0, 0)
                
                // 状态
                addView(TextView(context).apply {
                    tag = "status"
                    text = "状态: $currentStatus"
                    setTextColor(0xFFCCCCCC.toInt())
                    textSize = 12f
                })
                
                // 步骤
                addView(TextView(context).apply {
                    tag = "step"
                    text = "步骤: $currentStep"
                    setTextColor(0xFFCCCCCC.toInt())
                    textSize = 12f
                    setPadding(0, dpToPx(4), 0, 0)
                })
                
                // 详细信息
                addView(TextView(context).apply {
                    tag = "detail"
                    text = ""
                    setTextColor(0xFF999999.toInt())
                    textSize = 11f
                    maxLines = 3
                    setPadding(0, dpToPx(4), 0, 0)
                })
            })
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun adjustSize(deltaWidth: Int, deltaHeight: Int) {
        currentWidth = (currentWidth + deltaWidth).coerceIn(minWidth, maxWidth)
        currentHeight = (currentHeight + deltaHeight).coerceIn(minHeight, maxHeight)
        
        params?.width = dpToPx(currentWidth)
        params?.height = dpToPx(currentHeight)
        windowManager?.updateViewLayout(floatingView, params)
    }
    
    private fun toggleExpand() {
        isExpanded = !isExpanded
        val content = floatingView?.findViewWithTag<View>("content")
        val toggle = floatingView?.findViewWithTag<TextView>("toggle")
        
        content?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        toggle?.text = if (isExpanded) "▼" else "▶"
        
        params?.height = if (isExpanded) dpToPx(currentHeight) else WindowManager.LayoutParams.WRAP_CONTENT
        windowManager?.updateViewLayout(floatingView, params)
    }
    
    fun updateStatus(status: String, step: Int = currentStep, detail: String = "") {
        currentStatus = status
        currentStep = step
        
        floatingView?.post {
            floatingView?.findViewWithTag<TextView>("status")?.text = "状态: $status"
            floatingView?.findViewWithTag<TextView>("step")?.text = "步骤: $step"
            floatingView?.findViewWithTag<TextView>("detail")?.text = detail
            
            val isRunningOrPaused = status == "执行中" || status == "已暂停"
            
            // 按钮可见性
            floatingView?.findViewWithTag<View>("stop_btn")?.visibility = if (isRunningOrPaused) View.VISIBLE else View.GONE
            floatingView?.findViewWithTag<View>("pause_resume_btn")?.visibility = if (isRunningOrPaused) View.VISIBLE else View.GONE
        }
    }
    
    fun updatePauseStatus(paused: Boolean) {
        this.isPaused = paused
        floatingView?.post {
            val pauseBtn = floatingView?.findViewWithTag<TextView>("pause_resume_btn")
            pauseBtn?.text = if (paused) "▶" else "⏸"
            
            if (paused) {
                updateStatus("已暂停")
            } else if (currentStatus == "已暂停") {
                updateStatus("执行中")
            }
        }
    }

    /**
     * 设置悬浮窗可见性
     */
    fun setVisibility(visible: Boolean) {
        floatingView?.post {
            floatingView?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }
    
    private fun removeFloatingWindow() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private var instance: FloatingWindowService? = null
        var onStopClickListener: (() -> Unit)? = null
        var onPauseResumeClickListener: (() -> Unit)? = null
        
        fun getInstance(): FloatingWindowService? = instance
        
        fun hasOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
        
        fun startService(context: Context) {
            if (hasOverlayPermission(context)) {
                context.startService(Intent(context, FloatingWindowService::class.java))
            }
        }
        
        fun stopService(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }
}
