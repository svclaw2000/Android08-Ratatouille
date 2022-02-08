package com.kdjj.domain.model.response

import kotlinx.coroutines.flow.Flow

data class ValidateRecipeStepFlowResponse(
    val nameFlow: Flow<Boolean>,
    val descriptionFlow: Flow<Boolean>
) : Response