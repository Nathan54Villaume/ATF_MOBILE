package com.riva.atsmobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EtapeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AtsDatabase : RoomDatabase() {
    abstract fun etapesDao(): EtapesDao

    companion object {
        @Volatile private var INSTANCE: AtsDatabase? = null

        fun getInstance(context: Context): AtsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtsDatabase::class.java,
                    "AI_ATS"
                ).build().also { INSTANCE = it }
            }
    }
}
