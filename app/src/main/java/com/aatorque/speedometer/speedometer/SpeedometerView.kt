package com.aatorque.speedometer.speedometer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.aatorque.speedometer.R
import com.aatorque.speedometer.gps.SpeedState
import com.aatorque.speedometer.theme.DarkSpeedometerColors
import com.aatorque.speedometer.theme.LightSpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerColors
import com.aatorque.speedometer.theme.SpeedometerTheme

class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Speed State data
    private var speedState = SpeedState()

    private var targetSpeed: Float = 0f
    private var currentAnimatedSpeed: Float = 0f
    private var speedAnimator: ValueAnimator? = null

    // Current Theme & Colors
    private var currentTheme: SpeedometerTheme = SpeedometerTheme.DARK
    private var currentColors: SpeedometerColors = DarkSpeedometerColors

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val digitalSpeedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val unitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val secondaryInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointerCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val arcRect = RectF()
    private val pointerPath = Path()

    // Angles: Start angle at 135 degrees (bottom left), sweep 270 degrees to 405 degrees (bottom right)
    private val startAngle = 135f
    private val totalSweepAngle = 270f

    // Theme state override: null = use system / state theme
    var isDarkModeOverride: Boolean? = null

    fun setTheme(theme: SpeedometerTheme) {
        this.currentTheme = theme
        this.currentColors = if (theme == SpeedometerTheme.DARK) DarkSpeedometerColors else LightSpeedometerColors
        invalidate()
    }

    fun setSpeedometerColors(colors: SpeedometerColors) {
        this.currentColors = colors
        invalidate()
    }

    fun updateState(state: SpeedState) {
        this.speedState = state
        val speedInUnit = if (state.unit.equals("mph", ignoreCase = true)) {
            state.filteredSpeedKmh * 0.621371
        } else {
            state.filteredSpeedKmh
        }
        setSpeed(speedInUnit.toFloat())
        invalidate()
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0f, speedState.maxSpeedLimit.toFloat())
        if (clamped == targetSpeed) return

        speedAnimator?.cancel()
        speedAnimator = ValueAnimator.ofFloat(currentAnimatedSpeed, clamped).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                currentAnimatedSpeed = anim.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
        targetSpeed = clamped
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val colors = currentColors

        canvas.drawColor(colors.background)

        val widthF = width.toFloat()
        val heightF = height.toFloat()
        val centerX = widthF / 2f
        val centerY = heightF * 0.46f
        val radius = (widthF.coerceAtMost(heightF) / 2f) * 0.78f

        if (radius <= 0) return

        val strokeWidth = radius * 0.08f
        trackPaint.strokeWidth = strokeWidth
        trackPaint.color = colors.inactiveTrack

        progressArcPaint.strokeWidth = strokeWidth

        // Setup Gradient for the speed arc
        val arcShader = LinearGradient(
            centerX - radius, centerY + radius,
            centerX + radius, centerY - radius,
            intArrayOf(
                colors.activeArcStart,
                colors.activeArcMid,
                colors.activeArcEnd
            ),
            floatArrayOf(0.0f, 0.6f, 1.0f),
            Shader.TileMode.CLAMP
        )
        progressArcPaint.shader = arcShader

        arcRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 1. Draw Background Track Arc
        canvas.drawArc(arcRect, startAngle, totalSweepAngle, false, trackPaint)

        // 2. Draw Active Speed Arc
        val currentSweep = (currentAnimatedSpeed / speedState.maxSpeedLimit.toFloat()) * totalSweepAngle
        if (currentSweep > 0.5f) {
            canvas.drawArc(arcRect, startAngle, currentSweep, false, progressArcPaint)
        }

        // 3. Draw Ticks and Dial Scale Numbers
        val maxSpeed = speedState.maxSpeedLimit
        val majorStep = 20
        val totalSteps = maxSpeed / majorStep

        numberPaint.color = colors.primaryText
        numberPaint.textSize = radius * 0.11f

        for (i in 0..totalSteps) {
            val stepSpeed = i * majorStep
            val fraction = stepSpeed.toFloat() / maxSpeed.toFloat()
            val angleDeg = startAngle + fraction * totalSweepAngle
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val isMajor = stepSpeed % 40 == 0 || stepSpeed == maxSpeed || stepSpeed == 0

            tickPaint.color = if (isMajor) colors.majorTick else colors.minorTick

            val outerRadius = radius - (strokeWidth / 2f) - 6f
            val innerRadius = outerRadius - if (isMajor) radius * 0.09f else radius * 0.05f

            val startX = (centerX + outerRadius * Math.cos(angleRad)).toFloat()
            val startY = (centerY + outerRadius * Math.sin(angleRad)).toFloat()
            val stopX = (centerX + innerRadius * Math.cos(angleRad)).toFloat()
            val stopY = (centerY + innerRadius * Math.sin(angleRad)).toFloat()

            tickPaint.strokeWidth = if (isMajor) strokeWidth * 0.25f else strokeWidth * 0.12f
            canvas.drawLine(startX, startY, stopX, stopY, tickPaint)

            if (isMajor) {
                val textRadius = innerRadius - radius * 0.12f
                val textX = (centerX + textRadius * Math.cos(angleRad)).toFloat()
                val textY = (centerY + textRadius * Math.sin(angleRad)).toFloat() + (numberPaint.textSize / 3f)
                canvas.drawText(stepSpeed.toString(), textX, textY, numberPaint)
            }
        }

        // 4. Draw Center Digital Speed Readout (Highest Visual Importance)
        digitalSpeedPaint.color = colors.primaryText
        digitalSpeedPaint.textSize = radius * 0.44f

        val displayVal = Math.round(currentAnimatedSpeed).toInt()
        val digitalY = centerY + radius * 0.08f
        canvas.drawText(displayVal.toString(), centerX, digitalY, digitalSpeedPaint)

        // 5. Draw Unit ("km/h" or "mph") below Digital Speed
        unitTextPaint.color = colors.activeArcStart
        unitTextPaint.textSize = radius * 0.14f
        val unitY = digitalY + radius * 0.18f
        canvas.drawText(speedState.unit, centerX, unitY, unitTextPaint)

        // 6. Draw Smooth Pointer / Needle
        val needleAngleDeg = startAngle + currentSweep
        val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())

        val pointerLength = radius * 0.72f
        val tipX = (centerX + pointerLength * Math.cos(needleAngleRad)).toFloat()
        val tipY = (centerY + pointerLength * Math.sin(needleAngleRad)).toFloat()

        pointerPaint.color = colors.activeArcStart
        pointerPaint.strokeWidth = strokeWidth * 0.35f
        pointerPaint.style = Paint.Style.STROKE
        pointerPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(centerX, centerY, tipX, tipY, pointerPaint)

        // Center Pivot Circle
        pointerCenterPaint.color = colors.activeArcStart
        canvas.drawCircle(centerX, centerY, radius * 0.06f, pointerCenterPaint)

        // 7. Draw Secondary Information Panel at Bottom (Discreet)
        drawSecondaryPanel(canvas, centerX, heightF - radius * 0.20f, radius, colors.secondaryText)
    }

    private fun drawSecondaryPanel(canvas: Canvas, centerX: Float, startY: Float, radius: Float, textColor: Int) {
        secondaryInfoPaint.color = textColor
        secondaryInfoPaint.textSize = radius * 0.095f

        val items = mutableListOf<String>()

        if (speedState.showSatellites) {
            items.add("Satélites: ${speedState.satelliteCount}")
        }
        if (speedState.showAccuracy) {
            items.add("Precisão: ${Math.round(speedState.accuracyMeters)}m")
        }
        if (speedState.showAltitude) {
            items.add("Alt: ${Math.round(speedState.altitudeMeters)}m")
        }
        if (speedState.showTrip) {
            items.add("Distância: %.1f km".format(speedState.tripDistanceKm))
        }

        val infoLine = items.joinToString("  │  ")
        if (infoLine.isNotEmpty()) {
            canvas.drawText(infoLine, centerX, startY, secondaryInfoPaint)
        }

        // Discrete GPS status dot
        val gpsDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(
                context,
                if (speedState.isGpsActive) R.color.status_green else R.color.status_red
            )
        }
        val gpsLabel = if (speedState.isGpsActive) "GPS: ON" else "GPS: BUSCANDO"
        val dotRadius = radius * 0.03f
        val gpsY = startY + radius * 0.12f

        secondaryInfoPaint.textSize = radius * 0.085f
        val textWidth = secondaryInfoPaint.measureText(gpsLabel)
        val dotX = centerX - (textWidth / 2f) - (dotRadius * 2.5f)

        canvas.drawCircle(dotX, gpsY - (dotRadius / 2f), dotRadius, gpsDotPaint)
        canvas.drawText(gpsLabel, centerX, gpsY, secondaryInfoPaint)
    }

    override fun onDetachedFromWindow() {
        speedAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
