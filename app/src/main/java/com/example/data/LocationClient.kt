package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed interface LocationFetchResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float?
    ) : LocationFetchResult

    data object PermissionMissing : LocationFetchResult
    data object LocationDisabled : LocationFetchResult
    data object Unavailable : LocationFetchResult
}

class ParkingLocationClient(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(): LocationFetchResult {
        if (!hasLocationPermission()) {
            return LocationFetchResult.PermissionMissing
        }
        if (!isLocationEnabled()) {
            return LocationFetchResult.LocationDisabled
        }

        // 1. Request fresh high-accuracy location fix via FusedLocationProviderClient
        val freshLocation = withTimeoutOrNull(7_500L) {
            requestFusedCurrentLocation()
        }
        if (freshLocation != null) {
            return freshLocation.toSuccessResult()
        }

        // 2. Fallback to Fused lastLocation
        val fusedLast = withTimeoutOrNull(2_500L) {
            requestFusedLastLocation()
        }
        if (fusedLast != null) {
            return fusedLast.toSuccessResult()
        }

        // 3. Fallback to Android LocationManager last known GPS/Network fix
        val legacyLast = getBestLegacyLastKnownLocation()
        if (legacyLast != null) {
            return legacyLast.toSuccessResult()
        }

        return LocationFetchResult.Unavailable
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            continuation.invokeOnCancellation { cts.cancel() }

            val priority = if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            try {
                fusedClient.getCurrentLocation(priority, cts.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun requestFusedLastLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun getBestLegacyLastKnownLocation(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLocation: Location? = null
        for (provider in providers) {
            val candidate = try {
                manager.getLastKnownLocation(provider)
            } catch (_: Exception) {
                null
            } ?: continue

            if (bestLocation == null || candidate.time > bestLocation.time) {
                bestLocation = candidate
            }
        }
        return bestLocation
    }

    private fun Location.toSuccessResult(): LocationFetchResult.Success {
        return LocationFetchResult.Success(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else null
        )
    }
}
