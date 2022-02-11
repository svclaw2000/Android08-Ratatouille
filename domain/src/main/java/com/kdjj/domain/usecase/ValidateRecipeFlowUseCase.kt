package com.kdjj.domain.usecase

import com.kdjj.domain.common.RecipeValidator
import com.kdjj.domain.model.request.ValidateRecipeFlowRequest
import com.kdjj.domain.model.response.ValidateRecipeFlowResponse
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ValidateRecipeFlowUseCase @Inject constructor(
    private val recipeValidator: RecipeValidator
) : UseCase<ValidateRecipeFlowRequest, ValidateRecipeFlowResponse> {

    override fun invoke(request: ValidateRecipeFlowRequest): ValidateRecipeFlowResponse {
        return ValidateRecipeFlowResponse(
            request.titleFlow.map { recipeValidator.validateTitle(it) },
            request.stuffFlow.map { recipeValidator.validateStuff(it) }
        )
    }
}