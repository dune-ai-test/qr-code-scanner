package com.quickscan.di

import android.content.Context
import androidx.room.Room
import com.quickscan.data.barcode.ZxingDecoder
import com.quickscan.data.local.QuickScanDatabase
import com.quickscan.data.local.ScanDao
import com.quickscan.data.local.SettingsStore
import com.quickscan.data.repository.ScanRepository
import com.quickscan.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuickScanDatabase =
        Room.databaseBuilder(context, QuickScanDatabase::class.java, QuickScanDatabase.NAME)
            .addMigrations(QuickScanDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideScanDao(database: QuickScanDatabase): ScanDao = database.scanDao()

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStore(context)
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideScanRepository(
        scanDao: ScanDao,
        settingsStore: SettingsStore,
    ): ScanRepository = ScanRepository(scanDao, settingsStore.retentionDays)

    @Provides
    @Singleton
    fun provideSettingsRepository(settingsStore: SettingsStore): SettingsRepository =
        SettingsRepository(settingsStore)


    @Provides
    @Singleton
    fun provideZxingDecoder(): ZxingDecoder = ZxingDecoder()
}