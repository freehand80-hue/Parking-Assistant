package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.parkingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "my_car_parking_prefs"
)

data class StoredParkingState(
    val activeSpot: ParkingSpot? = null,
    val preferredMapApp: MapAppOption = MapAppOption.AUTO,
    val reminderEnabled: Boolean = true,
    val history: List<ParkingHistoryItem> = emptyList()
)

class ParkingPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAS_ACTIVE_SPOT = booleanPreferencesKey("has_active_spot")
        val HAS_COORDINATES = booleanPreferencesKey("has_coordinates")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val HAS_ACCURACY = booleanPreferencesKey("has_accuracy")
        val ACCURACY = floatPreferencesKey("accuracy")
        val SAVED_AT_MILLIS = longPreferencesKey("saved_at_millis")
        val FLOOR_LABEL = stringPreferencesKey("floor_label")
        val ZONE_LABEL = stringPreferencesKey("zone_label")
        val MEMO = stringPreferencesKey("memo")
        val PHOTO_PATH = stringPreferencesKey("photo_path")
        val PREFERRED_MAP_APP = stringPreferencesKey("preferred_map_app")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val HISTORY_JSON = stringPreferencesKey("history_json")
    }

    val stateFlow: Flow<StoredParkingState> = context.parkingDataStore.data.map { prefs ->
        val hasSpot = prefs[Keys.HAS_ACTIVE_SPOT] ?: false
        val spot = if (hasSpot) {
            val hasCoords = prefs[Keys.HAS_COORDINATES] ?: false
            val hasAccuracy = prefs[Keys.HAS_ACCURACY] ?: false
            ParkingSpot(
                latitude = if (hasCoords) prefs[Keys.LATITUDE] else null,
                longitude = if (hasCoords) prefs[Keys.LONGITUDE] else null,
                accuracyMeters = if (hasAccuracy) prefs[Keys.ACCURACY] else null,
                savedAtMillis = prefs[Keys.SAVED_AT_MILLIS] ?: System.currentTimeMillis(),
                floorLabel = prefs[Keys.FLOOR_LABEL].orEmpty(),
                zoneLabel = prefs[Keys.ZONE_LABEL].orEmpty(),
                memo = prefs[Keys.MEMO].orEmpty(),
                photoPath = prefs[Keys.PHOTO_PATH]?.takeIf { it.isNotBlank() }
            )
        } else {
            null
        }

        StoredParkingState(
            activeSpot = spot,
            preferredMapApp = MapAppOption.fromKey(prefs[Keys.PREFERRED_MAP_APP]),
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            history = decodeHistory(prefs[Keys.HISTORY_JSON])
        )
    }

    suspend fun saveActiveSpot(spot: ParkingSpot) {
        context.parkingDataStore.edit { prefs ->
            prefs[Keys.HAS_ACTIVE_SPOT] = true
            prefs[Keys.HAS_COORDINATES] = spot.hasCoordinates
            if (spot.latitude != null && spot.longitude != null) {
                prefs[Keys.LATITUDE] = spot.latitude
                prefs[Keys.LONGITUDE] = spot.longitude
            } else {
                prefs.remove(Keys.LATITUDE)
                prefs.remove(Keys.LONGITUDE)
            }
            if (spot.accuracyMeters != null) {
                prefs[Keys.HAS_ACCURACY] = true
                prefs[Keys.ACCURACY] = spot.accuracyMeters
            } else {
                prefs[Keys.HAS_ACCURACY] = false
                prefs.remove(Keys.ACCURACY)
            }
            prefs[Keys.SAVED_AT_MILLIS] = spot.savedAtMillis
            prefs[Keys.FLOOR_LABEL] = spot.floorLabel
            prefs[Keys.ZONE_LABEL] = spot.zoneLabel
            prefs[Keys.MEMO] = spot.memo
            if (spot.photoPath != null) {
                prefs[Keys.PHOTO_PATH] = spot.photoPath
            } else {
                prefs.remove(Keys.PHOTO_PATH)
            }
        }
    }

    suspend fun updateMemo(floorLabel: String, zoneLabel: String, memo: String) {
        context.parkingDataStore.edit { prefs ->
            if (prefs[Keys.HAS_ACTIVE_SPOT] == true) {
                prefs[Keys.FLOOR_LABEL] = floorLabel
                prefs[Keys.ZONE_LABEL] = zoneLabel
                prefs[Keys.MEMO] = memo
            }
        }
    }

    suspend fun updatePhotoPath(photoPath: String?) {
        context.parkingDataStore.edit { prefs ->
            if (prefs[Keys.HAS_ACTIVE_SPOT] == true) {
                if (photoPath.isNullOrBlank()) {
                    prefs.remove(Keys.PHOTO_PATH)
                } else {
                    prefs[Keys.PHOTO_PATH] = photoPath
                }
            }
        }
    }

    suspend fun updatePreferredMapApp(option: MapAppOption) {
        context.parkingDataStore.edit { prefs ->
            prefs[Keys.PREFERRED_MAP_APP] = option.storageKey
        }
    }

    suspend fun updateReminderEnabled(enabled: Boolean) {
        context.parkingDataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    suspend fun archiveAndClearActiveSpot(currentSpot: ParkingSpot?) {
        context.parkingDataStore.edit { prefs ->
            if (currentSpot != null) {
                val existing = decodeHistory(prefs[Keys.HISTORY_JSON]).toMutableList()
                existing.add(
                    0,
                    ParkingHistoryItem(
                        id = currentSpot.savedAtMillis,
                        latitude = currentSpot.latitude,
                        longitude = currentSpot.longitude,
                        savedAtMillis = currentSpot.savedAtMillis,
                        floorLabel = currentSpot.floorLabel,
                        zoneLabel = currentSpot.zoneLabel,
                        memo = currentSpot.memo
                    )
                )
                val trimmed = existing.distinctBy { it.id }.take(MAX_HISTORY_ITEMS)
                prefs[Keys.HISTORY_JSON] = encodeHistory(trimmed)
            }

            prefs[Keys.HAS_ACTIVE_SPOT] = false
            prefs.remove(Keys.HAS_COORDINATES)
            prefs.remove(Keys.LATITUDE)
            prefs.remove(Keys.LONGITUDE)
            prefs.remove(Keys.HAS_ACCURACY)
            prefs.remove(Keys.ACCURACY)
            prefs.remove(Keys.SAVED_AT_MILLIS)
            prefs.remove(Keys.FLOOR_LABEL)
            prefs.remove(Keys.ZONE_LABEL)
            prefs.remove(Keys.MEMO)
            prefs.remove(Keys.PHOTO_PATH)
        }
    }

    suspend fun clearHistory() {
        context.parkingDataStore.edit { prefs ->
            prefs.remove(Keys.HISTORY_JSON)
        }
    }

    private fun encodeHistory(items: List<ParkingHistoryItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                if (item.latitude != null) put("lat", item.latitude)
                if (item.longitude != null) put("lng", item.longitude)
                put("savedAt", item.savedAtMillis)
                put("floor", item.floorLabel)
                put("zone", item.zoneLabel)
                put("memo", item.memo)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeHistory(raw: String?): List<ParkingHistoryItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val lat = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat") else null
                    val lng = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng") else null
                    add(
                        ParkingHistoryItem(
                            id = obj.optLong("id", index.toLong()),
                            latitude = lat?.takeIf { !it.isNaN() },
                            longitude = lng?.takeIf { !it.isNaN() },
                            savedAtMillis = obj.optLong("savedAt", 0L),
                            floorLabel = obj.optString("floor", ""),
                            zoneLabel = obj.optString("zone", ""),
                            memo = obj.optString("memo", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_HISTORY_ITEMS = 6
    }
}
