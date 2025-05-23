package com.example.tapblocker.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [AppSettingEntity::class, RegionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tapblocker.db"
            )
                // Callback nur beim ersten Anlegen ausführen
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // in einem IO-Thread die Default-Daten einfügen
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).appSettingsDao()
                            // Default App
                            val defaultApp = AppSettingEntity(
                                id        = "com.instagram.android",
                                name      = "Instagram",
                                activated = true
                            )
                            dao.insertApp(defaultApp)
                            // Default Regions
                            val defaultRegions = listOf(
                                RegionEntity(
                                    appId       = "default_app",
                                    orientation = RegionOrientation.BOTTOM.name,
                                    xOffset     = 0,
                                    yOffset     = 0,
                                    xSize       = 100,
                                    ySize       = 100
                                ),
                            )
                            dao.insertRegions(defaultRegions)
                        }
                    }
                })
                .build()
        }
    }
}

