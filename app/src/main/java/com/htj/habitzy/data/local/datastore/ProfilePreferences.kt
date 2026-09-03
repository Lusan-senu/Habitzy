package com.htj.habitzy.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "habitzy_profile")

@Singleton
class ProfilePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val NAME = stringPreferencesKey("name")
        val PHOTO_URI = stringPreferencesKey("photo_uri")
    }

    val name: Flow<String?> = context.profileDataStore.data.map { it[Keys.NAME] }
    suspend fun setName(name: String?) {
        if (name == null) context.profileDataStore.edit { it.remove(Keys.NAME) }
        else context.profileDataStore.edit { it[Keys.NAME] = name }
    }

    val photoUri: Flow<String?> = context.profileDataStore.data.map { it[Keys.PHOTO_URI] }
    suspend fun setPhotoUri(uri: String?) {
        if (uri == null) context.profileDataStore.edit { it.remove(Keys.PHOTO_URI) }
        else context.profileDataStore.edit { it[Keys.PHOTO_URI] = uri }
    }
}
