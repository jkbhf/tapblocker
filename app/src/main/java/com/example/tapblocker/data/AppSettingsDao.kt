package com.example.tapblocker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class RegionOrientation {
    TOPLEFT, TOP, TOPRIGHT, RIGHT, BOTTOMRIGHT, BOTTOM, BOTTOMLEFT, LEFT
}

@Dao
interface AppSettingsDao {
    @Transaction
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingWithRegions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppSettingEntity)

    @Update
    suspend fun updateApp(app: AppSettingEntity)

    @Query("DELETE FROM app_settings WHERE id = :appId")
    suspend fun deleteAppById(appId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<RegionEntity>)

    @Query("DELETE FROM regions WHERE appId = :appId")
    suspend fun deleteRegionsForApp(appId: String)

    @Query("UPDATE app_settings SET activated = :activated WHERE id = :appId")
    suspend fun updateActivation(appId: String, activated: Boolean)
}