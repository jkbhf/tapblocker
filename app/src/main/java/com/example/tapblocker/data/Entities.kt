package com.example.tapblocker.data

import androidx.room.*

/**
 * Entity für App-Einstellungen.
 */
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val id: String,
    val name: String,
    var activated: Boolean
)

/**
 * Entity für blockierte Regionen pro App.
 */
@Entity(
    tableName = "regions",
    foreignKeys = [ForeignKey(
        entity = AppSettingEntity::class,
        parentColumns = ["id"],
        childColumns = ["appId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("appId")]
)
data class RegionEntity(
    @PrimaryKey(autoGenerate = true) val regionId: Long = 0,
    val appId: String,
    val orientation: String,
    val xOffset: Int,
    val yOffset: Int,
    val xSize: Int,
    val ySize: Int
)

/**
 * Datenklasse mit App-Einstellungen und zugehörigen Regionen.
 */
data class AppSettingWithRegions(
    @Embedded val app: AppSettingEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "appId"
    ) val regions: List<RegionEntity>
)
