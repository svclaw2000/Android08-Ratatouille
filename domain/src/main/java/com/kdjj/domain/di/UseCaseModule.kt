package com.kdjj.domain.di

import com.kdjj.domain.model.request.*
import com.kdjj.domain.model.response.ValidateRecipeFlowResponse
import com.kdjj.domain.usecase.*
import dagger.Binds
import dagger.Module

@Module
abstract class UseCaseModule {

    @Binds
    internal abstract fun bindValidateRecipeFlowUseCase(
        validateRecipeFlowUseCase: ValidateRecipeFlowUseCase
    ) : UseCase<ValidateRecipeFlowRequest, ValidateRecipeFlowResponse>
}
