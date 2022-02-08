package com.kdjj.domain.model.request

import kotlinx.coroutines.flow.Flow

data class ValidateRecipeStepFlowRequest(
    val nameFlow: Flow<String>,
    val descriptionFlow: Flow<String>
) : Request