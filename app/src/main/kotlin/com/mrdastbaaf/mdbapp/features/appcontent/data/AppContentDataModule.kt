package com.mrdastbaaf.mdbapp.features.appcontent.data

import com.mrdastbaaf.mdbapp.features.appcontent.domain.repository.AppContentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppContentDataModule {

    @Binds
    abstract fun bindAppContentRepository(impl: AppContentRepositoryImpl): AppContentRepository
}
