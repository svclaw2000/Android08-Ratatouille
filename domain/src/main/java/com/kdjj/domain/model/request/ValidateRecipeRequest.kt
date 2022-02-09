package com.kdjj.domain.model.request

import com.kdjj.domain.model.Recipe

data class ValidateRecipeRequest(
    val recipe: Recipe
) : Request