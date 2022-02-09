package com.kdjj.domain.model.response

sealed class ValidateRecipeResponse : Response {

    object Valid : ValidateRecipeResponse()

    data class Invalid(val firstInvalidPosition: Int) : ValidateRecipeResponse()
}

