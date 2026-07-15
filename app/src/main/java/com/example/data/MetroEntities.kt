package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phoneNumber: String,
    val walletBalance: Double,
    val preferredLanguage: String
)

@Entity(tableName = "booked_tickets")
data class BookedTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceStation: String,
    val destStation: String,
    val fare: Double,
    val ticketType: String, // "SINGLE", "RETURN", "PASS"
    val timestamp: Long,
    val qrCodeData: String,
    val status: String, // "ACTIVE", "USED", "EXPIRED"
    val distance: Double,
    val duration: Int
)

@Dao
interface MetroDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM booked_tickets ORDER BY timestamp DESC")
    fun getBookedTicketsFlow(): Flow<List<BookedTicket>>

    @Query("SELECT * FROM booked_tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: Int): BookedTicket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookedTicket(ticket: BookedTicket): Long
}

@Database(entities = [UserProfile::class, BookedTicket::class], version = 1, exportSchema = false)
abstract class MetroDatabase : RoomDatabase() {
    abstract fun metroDao(): MetroDao
}
