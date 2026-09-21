package com.thrustwealth.app.engine

import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.LotSizeResult
import com.thrustwealth.app.data.model.PnlResult
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

object CalculationEngine {

    fun calculatePipValue(instrument: Instrument, accountCurrency: String = "USD"): Double {
        val pipValueQuote = instrument.contractSize * instrument.pipSize
        if (pipValueQuote <= 0) return 0.0

        if (instrument.quoteCurrency == accountCurrency) {
            return pipValueQuote
        }

        if (instrument.quoteCurrency == "JPY" && accountCurrency == "USD") {
            val rate = InstrumentConfig.REFERENCE_EXCHANGE_RATES["USDJPY"] ?: 154.50
            return pipValueQuote / rate
        }

        val pairDirect = "$accountCurrency${instrument.quoteCurrency}"
        InstrumentConfig.REFERENCE_EXCHANGE_RATES[pairDirect]?.let {
            return pipValueQuote / it
        }

        val pairInverse = "${instrument.quoteCurrency}$accountCurrency"
        InstrumentConfig.REFERENCE_EXCHANGE_RATES[pairInverse]?.let {
            return pipValueQuote * it
        }

        return pipValueQuote
    }

    fun calculateLotSize(
        balance: Double,
        riskPercent: Double,
        stopPips: Double,
        instrument: Instrument?,
        entryPrice: Double = 0.0,
        direction: String = "long",
        accountCurrency: String = "USD"
    ): LotSizeResult {
        if (instrument == null || balance <= 0 || riskPercent < 0.01 || riskPercent > 100 || stopPips <= 0) {
            return LotSizeResult(
                isValid = false,
                riskAmount = balance * (riskPercent / 100.0),
                riskPercent = riskPercent,
                error = "Balance must be positive; risk must be 0.01–100%; stop loss must be positive"
            )
        }

        val riskAmount = balance * (riskPercent / 100.0)
        val pipValue = calculatePipValue(instrument, accountCurrency)
        if (pipValue <= 0) {
            return LotSizeResult(isValid = false, error = "Invalid instrument pip value")
        }

        val price = if (entryPrice > 0) entryPrice else instrument.defaultPrice
        val rawLots = riskAmount / (stopPips * pipValue)
        if (rawLots > 100) {
            return LotSizeResult(
                isValid = false,
                rawLots = rawLots,
                riskAmount = riskAmount,
                riskPercent = riskPercent,
                pipValue = pipValue,
                error = "Calculated lot size exceeds the 100-lot maximum"
            )
        }
        val suggestedLots = max(0.01, floor(rawLots * 100.0) / 100.0)
        val notionalValue = suggestedLots * instrument.contractSize * price

        val stopLossPrice = if (price > 0) {
            val priceShift = stopPips * instrument.pipSize
            val sl = if (direction.equals("long", ignoreCase = true)) {
                price - priceShift
            } else {
                price + priceShift
            }
            roundToDigits(sl, instrument.digits)
        } else null

        return LotSizeResult(
            isValid = true,
            suggestedLots = suggestedLots,
            rawLots = rawLots,
            miniLots = round(suggestedLots * 10.0 * 10.0) / 10.0,
            microLots = round(suggestedLots * 100.0).toInt(),
            riskAmount = riskAmount,
            riskPercent = riskPercent,
            pipValue = pipValue,
            notionalValue = notionalValue,
            stopLossPrice = stopLossPrice
        )
    }

    fun calculateRiskFromLots(
        balance: Double,
        lotSize: Double,
        stopPips: Double,
        instrument: Instrument?,
        entryPrice: Double = 0.0,
        direction: String = "long",
        accountCurrency: String = "USD"
    ): LotSizeResult {
        if (instrument == null || balance <= 0 || lotSize < 0.01 || lotSize > 100 || stopPips <= 0) {
            return LotSizeResult(
                isValid = false,
                error = "Balance must be positive; lots must be 0.01–100; stop loss must be positive"
            )
        }

        val pipValue = calculatePipValue(instrument, accountCurrency)
        val riskAmount = stopPips * pipValue * lotSize
        val riskPercent = (riskAmount / balance) * 100.0
        val price = if (entryPrice > 0) entryPrice else instrument.defaultPrice
        val notionalValue = lotSize * instrument.contractSize * price
        if (riskPercent > 100) {
            return LotSizeResult(
                isValid = false,
                suggestedLots = lotSize,
                rawLots = lotSize,
                riskAmount = riskAmount,
                riskPercent = riskPercent,
                pipValue = pipValue,
                notionalValue = notionalValue,
                error = "Calculated risk exceeds the 100% maximum"
            )
        }

        val stopLossPrice = if (price > 0) {
            val priceShift = stopPips * instrument.pipSize
            val sl = if (direction.equals("long", ignoreCase = true)) {
                price - priceShift
            } else {
                price + priceShift
            }
            roundToDigits(sl, instrument.digits)
        } else null

        return LotSizeResult(
            isValid = true,
            suggestedLots = lotSize,
            miniLots = round(lotSize * 10.0 * 10.0) / 10.0,
            microLots = round(lotSize * 100.0).toInt(),
            riskAmount = riskAmount,
            riskPercent = roundToDigits(riskPercent, 2),
            pipValue = pipValue,
            notionalValue = notionalValue,
            stopLossPrice = stopLossPrice
        )
    }

    fun calculateProfitLoss(
        instrument: Instrument?,
        direction: String = "long",
        openPrice: Double,
        closePrice: Double,
        lotSize: Double,
        takeProfitPrice: Double? = null,
        stopLossPrice: Double? = null,
        accountCurrency: String = "USD"
    ): PnlResult {
        if (instrument == null || openPrice <= 0 || closePrice <= 0 || lotSize < 0.01 || lotSize > 100) {
            return PnlResult(
                isValid = false,
                error = "Prices must be positive and lots must be 0.01–100"
            )
        }

        val priceDelta = if (direction.equals("long", ignoreCase = true)) {
            closePrice - openPrice
        } else {
            openPrice - closePrice
        }

        val profitPips = priceDelta / instrument.pipSize
        val pipValue = calculatePipValue(instrument, accountCurrency)
        val profitAmount = profitPips * pipValue * lotSize
        val notionalValue = lotSize * instrument.contractSize * openPrice
        val returnPercent = if (notionalValue > 0) (profitAmount / notionalValue) * 100.0 else 0.0
        val takeProfit = calculateTarget(
            takeProfitPrice, openPrice, lotSize, instrument, direction, pipValue
        )
        val stopLoss = calculateTarget(
            stopLossPrice, openPrice, lotSize, instrument, direction, pipValue
        )

        return PnlResult(
            isValid = true,
            priceDelta = roundToDigits(priceDelta, instrument.digits),
            profitPips = roundToDigits(profitPips, 1),
            profitAmount = roundToDigits(profitAmount, 2),
            returnPercent = roundToDigits(returnPercent, 2),
            notionalValue = notionalValue,
            isProfit = profitAmount >= 0,
            takeProfitAmount = takeProfit?.first,
            takeProfitPips = takeProfit?.second,
            stopLossAmount = stopLoss?.first,
            stopLossPips = stopLoss?.second
        )
    }

    private fun calculateTarget(
        targetPrice: Double?,
        openPrice: Double,
        lotSize: Double,
        instrument: Instrument,
        direction: String,
        pipValue: Double
    ): Pair<Double, Double>? {
        if (targetPrice == null || targetPrice <= 0) return null
        val delta = if (direction.equals("long", ignoreCase = true)) {
            targetPrice - openPrice
        } else {
            openPrice - targetPrice
        }
        val pips = delta / instrument.pipSize
        return roundToDigits(pips * pipValue * lotSize, 2) to roundToDigits(pips, 1)
    }

    private fun roundToDigits(value: Double, digits: Int): Double {
        var factor = 1.0
        repeat(digits) { factor *= 10.0 }
        return round(value * factor) / factor
    }
}
