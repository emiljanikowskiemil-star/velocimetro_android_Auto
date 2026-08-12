package com.aatorque.speedometer.theme

import android.graphics.Color

enum class SpeedometerTheme {
    LIGHT,
    DARK
}

data class SpeedometerColors(
    val background: Int,
    val primaryText: Int,
    val secondaryText: Int,
    val majorTick: Int,
    val minorTick: Int,
    val activeArcStart: Int,
    val activeArcMid: Int,
    val activeArcEnd: Int,
    val inactiveTrack: Int,
    val themeButtonIconColor: Int
)

val DarkSpeedometerColors = SpeedometerColors(
    background = Color.parseColor("#101010"),
    primaryText = Color.parseColor("#FFFFFF"),
    secondaryText = Color.parseColor("#BDBDBD"),
    majorTick = Color.parseColor("#FFFFFF"),
    minorTick = Color.parseColor("#707070"),
    activeArcStart = Color.parseColor("#00E676"),
    activeArcMid = Color.parseColor("#FFEA00"),
    activeArcEnd = Color.parseColor("#FF1744"),
    inactiveTrack = Color.parseColor("#30363D"),
    themeButtonIconColor = Color.parseColor("#FFFFFF")
)

val LightSpeedometerColors = SpeedometerColors(
    background = Color.parseColor("#F5F5F5"),
    primaryText = Color.parseColor("#111111"),
    secondaryText = Color.parseColor("#555555"),
    majorTick = Color.parseColor("#202020"),
    minorTick = Color.parseColor("#AAAAAA"),
    activeArcStart = Color.parseColor("#00E676"),
    activeArcMid = Color.parseColor("#FFEA00"),
    activeArcEnd = Color.parseColor("#FF1744"),
    inactiveTrack = Color.parseColor("#D0D7DE"),
    themeButtonIconColor = Color.parseColor("#111111")
)
