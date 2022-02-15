package com.kdjj.domain.usecase

import com.kdjj.domain.common.RecipeStepValidator
import com.kdjj.domain.common.RecipeValidator
import com.kdjj.domain.model.Recipe
import com.kdjj.domain.model.RecipeStep
import com.kdjj.domain.model.request.ValidateRecipeRequest
import com.kdjj.domain.model.response.ValidateRecipeResponse
import javax.inject.Inject

internal class ValidateRecipeUseCase @Inject constructor(
    private val recipeValidator: RecipeValidator,
    private val stepValidator: RecipeStepValidator
) : UseCase<ValidateRecipeRequest, ValidateRecipeResponse> {

    override fun invoke(request: ValidateRecipeRequest): ValidateRecipeResponse {
        val recipe = request.recipe
        val invalidStepIdx = recipe.stepList.indexOfFirst { !validateRecipeStep(it) }

        return when {
            !validateRecipeMeta(recipe) -> ValidateRecipeResponse.Invalid(0)
            invalidStepIdx >= 0 -> ValidateRecipeResponse.Invalid(invalidStepIdx + 1)
            else -> ValidateRecipeResponse.Valid
        }
    }

    private fun validateRecipeMeta(recipe: Recipe) =
        recipeValidator.validateTitle(recipe.title) && recipeValidator.validateStuff(recipe.stuff)

    private fun validateRecipeStep(step: RecipeStep) =
        stepValidator.validateName(step.name) && stepValidator.validateDescription(step.description)
}