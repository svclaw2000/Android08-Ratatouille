package com.kdjj.domain.usecase

import com.kdjj.domain.model.Recipe
import com.kdjj.domain.repository.RecipeListRepository
import com.kdjj.domain.model.request.FetchLocalSearchRecipeListRequest
import javax.inject.Inject

internal class FetchLocalSearchRecipeListUseCase @Inject constructor(
    private val recipeListRepository: RecipeListRepository,
) : UseCase<FetchLocalSearchRecipeListRequest, @JvmSuppressWildcards List<Recipe>> {

    override suspend fun invoke(request: FetchLocalSearchRecipeListRequest): Result<List<Recipe>> =
        recipeListRepository.fetchLocalSearchRecipeListAfter(request.keyword, request.index)
}
