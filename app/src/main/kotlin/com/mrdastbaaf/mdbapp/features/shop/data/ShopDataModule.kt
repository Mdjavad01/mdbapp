package com.mrdastbaaf.mdbapp.features.shop.data

import com.mrdastbaaf.mdbapp.features.shop.domain.repository.ShopRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ShopDataModule {

    @Binds
    abstract fun bindShopRepository(impl: ShopRepositoryImpl): ShopRepository
}
