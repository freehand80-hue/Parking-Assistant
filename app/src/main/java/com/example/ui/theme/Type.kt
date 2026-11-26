package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSansKrFamily = FontFamily(
    Font(R.font.noto_sans_kr, FontWeight.Normal)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansKrFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)
