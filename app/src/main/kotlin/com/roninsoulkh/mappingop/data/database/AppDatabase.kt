package com.roninsoulkh.mappingop.data.database

import android.content.Context
import androidx.room.*
import com.roninsoulkh.mappingop.domain.models.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [Worksheet::class, Consumer::class, WorkResult::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun worksheetDao(): WorksheetDao
    abstract fun consumerDao(): ConsumerDao
    abstract fun workResultDao(): WorkResultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mappingop_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface WorksheetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorksheet(worksheet: Worksheet)

    @Update
    suspend fun updateWorksheet(worksheet: Worksheet)

    @Query("SELECT * FROM worksheet ORDER BY import_date DESC")
    fun getAllWorksheets(): Flow<List<Worksheet>>

    @Query("SELECT * FROM worksheet ORDER BY import_date DESC")
    suspend fun getAllWorksheetsSync(): List<Worksheet>

    @Query("SELECT * FROM worksheet WHERE id = :id")
    suspend fun getWorksheetById(id: String): Worksheet?

    @Delete
    suspend fun deleteWorksheet(worksheet: Worksheet)

    @Query("DELETE FROM worksheet")
    suspend fun deleteAllWorksheets()
}

@Dao
interface ConsumerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumer(consumer: Consumer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllConsumers(consumers: List<Consumer>)

    @Update
    suspend fun updateConsumer(consumer: Consumer)

    @Query("SELECT * FROM consumer WHERE worksheet_id = :worksheetId ORDER BY or_number")
    fun getConsumersByWorksheetId(worksheetId: String): Flow<List<Consumer>>

    @Query("SELECT * FROM consumer WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    fun getConsumersWithCoordinates(): Flow<List<Consumer>>

    @Query("SELECT * FROM consumer WHERE id = :id")
    suspend fun getConsumerById(id: String): Consumer?

    @Query("SELECT * FROM consumer WHERE worksheet_id = :worksheetId ORDER BY or_number")
    suspend fun getConsumersByWorksheetIdSync(worksheetId: String): List<Consumer>

    @Query("UPDATE consumer SET is_processed = :isProcessed WHERE id = :consumerId")
    suspend fun updateProcessedStatus(consumerId: String, isProcessed: Boolean)

    @Query("DELETE FROM consumer")
    suspend fun deleteAllConsumers()

    @Update
    suspend fun update(consumer: Consumer)
}

@Dao
interface WorkResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkResult(workResult: WorkResult)

    @Query("SELECT * FROM workresult WHERE consumer_id = :consumerId")
    suspend fun getWorkResultByConsumerId(consumerId: String): WorkResult?

    @Query("SELECT * FROM workresult WHERE worksheet_id = :worksheetId")
    suspend fun getWorkResultsByWorksheetId(worksheetId: String): List<WorkResult>

    @Query("DELETE FROM workresult")
    suspend fun deleteAllWorkResults()
}