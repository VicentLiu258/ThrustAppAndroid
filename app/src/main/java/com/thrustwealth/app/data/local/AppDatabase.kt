package com.thrustwealth.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.TradeEntry

@Database(
    entities = [CalculationRecord::class, TradeEntry::class, Instrument::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun tradeDao(): TradeDao
    abstract fun instrumentDao(): InstrumentDao
}
