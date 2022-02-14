package com.kdjj.domain.usecase

import com.kdjj.domain.common.RecipeStepValidator
import com.kdjj.domain.model.request.ValidateRecipeStepFlowRequest
import com.kdjj.domain.model.response.ValidateRecipeStepFlowResponse
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ValidateRecipeStepFlowUseCase @Inject constructor(
    private val stepValidator: RecipeStepValidator
) : UseCase<ValidateRecipeStepFlowRequest, ValidateRecipeStepFlowResponse> {

    override fun invoke(request: ValidateRecipeStepFlowRequest): ValidateRecipeStepFlowResponse {
        return ValidateRecipeStepFlowResponse(
            nameFlow = request.nameFlow.map { stepValidator.validateName(it) },
            descriptionFlow = request.descriptionFlow.map { stepValidator.validateDescription(it) }
        )
    }
}