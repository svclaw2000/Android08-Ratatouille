package com.kdjj.ratatouille.di

import com.kdjj.domain.request.RecipeRequest
import com.kdjj.domain.usecase.SaveRecipeUseCase
import com.kdjj.domain.usecase.UseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

//    @Binds
//    abstract fun provideRecipeRepository(recipeRepositoryImpl: RecipeRepositoryImpl): RecipeRepository
    @Binds
    abstract fun provideSaveRecipeUseCase(saveRecipeUseCase: SaveRecipeUseCase): UseCase<RecipeRequest, Boolean>
}