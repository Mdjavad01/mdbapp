package com.mrdastbaaf.mdbapp.features.auth.data

import com.mrdastbaaf.mdbapp.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the auth repository interface to its implementation.
 *
 * Kept in the feature's `data` package rather than in a global module so the
 * dependency direction stays visible: presentation depends on domain, domain
 * knows nothing, and data is wired in at the edge.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
