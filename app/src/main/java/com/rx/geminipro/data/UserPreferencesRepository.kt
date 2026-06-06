package com.rx.geminipro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val IS_MENU_LEFT = booleanPreferencesKey("is_menu_left")
        val SKIPPED_VERSION = stringPreferencesKey("skipped_update_version")
    }

    val isMenuLeftFlow: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.IS_MENU_LEFT] ?: false }
    val skippedVersionFlow: Flow<String?> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.SKIPPED_VERSION] }

    suspend fun saveMenuPosition(isLeft: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MENU_LEFT] = isLeft
        }
    }
    suspend fun setSkippedVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIPPED_VERSION] = version
        }
    }
}
