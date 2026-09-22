package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DetectionRecord
import com.example.data.local.entity.EmergencyContact
import com.example.data.local.entity.SavedPlace
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, id ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryContact(): EmergencyContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact): Long

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}

@Dao
interface DetectionHistoryDao {
    @Query("SELECT * FROM detection_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentDetections(): Flow<List<DetectionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetection(record: DetectionRecord)

    @Query("DELETE FROM detection_history")
    suspend fun clearHistory()
}

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places ORDER BY title ASC")
    fun getAllPlaces(): Flow<List<SavedPlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlace): Long

    @Delete
    suspend fun deletePlace(place: SavedPlace)
}
