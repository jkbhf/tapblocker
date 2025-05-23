package com.example.tapblocker.screens.mainscreen

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tapblocker.data.AppDatabase
import com.example.tapblocker.data.AppSettingEntity
import com.example.tapblocker.repository.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).appSettingsDao()
    private val repo = AppSettingsRepository(dao)

    // Observable Flow in Compose
    val settings = repo.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleApp(appId: String, activated: Boolean) {
        viewModelScope.launch {
            repo.toggleActivation(appId, activated)
        }
    }

    fun addApp(name: String, id: String) {
        viewModelScope.launch {
            val appEntity = AppSettingEntity(id = id, name = name, activated = false)
            repo.saveSetting(appEntity, emptyList())
        }
    }
}
