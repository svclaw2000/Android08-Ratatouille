package com.kdjj.domain.common

import javax.inject.Inject

class RecipeStepValidator @Inject constructor() {

    fun validateName(name: String): Boolean {
        return name.isNotBlank()
    }

    fun validateDescription(description: String): Boolean {
        return description.isNotBlank()
    }
}