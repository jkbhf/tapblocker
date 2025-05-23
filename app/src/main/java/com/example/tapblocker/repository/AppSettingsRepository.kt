package com.example.tapblocker.repository

import com.example.tapblocker.data.*
import kotlinx.coroutines.flow.Flow

class AppSettingsRepository(private val dao: AppSettingsDao) {
    fun observeSettings(): Flow<List<AppSettingWithRegions>> = dao.getAllSettings()

    suspend fun saveSetting(
        app: AppSettingEntity,
        regions: List<RegionEntity>
    ) {
        dao.insertApp(app)
        dao.deleteRegionsForApp(app.id)
        dao.insertRegions(regions)
    }

    suspend fun toggleActivation(appId: String, activated: Boolean) {
        dao.updateActivation(appId, activated)
    }

    suspend fun deleteApp(appId: String) {
        dao.deleteAppById(appId)
    }
}