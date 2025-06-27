package insa.eda.database.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import insa.eda.database.models.DrivingRecord
import java.util.Date

@Entity(
    tableName = "driving_records",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["uid"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class DrivingRecordEntity(
    @PrimaryKey
    val recordId: String,
    val userId: String,
    val startTime: Date?,
    val endTime: Date?,
    val duration: Int = 0,
    val distance: Float = 0f,
    val avgSpeed: Float = 0f,
    val fuelEfficiency: Float = 0f,
    val co2Emission: Float = 0f,
    val co2Saved: Float = 0f,
    val ecoScore: Int = 0,
    val rapidAcceleration: Int = 0,
    val hardBraking: Int = 0,
    val sharpTurns: Int = 0,
    val idlingTime: Int = 0,
    val createdAt: Date?,
    val isSynced: Boolean = false,
    val lastSyncTimeStamp: Long = System.currentTimeMillis()
) {
    fun toDrivingRecord(): DrivingRecord {
        return DrivingRecord(
            recordId = recordId,
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            distance = distance,
            avgSpeed = avgSpeed,
            fuelEfficiency = fuelEfficiency,
            co2Emission = co2Emission,
            co2Saved = co2Saved,
            ecoScore = ecoScore,
            rapidAcceleration = rapidAcceleration,
            hardBraking = hardBraking,
            sharpTurns = sharpTurns,
            idlingTime = idlingTime,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDrivingRecord(record: DrivingRecord, isSynced: Boolean = false): DrivingRecordEntity {
            return DrivingRecordEntity(
                recordId = record.recordId!!,
                userId = record.userId,
                startTime = record.startTime,
                endTime = record.endTime,
                duration = record.duration,
                distance = record.distance,
                avgSpeed = record.avgSpeed,
                fuelEfficiency = record.fuelEfficiency,
                co2Emission = record.co2Emission,
                co2Saved = record.co2Saved,
                ecoScore = record.ecoScore,
                rapidAcceleration = record.rapidAcceleration,
                hardBraking = record.hardBraking,
                sharpTurns = record.sharpTurns,
                idlingTime = record.idlingTime,
                createdAt = record.createdAt,
                isSynced = isSynced
            )
        }
    }
}
