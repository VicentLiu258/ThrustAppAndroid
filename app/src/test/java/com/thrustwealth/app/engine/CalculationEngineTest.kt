package com.thrustwealth.app.engine

import com.thrustwealth.app.data.model.Instrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationEngineTest {

    private val xauusd = Instrument(
        id = "XAUUSD",
        name = "Gold vs US Dollar",
        category = "Metals",
        contractSize = 100.0,
        unit = "oz",
        pipSize = 0.10,
        tickSize = 0.01,
        digits = 2,
        baseCurrency = "XAU",
        quoteCurrency = "USD",
        defaultPrice = 2350.00
    )

    private val eurusd = Instrument(
        id = "EURUSD",
        name = "Euro vs US Dollar",
        category = "Forex",
        contractSize = 100000.0,
        unit = "units",
        pipSize = 0.0001,
        tickSize = 0.00001,
        digits = 5,
        baseCurrency = "EUR",
        quoteCurrency = "USD",
        defaultPrice = 1.0850
    )

    @Test
    fun pipValue_xauusd_is_10() {
        val pipValue = CalculationEngine.calculatePipValue(xauusd, "USD")
        assertEquals(10.0, pipValue, 0.001)
    }

    @Test
    fun pipValue_eurusd_is_10() {
        val pipValue = CalculationEngine.calculatePipValue(eurusd, "USD")
        assertEquals(10.0, pipValue, 0.001)
    }

    @Test
    fun lotSize_example_from_spec() {
        // Balance $10,000, Risk 1%, XAUUSD, SL 50 pips → 0.20 lots
        val result = CalculationEngine.calculateLotSize(
            balance = 10000.0,
            riskPercent = 1.0,
            stopPips = 50.0,
            instrument = xauusd,
            entryPrice = 2350.0,
            direction = "long"
        )
        assertTrue(result.isValid)
        assertEquals(0.20, result.suggestedLots, 0.001)
        assertEquals(100.0, result.riskAmount, 0.01)
        assertEquals(10.0, result.pipValue, 0.01)
        assertEquals(47000.0, result.notionalValue, 1.0)
        assertEquals(2345.0, result.stopLossPrice!!, 0.01)
    }

    @Test
    fun riskFromLots_reverse() {
        val result = CalculationEngine.calculateRiskFromLots(
            balance = 10000.0,
            lotSize = 0.20,
            stopPips = 50.0,
            instrument = xauusd,
            entryPrice = 2350.0,
            direction = "long"
        )
        assertTrue(result.isValid)
        assertEquals(100.0, result.riskAmount, 0.01)
        assertEquals(1.0, result.riskPercent, 0.01)
    }

    @Test
    fun profitLoss_example_from_spec() {
        // XAUUSD Long 0.20 lots, Open 2350 → Close 2360 = +$200
        val result = CalculationEngine.calculateProfitLoss(
            instrument = xauusd,
            direction = "long",
            openPrice = 2350.0,
            closePrice = 2360.0,
            lotSize = 0.20
        )
        assertTrue(result.isValid)
        assertEquals(200.0, result.profitAmount, 0.01)
        assertEquals(100.0, result.profitPips, 0.1)
        assertTrue(result.isProfit)
        assertEquals(0.43, result.returnPercent, 0.01)
    }

    @Test
    fun profitLoss_target_prices_calculate_tp_and_sl() {
        val result = CalculationEngine.calculateProfitLoss(
            instrument = xauusd,
            direction = "long",
            openPrice = 2350.0,
            closePrice = 2360.0,
            lotSize = 0.20,
            takeProfitPrice = 2370.0,
            stopLossPrice = 2345.0
        )
        assertTrue(result.isValid)
        assertEquals(400.0, result.takeProfitAmount!!, 0.01)
        assertEquals(200.0, result.takeProfitPips!!, 0.1)
        assertEquals(-100.0, result.stopLossAmount!!, 0.01)
        assertEquals(-50.0, result.stopLossPips!!, 0.1)
    }

    @Test
    fun profitLoss_short_loss() {
        val result = CalculationEngine.calculateProfitLoss(
            instrument = xauusd,
            direction = "short",
            openPrice = 2350.0,
            closePrice = 2360.0,
            lotSize = 0.20
        )
        assertTrue(result.isValid)
        assertEquals(-200.0, result.profitAmount, 0.01)
        assertFalse(result.isProfit)
    }

    @Test
    fun invalid_inputs_return_invalid() {
        val result = CalculationEngine.calculateLotSize(
            balance = 0.0,
            riskPercent = 1.0,
            stopPips = 50.0,
            instrument = xauusd
        )
        assertFalse(result.isValid)
    }

    @Test
    fun bounds_are_enforced() {
        assertFalse(
            CalculationEngine.calculateLotSize(10000.0, 100.01, 50.0, xauusd).isValid
        )
        assertFalse(
            CalculationEngine.calculateRiskFromLots(10000.0, 0.001, 50.0, xauusd).isValid
        )
        assertFalse(
            CalculationEngine.calculateProfitLoss(xauusd, "long", 2350.0, 2360.0, 100.01).isValid
        )
    }
}
