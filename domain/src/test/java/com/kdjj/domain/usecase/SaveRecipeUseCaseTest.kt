package com.kdjj.domain.usecase

import com.kdjj.domain.common.IdGenerator
import com.kdjj.domain.model.*
import com.kdjj.domain.model.request.SaveRecipeRequest
import com.kdjj.domain.repository.RecipeImageRepository
import com.kdjj.domain.repository.RecipeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
class SaveRecipeUseCaseTest {

    @Mock private lateinit var recipeRepository: RecipeRepository
    @Mock private lateinit var imageRepository: RecipeImageRepository
    @Mock private lateinit var idGenerator: IdGenerator

    private lateinit var saveRecipeUseCase: ResultUseCase<SaveRecipeRequest, Unit>

    private val dummyRecipe = Recipe(
        DUMMY_STRING,
        DUMMY_STRING,
        RecipeType(DUMMY_INT, DUMMY_STRING),
        DUMMY_STRING,
        DUMMY_STRING,
        listOf(
            RecipeStep(
                DUMMY_STRING,
                DUMMY_STRING,
                RecipeStepType.COOK,
                DUMMY_STRING,
                DUMMY_STRING,
                DUMMY_INT
            )
        ),
        DUMMY_STRING,
        DUMMY_INT,
        DUMMY_BOOLEAN,
        DUMMY_LONG,
        RecipeState.CREATE
    )

    @Before
    fun setup() {
        saveRecipeUseCase = SaveRecipeUseCase(recipeRepository, imageRepository, idGenerator)
    }

    @Test
    fun saveRecipeUseCase_saveNewRecipe_stateLocal(): Unit = runBlocking {
        // given
        whenever(idGenerator.generateId()).thenReturn(DUMMY_STRING)
        whenever(imageRepository.copyExternalImageToInternal(any()))
            .thenReturn(Result.success(listOf(DUMMY_STRING, DUMMY_STRING)))
        whenever(recipeRepository.saveMyRecipe(any())).thenAnswer {
            val savedRecipe = it.arguments[0] as Recipe
            assertEquals(RecipeState.LOCAL, savedRecipe.state)
        }

        // when
        val result = saveRecipeUseCase(SaveRecipeRequest(dummyRecipe))

        // then
        assertTrue(result.isSuccess)
        verify(recipeRepository).saveMyRecipe(any())
    }

    @Test
    fun saveRecipeUseCase_editLocalRecipe_stateLocal(): Unit = runBlocking {
        // given
        whenever(idGenerator.generateId()).thenReturn(DUMMY_STRING)
        whenever(imageRepository.copyExternalImageToInternal(any()))
            .thenReturn(Result.success(listOf(DUMMY_STRING, DUMMY_STRING)))
        whenever(recipeRepository.getLocalRecipe(any()))
            .thenReturn(Result.success(dummyRecipe))
        whenever(recipeRepository.updateMyRecipe(any(), any())).thenAnswer {
            val savedRecipe = it.arguments[0] as Recipe
            assertEquals(RecipeState.LOCAL, savedRecipe.state)
        }

        // when
        val result = saveRecipeUseCase(SaveRecipeRequest(dummyRecipe.copy(
            state = RecipeState.LOCAL
        )))

        // then
        assertTrue(result.isSuccess)
        verify(recipeRepository).updateMyRecipe(any(), any())
    }

    @Test
    fun saveRecipeUseCase_editUploadedRecipe_stateUpload(): Unit = runBlocking {
        // given
        whenever(idGenerator.generateId()).thenReturn(DUMMY_STRING)
        whenever(imageRepository.copyExternalImageToInternal(any()))
            .thenReturn(Result.success(listOf(DUMMY_STRING, DUMMY_STRING)))
        whenever(recipeRepository.getLocalRecipe(any()))
            .thenReturn(Result.success(dummyRecipe))
        whenever(recipeRepository.updateMyRecipe(any(), any())).thenAnswer {
            val savedRecipe = it.arguments[0] as Recipe
            assertEquals(RecipeState.UPLOAD, savedRecipe.state)
        }

        // when
        val result = saveRecipeUseCase(SaveRecipeRequest(dummyRecipe.copy(
            state = RecipeState.UPLOAD
        )))

        // then
        assertTrue(result.isSuccess)
        verify(recipeRepository).updateMyRecipe(any(), any())
    }

    @Test
    fun saveRecipeUseCase_editDownloadedRecipe_stateDownload(): Unit = runBlocking {
        // given
        whenever(idGenerator.generateId()).thenReturn(DUMMY_STRING)
        whenever(imageRepository.copyExternalImageToInternal(any()))
            .thenReturn(Result.success(listOf(DUMMY_STRING, DUMMY_STRING)))
        whenever(recipeRepository.getLocalRecipe(any()))
            .thenReturn(Result.success(dummyRecipe))
        whenever(recipeRepository.updateMyRecipe(any(), any())).thenAnswer {
            val savedRecipe = it.arguments[0] as Recipe
            assertEquals(RecipeState.DOWNLOAD, savedRecipe.state)
        }

        // when
        val result = saveRecipeUseCase(SaveRecipeRequest(dummyRecipe.copy(
            state = RecipeState.DOWNLOAD
        )))

        // then
        assertTrue(result.isSuccess)
        verify(recipeRepository).updateMyRecipe(any(), any())
    }

    @Test
    fun saveRecipeUseCase_downloadNetworkRecipe_stateDownload(): Unit = runBlocking {
        // given
        whenever(idGenerator.generateId()).thenReturn(DUMMY_STRING)
        whenever(imageRepository.copyRemoteImageToInternal(any()))
            .thenReturn(Result.success(listOf(DUMMY_STRING, DUMMY_STRING)))
        whenever(recipeRepository.saveMyRecipe(any())).thenAnswer {
            val savedRecipe = it.arguments[0] as Recipe
            assertEquals(RecipeState.DOWNLOAD, savedRecipe.state)
        }

        // when
        val result = saveRecipeUseCase(SaveRecipeRequest(dummyRecipe.copy(
            state = RecipeState.NETWORK,
            imgPath = "https://${DUMMY_STRING}"
        )))

        // then
        assertTrue(result.isSuccess)
        verify(recipeRepository).saveMyRecipe(any())
    }

    companion object {
        private const val DUMMY_STRING = "dummy"
        private const val DUMMY_INT = 0
        private const val DUMMY_LONG = 0L
        private const val DUMMY_BOOLEAN = false
    }
}