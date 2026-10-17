package com.mrdastbaaf.mdbapp.features.catalogue.data

import com.mrdastbaaf.mdbapp.features.catalogue.domain.repository.CatalogueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds the catalogue repository at the edge, same pattern as the other features. */
@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogueDataModule {

    @Binds
    abstract fun bindCatalogueRepository(impl: CatalogueRepositoryImpl): CatalogueRepository
}
