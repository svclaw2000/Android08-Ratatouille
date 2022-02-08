package com.kdjj.domain.model.response

import kotlinx.coroutines.flow.Flow

data class ValidateRecipeFlowResponse(
    val titleFlow: Flow<Boolean>,
    val stuffFlow: Flow<Boolean>
) : Response