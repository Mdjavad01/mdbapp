package com.mrdastbaaf.mdbapp.features.home.data

import com.mrdastbaaf.mdbapp.features.home.domain.repository.VersionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the repository interface to its implementation.
 *
 * Kept in the feature's `data` package rather than in a global module so that
 * the dependency direction stays visible: presentation depends on domain,
 * domain knows nothing, and data is wired in at the edge.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {

    @Binds
    abstract fun bindVersionRepository(impl: VersionRepositoryImpl): VersionRepository
}
