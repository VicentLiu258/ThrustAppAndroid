package com.thrustwealth.app.data.repository

import com.thrustwealth.app.data.local.HistoryDao
import com.thrustwealth.app.data.local.InstrumentDao
import com.thrustwealth.app.data.local.TradeDao
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.JournalStats
import com.thrustwealth.app.data.model.TradeEntry
import com.thrustwealth.app.engine.InstrumentConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun observeHistory(): Flow<List<CalculationRecord>> = historyDao.observeAll()

    suspend fun getAll(): List<CalculationRecord> = historyDao.getAll()

    suspend fun save(record: CalculationRecord) {
        historyDao.insert(record)
        historyDao.trimToFifty()
    }

    suspend fun delete(id: Long) = historyDao.deleteById(id)

    suspend fun clearAll() = historyDao.clearAll()
}

@Singleton
class TradeRepository @Inject constructor(
    private val tradeDao: TradeDao
) {
    fun observeTrades(): Flow<List<TradeEntry>> = tradeDao.observeAll()

    fun observeStats(): Flow<JournalStats> = tradeDao.observeAll().map { trades ->
        computeStats(trades)
    }

    suspend fun getAll(): List<TradeEntry> = tradeDao.getAll()

    suspend fun insert(entry: TradeEntry) = tradeDao.insert(entry)

    suspend fun update(entry: TradeEntry) = tradeDao.update(entry)

    suspend fun delete(id: Long) = tradeDao.deleteById(id)

    suspend fun clearAll() = tradeDao.clearAll()

    companion object {
        fun computeStats(trades: List<TradeEntry>): JournalStats {
            if (trades.isEmpty()) return JournalStats()
            val netPnl = trades.sumOf { it.pnl }
            val wins = trades.count { it.pnl > 0 }
            val winRate = wins.toDouble() / trades.size * 100.0

            val profits = trades.filter { it.pnl > 0 }.map { it.pnl }
            val losses = trades.filter { it.pnl < 0 }.map { abs(it.pnl) }
            val avgWin = if (profits.isNotEmpty()) profits.average() else 0.0
            val avgLoss = if (losses.isNotEmpty()) losses.average() else 0.0
            val avgRr = if (avgLoss > 0) avgWin / avgLoss else 0.0

            var equity = 0.0
            var peak = 0.0
            var maxDd = 0.0
            trades.sortedBy { it.createdAt }.forEach { trade ->
                equity += trade.pnl
                peak = max(peak, equity)
                maxDd = max(maxDd, peak - equity)
            }

            return JournalStats(
                netPnl = netPnl,
                winRate = winRate,
                avgRr = avgRr,
                maxDrawdown = maxDd,
                tradeCount = trades.size
            )
        }
    }
}

@Singleton
class InstrumentRepository @Inject constructor(
    private val instrumentDao: InstrumentDao
) {
    fun observeInstruments(): Flow<List<Instrument>> = instrumentDao.observeAll()

    suspend fun getAll(): List<Instrument> = instrumentDao.getAll()

    suspend fun getById(id: String): Instrument? = instrumentDao.getById(id)

    suspend fun seedIfNeeded() {
        if (instrumentDao.count() == 0) {
            instrumentDao.upsertAll(InstrumentConfig.DEFAULT_INSTRUMENTS)
        }
    }

    suspend fun upsert(instrument: Instrument) = instrumentDao.upsert(instrument)

    suspend fun setFavorite(id: String, favorite: Boolean) =
        instrumentDao.setFavorite(id, favorite)

    suspend fun deleteCustom(id: String) = instrumentDao.deleteCustom(id)
}
