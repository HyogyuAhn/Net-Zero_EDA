package insa.eda.database.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import insa.eda.database.local.dao.DrivingRecordDao
import insa.eda.database.local.dao.UserDao
import insa.eda.database.local.entities.DrivingRecordEntity
import insa.eda.database.local.entities.UserEntity
import insa.eda.database.local.utils.DateConverter

@Database(
    entities = [UserEntity::class, DrivingRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun drivingRecordDao(): DrivingRecordDao
    
    companion object {
        private const val DATABASE_NAME = "eda_app_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
