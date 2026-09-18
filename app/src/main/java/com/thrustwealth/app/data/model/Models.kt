package com.thrustwealth.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "instruments")
data class Instrument(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val contractSize: Double,
    val unit: String,
    val pipSize: Double,
    val tickSize: Double,
    val digits: Int,
    val baseCurrency: String,
    val quoteCurrency: String,
    val defaultPrice: Double,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
)

enum class TradeDirection {
    LONG, SHORT;

    fun displayName(): String = when (this) {
        LONG -> "Long"
        SHORT -> "Short"
    }
}

enum class CalcMode {
    BY_RISK, BY_LOTS
}

enum class ThemeMode {
    DARK, LIGHT, AUTO
}

@Serializable
data class AppPreferences(
    val accountCurrency: String = "USD",
    val defaultSymbol: String = "XAUUSD",
    val defaultLeverage: String = "1:100",
    val lotUnitDisplay: String = "Standard",
    val theme: ThemeMode = ThemeMode.DARK
)

@Entity(tableName = "calculation_history")
data class CalculationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val mode: String,
    val balance: Double,
    val riskPercent: Double,
    val riskAmount: Double,
    val stopPips: Double,
    val entryPrice: Double,
    val direction: String,
    val lotSize: Double,
    val pipValue: Double,
    val notionalValue: Double,
    val stopLossPrice: Double?,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_journal")
data class TradeEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val direction: String,
    val lots: Double,
    val entryPrice: Double,
    val exitPrice: Double,
    val pnl: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class LotSizeResult(
    val isValid: Boolean,
    val suggestedLots: Double = 0.0,
    val rawLots: Double = 0.0,
    val miniLots: Double = 0.0,
    val microLots: Int = 0,
    val riskAmount: Double = 0.0,
    val riskPercent: Double = 0.0,
    val pipValue: Double = 0.0,
    val notionalValue: Double = 0.0,
    val stopLossPrice: Double? = null,
    val error: String? = null
)

data class PnlResult(
    val isValid: Boolean,
    val profitAmount: Double = 0.0,
    val profitPips: Double = 0.0,
    val priceDelta: Double = 0.0,
    val returnPercent: Double = 0.0,
    val notionalValue: Double = 0.0,
    val isProfit: Boolean = false
)

data class JournalStats(
    val netPnl: Double = 0.0,
    val winRate: Double = 0.0,
    val avgRr: Double = 0.0,
    val maxDrawdown: Double = 0.0,
    val tradeCount: Int = 0
)
