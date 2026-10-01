package com.samdori93.yeoksadam.core.data.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.samdori93.yeoksadam.core.common.dispatcher.DefaultDispatcherProvider
import com.samdori93.yeoksadam.core.common.dispatcher.DispatcherProvider
import com.samdori93.yeoksadam.core.data.repository.CatalogRepositoryImpl
import com.samdori93.yeoksadam.core.data.repository.ChatRepositoryImpl
import com.samdori93.yeoksadam.core.data.repository.SettingsRepositoryImpl
import com.samdori93.yeoksadam.core.domain.repository.CatalogRepository
import com.samdori93.yeoksadam.core.domain.repository.SettingsRepository
import com.samdori93.yeoksadam.core.data.repository.FigureRepositoryImpl
import com.samdori93.yeoksadam.core.data.repository.DiscoveryRepositoryImpl
import com.samdori93.yeoksadam.core.data.repository.HeritageRepositoryImpl
import com.samdori93.yeoksadam.core.data.repository.LocationRepositoryImpl
import com.samdori93.yeoksadam.core.data.repository.VisionRepositoryImpl
import com.samdori93.yeoksadam.core.domain.repository.ChatRepository
import com.samdori93.yeoksadam.core.domain.repository.DiscoveryRepository
import com.samdori93.yeoksadam.core.domain.repository.FigureRepository
import com.samdori93.yeoksadam.core.domain.repository.HeritageRepository
import com.samdori93.yeoksadam.core.domain.repository.LocationRepository
import com.samdori93.yeoksadam.core.domain.repository.VisionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindFigureRepository(impl: FigureRepositoryImpl): FigureRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindHeritageRepository(impl: HeritageRepositoryImpl): HeritageRepository

    @Binds
    @Singleton
    abstract fun bindVisionRepository(impl: VisionRepositoryImpl): VisionRepository

    @Binds
    @Singleton
    abstract fun bindDiscoveryRepository(impl: DiscoveryRepositoryImpl): DiscoveryRepository

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    companion object {
        @Provides
        @Singleton
        fun provideFusedLocationClient(
            @ApplicationContext context: Context,
        ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }
}