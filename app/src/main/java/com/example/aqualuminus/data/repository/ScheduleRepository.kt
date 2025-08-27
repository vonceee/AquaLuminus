package com.example.aqualuminus.data.repository

import android.util.Log
import com.example.aqualuminus.ui.screens.schedule.model.SavedSchedule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ScheduleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val schedulesCollection = firestore.collection("schedules")

    suspend fun saveSchedule(schedule: SavedSchedule): Result<Unit> {
        return try {
            schedulesCollection.document(schedule.id).set(schedule).await()
            Log.d("ScheduleRepository", "Schedule saved successfully: ${schedule.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error saving schedule", e)
            Result.failure(e)
        }
    }

    suspend fun getSchedules(): Result<List<SavedSchedule>> {
        return try {
            val snapshot = schedulesCollection.get().await()
            val schedules = snapshot.documents.mapNotNull { document ->
                document.toObject(SavedSchedule::class.java)
            }
            Log.d("ScheduleRepository", "Loaded ${schedules.size} schedules")
            Result.success(schedules)
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error loading schedules", e)
            Result.failure(e)
        }
    }

    suspend fun getScheduleById(scheduleId: String): SavedSchedule? {
        return try {
            val document = schedulesCollection.document(scheduleId).get().await()
            val schedule = document.toObject(SavedSchedule::class.java)
            Log.d("ScheduleRepository", "Loaded schedule by ID: $scheduleId -> ${schedule != null}")
            schedule
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error loading schedule by ID: $scheduleId", e)
            null
        }
    }

    suspend fun updateSchedule(schedule: SavedSchedule): Result<Unit> {
        return try {
            schedulesCollection.document(schedule.id).set(schedule).await()
            Log.d("ScheduleRepository", "Schedule Updated Successfully: ${schedule.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error Updating Schedule", e)
            Result.failure(e)
        }
    }

    fun observeSchedules(): Flow<List<SavedSchedule>> = callbackFlow {
        val listener: ListenerRegistration = schedulesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ScheduleRepository", "Error observing schedules", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val schedules = snapshot.documents.mapNotNull { document ->
                        document.toObject(SavedSchedule::class.java)
                    }
                    trySend(schedules)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        return try {
            schedulesCollection.document(scheduleId).delete().await()
            Log.d("ScheduleRepository", "Schedule deleted successfully: $scheduleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error deleting schedule", e)
            Result.failure(e)
        }
    }

    suspend fun updateScheduleStatus(scheduleId: String, isActive: Boolean): Result<Unit> {
        return try {
            schedulesCollection.document(scheduleId)
                .update("isActive", isActive)
                .await()
            Log.d("ScheduleRepository", "Schedule status updated: $scheduleId -> $isActive")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ScheduleRepository", "Error updating schedule status", e)
            Result.failure(e)
        }
    }
}