package insa.eda.database.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import insa.eda.database.local.entities.DrivingRecordEntity
import java.util.Date

@Dao
interface DrivingRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DrivingRecordEntity): Long
    
    @Update
    suspend fun update(record: DrivingRecordEntity): Int
    
    @Query("SELECT * FROM driving_records WHERE recordId = :recordId")
    suspend fun getRecordById(recordId: String): DrivingRecordEntity?
    
    @Query("SELECT * FROM driving_records WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllRecordsByUserId(userId: String): List<DrivingRecordEntity>
    
    @Query("SELECT * FROM driving_records WHERE userId = :userId AND endTime IS NULL")
    suspend fun getActiveRecordByUserId(userId: String): DrivingRecordEntity?
    
    @Query("SELECT * FROM driving_records WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentRecordsByUserId(userId: String, limit: Int): List<DrivingRecordEntity>
    
    @Query("SELECT * FROM driving_records WHERE startTime BETWEEN :startDate AND :endDate AND userId = :userId")
    suspend fun getRecordsByDateRange(startDate: Date, endDate: Date, userId: String): List<DrivingRecordEntity>
    
    @Query("SELECT AVG(ecoScore) FROM driving_records WHERE userId = :userId AND ecoScore > 0")
    suspend fun getAverageEcoScore(userId: String): Float
    
    @Query("SELECT * FROM driving_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<DrivingRecordEntity>
    
    @Query("UPDATE driving_records SET isSynced = 1, lastSyncTimeStamp = :timestamp WHERE recordId = :recordId")
    suspend fun markRecordSynced(recordId: String, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("SELECT SUM(distance) FROM driving_records WHERE userId = :userId AND endTime IS NOT NULL")
    suspend fun getTotalDistance(userId: String): Float?
    
    @Query("SELECT COUNT(*) FROM driving_records WHERE userId = :userId AND endTime IS NOT NULL")
    suspend fun getCompletedDrivingSessionsCount(userId: String): Int
}
