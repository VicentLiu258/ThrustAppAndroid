package com.thrustwealth.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.TradeEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY createdAt DESC LIMIT 50")
    fun observeAll(): Flow<List<CalculationRecord>>

    @Query("SELECT * FROM calculation_history ORDER BY createdAt DESC LIMIT 50")
    suspend fun getAll(): List<CalculationRecord>

    @Insert
    suspend fun insert(record: CalculationRecord): Long

    @Delete
    suspend fun delete(record: CalculationRecord)

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()

    @Query(
        """
        DELETE FROM calculation_history WHERE id NOT IN (
            SELECT id FROM calculation_history ORDER BY createdAt DESC LIMIT 50
        )
        """
    )
    suspend fun trimToFifty()
}

@Dao
interface TradeDao {
    @Query("SELECT * FROM trade_journal ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TradeEntry>>

    @Query("SELECT * FROM trade_journal ORDER BY createdAt DESC")
    suspend fun getAll(): List<TradeEntry>

    @Insert
    suspend fun insert(entry: TradeEntry): Long

    @Update
    suspend fun update(entry: TradeEntry)

    @Delete
    suspend fun delete(entry: TradeEntry)

    @Query("DELETE FROM trade_journal WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trade_journal")
    suspend fun clearAll()
}

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instruments ORDER BY category, id")
    fun observeAll(): Flow<List<Instrument>>

    @Query("SELECT * FROM instruments ORDER BY category, id")
    suspend fun getAll(): List<Instrument>

    @Query("SELECT * FROM instruments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Instrument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(instrument: Instrument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(instruments: List<Instrument>)

    @Update
    suspend fun update(instrument: Instrument)

    @Query("DELETE FROM instruments WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: String)

    @Query("UPDATE instruments SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT COUNT(*) FROM instruments")
    suspend fun count(): Int
}
