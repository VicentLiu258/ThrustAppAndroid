package com.thrustwealth.app.engine

import com.thrustwealth.app.data.model.Instrument

object InstrumentConfig {
    val DEFAULT_INSTRUMENTS: List<Instrument> = listOf(
        Instrument(
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
            defaultPrice = 2350.00,
            isFavorite = true
        ),
        Instrument(
            id = "XAGUSD",
            name = "Silver vs US Dollar",
            category = "Metals",
            contractSize = 5000.0,
            unit = "oz",
            pipSize = 0.01,
            tickSize = 0.001,
            digits = 3,
            baseCurrency = "XAG",
            quoteCurrency = "USD",
            defaultPrice = 28.50,
            isFavorite = true
        ),
        Instrument(
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
            defaultPrice = 1.0850,
            isFavorite = true
        ),
        Instrument(
            id = "GBPUSD",
            name = "British Pound vs US Dollar",
            category = "Forex",
            contractSize = 100000.0,
            unit = "units",
            pipSize = 0.0001,
            tickSize = 0.00001,
            digits = 5,
            baseCurrency = "GBP",
            quoteCurrency = "USD",
            defaultPrice = 1.2950,
            isFavorite = true
        ),
        Instrument(
            id = "USDJPY",
            name = "US Dollar vs Japanese Yen",
            category = "Forex",
            contractSize = 100000.0,
            unit = "units",
            pipSize = 0.01,
            tickSize = 0.001,
            digits = 3,
            baseCurrency = "USD",
            quoteCurrency = "JPY",
            defaultPrice = 154.50,
            isFavorite = false
        ),
        Instrument(
            id = "AUDUSD",
            name = "Australian Dollar vs US Dollar",
            category = "Forex",
            contractSize = 100000.0,
            unit = "units",
            pipSize = 0.0001,
            tickSize = 0.00001,
            digits = 5,
            baseCurrency = "AUD",
            quoteCurrency = "USD",
            defaultPrice = 0.6650,
            isFavorite = false
        ),
        Instrument(
            id = "US30",
            name = "Wall Street 30 (Dow Jones)",
            category = "Indices & Energy",
            contractSize = 1.0,
            unit = "points",
            pipSize = 1.00,
            tickSize = 0.1,
            digits = 1,
            baseCurrency = "USD",
            quoteCurrency = "USD",
            defaultPrice = 40500.0,
            isFavorite = false
        ),
        Instrument(
            id = "NAS100",
            name = "US Tech 100 (Nasdaq)",
            category = "Indices & Energy",
            contractSize = 1.0,
            unit = "points",
            pipSize = 1.00,
            tickSize = 0.25,
            digits = 2,
            baseCurrency = "USD",
            quoteCurrency = "USD",
            defaultPrice = 19800.0,
            isFavorite = false
        ),
        Instrument(
            id = "USOIL",
            name = "WTI Crude Oil",
            category = "Indices & Energy",
            contractSize = 1000.0,
            unit = "barrels",
            pipSize = 0.01,
            tickSize = 0.01,
            digits = 2,
            baseCurrency = "USD",
            quoteCurrency = "USD",
            defaultPrice = 78.50,
            isFavorite = false
        )
    )

    val REFERENCE_EXCHANGE_RATES = mapOf(
        "USDJPY" to 154.50,
        "EURUSD" to 1.0850,
        "GBPUSD" to 1.2950,
        "AUDUSD" to 0.6650,
        "USDCAD" to 1.3650,
        "USDCHF" to 0.8950
    )

    val QUICK_SYMBOLS = listOf("XAUUSD", "XAGUSD", "EURUSD", "GBPUSD", "USDJPY", "US30")
}
