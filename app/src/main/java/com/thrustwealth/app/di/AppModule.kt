package com.thrustwealth.app.di

import android.content.Context
import androidx.room.Room
import com.thrustwealth.app.data.local.AppDatabase
import com.thrustwealth.app.data.local.HistoryDao
import com.thrustwealth.app.data.local.InstrumentDao
import com.thrustwealth.app.data.local.TradeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "thrust_wealth.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideTradeDao(db: AppDatabase): TradeDao = db.tradeDao()

    @Provides
    fun provideInstrumentDao(db: AppDatabase): InstrumentDao = db.instrumentDao()
}
