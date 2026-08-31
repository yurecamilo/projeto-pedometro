package br.com.ritmo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_activity")
data class DailyActivity(
    val day: Long,
    val steps: Int,
    val distanceMeters: Double,
    val calories: Double
)

@Dao
interface ActivityDao {
    @Query("SELECT * FROM daily_activity WHERE day = :day LIMIT 1")
    suspend fun get(day: Long): DailyActivity?

    @Query("SELECT * FROM daily_activity WHERE day >= :from ORDER BY day")
    fun observeFrom(from: Long): Flow<List<DailyActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(activity: DailyActivity)
}

@Database(entities = [DailyActivity::class], version = 1, exportSchema = false)
abstract class ActivityDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        fun create(context: Context): ActivityDatabase = Room.databaseBuilder(
            context, ActivityDatabase::class.java, "ritmo.db"
        ).build()
    }
}
