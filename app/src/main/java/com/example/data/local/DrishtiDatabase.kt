package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DetectionHistoryDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.SavedPlaceDao
import com.example.data.local.entity.DetectionRecord
import com.example.data.local.entity.EmergencyContact
import com.example.data.local.entity.SavedPlace

@Database(
    entities = [
        EmergencyContact::class,
        DetectionRecord::class,
        SavedPlace::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DrishtiDatabase : RoomDatabase() {
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun detectionHistoryDao(): DetectionHistoryDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile
        private var INSTANCE: DrishtiDatabase? = null

        fun getDatabase(context: Context): DrishtiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DrishtiDatabase::class.java,
                    "drishti_vaani_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
