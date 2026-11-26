package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

sealed interface MapLaunchResult {
    data class Launched(val appDisplayName: String) : MapLaunchResult
    data object Failed : MapLaunchResult
}

object MapNavigator {

    private const val PKG_KAKAO_MAP = "net.daum.android.map"
    private const val PKG_NAVER_MAP = "com.nhn.android.nmap"
    private const val PKG_GOOGLE_MAPS = "com.google.android.apps.maps"

    fun getInstalledMapApps(context: Context): InstalledMapApps {
        return InstalledMapApps(
            hasKakaoMap = isPackageInstalled(context, PKG_KAKAO_MAP),
            hasNaverMap = isPackageInstalled(context, PKG_NAVER_MAP),
            hasGoogleMaps = isPackageInstalled(context, PKG_GOOGLE_MAPS)
        )
    }

    fun launchWalkingDirections(
        context: Context,
        latitude: Double,
        longitude: Double,
        destinationLabel: String,
        preferredOption: MapAppOption
    ): MapLaunchResult {
        val label = destinationLabel.ifBlank { "내 차 주차 위치" }
        val encodedLabel = URLEncoder.encode(label, StandardCharsets.UTF_8.name())
        val latStr = String.format(Locale.US, "%.6f", latitude)
        val lngStr = String.format(Locale.US, "%.6f", longitude)

        val candidates: List<Pair<String, Intent>> = when (preferredOption) {
            MapAppOption.KAKAO -> listOf(
                "카카오맵" to kakaoWalkingIntent(latStr, lngStr),
                "카카오맵(웹)" to kakaoWebIntent(encodedLabel, latStr, lngStr),
                "지도" to geoIntent(latStr, lngStr, encodedLabel),
                "구글 지도(웹)" to googleWebWalkingIntent(latStr, lngStr)
            )
            MapAppOption.NAVER -> listOf(
                "네이버 지도" to naverWalkingIntent(context, latStr, lngStr, encodedLabel),
                "네이버 지도(웹)" to naverWebWalkingIntent(encodedLabel, latStr, lngStr),
                "지도" to geoIntent(latStr, lngStr, encodedLabel),
                "구글 지도(웹)" to googleWebWalkingIntent(latStr, lngStr)
            )
            MapAppOption.GOOGLE -> listOf(
                "구글 지도" to googleWalkingIntent(latStr, lngStr),
                "구글 지도(웹)" to googleWebWalkingIntent(latStr, lngStr),
                "지도" to geoIntent(latStr, lngStr, encodedLabel)
            )
            MapAppOption.AUTO -> listOf(
                "카카오맵" to kakaoWalkingIntent(latStr, lngStr),
                "네이버 지도" to naverWalkingIntent(context, latStr, lngStr, encodedLabel),
                "구글 지도" to googleWalkingIntent(latStr, lngStr),
                "기본 지도" to geoIntent(latStr, lngStr, encodedLabel),
                "구글 지도(웹)" to googleWebWalkingIntent(latStr, lngStr)
            )
        }

        for ((name, intent) in candidates) {
            if (tryStartActivity(context, intent)) {
                return MapLaunchResult.Launched(name)
            }
        }
        return MapLaunchResult.Failed
    }

    private fun kakaoWalkingIntent(lat: String, lng: String): Intent {
        val uri = Uri.parse("kakaomap://route?ep=$lat,$lng&by=FOOT")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(PKG_KAKAO_MAP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun kakaoWebIntent(encodedLabel: String, lat: String, lng: String): Intent {
        val uri = Uri.parse("https://map.kakao.com/link/to/$encodedLabel,$lat,$lng")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun naverWalkingIntent(
        context: Context,
        lat: String,
        lng: String,
        encodedLabel: String
    ): Intent {
        val uri = Uri.parse(
            "nmap://route/walk?dlat=$lat&dlng=$lng&dname=$encodedLabel&appname=${context.packageName}"
        )
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(PKG_NAVER_MAP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun naverWebWalkingIntent(encodedLabel: String, lat: String, lng: String): Intent {
        val uri = Uri.parse("https://map.naver.com/v5/directions/-/$lng,$lat,$encodedLabel/-/walk")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun googleWalkingIntent(lat: String, lng: String): Intent {
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(PKG_GOOGLE_MAPS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun googleWebWalkingIntent(lat: String, lng: String): Intent {
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=walking"
        )
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun geoIntent(lat: String, lng: String, encodedLabel: String): Intent {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            val resolved = intent.resolveActivity(context.packageManager)
            if (resolved != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
