package com.pelajstudios.trafiku.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pelajstudios.trafiku.data.model.BusSelection
import com.pelajstudios.trafiku.data.model.RouteDirection
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class BusSelectionStore(
    private val dataStore: DataStore<Preferences>,
) {
    val selection: Flow<BusSelection?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toSelection() }

    suspend fun save(selection: BusSelection) {
        dataStore.edit { preferences ->
            preferences[SELECTED_LINE_ID] = selection.lineId
            preferences[SELECTED_DIRECTION] = selection.direction.name
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(SELECTED_LINE_ID)
            preferences.remove(SELECTED_DIRECTION)
        }
    }

    private fun Preferences.toSelection(): BusSelection? {
        val lineId = this[SELECTED_LINE_ID] ?: return null
        val directionName = this[SELECTED_DIRECTION] ?: return null
        val direction = RouteDirection.entries.firstOrNull { it.name == directionName } ?: return null
        return BusSelection(lineId = lineId, direction = direction)
    }

    private companion object {
        val SELECTED_LINE_ID = stringPreferencesKey("selected_bus_line_id")
        val SELECTED_DIRECTION = stringPreferencesKey("selected_bus_route_direction")
    }
}
