package com.kdjj.presentation.viewmodel.recipeeditor

import androidx.lifecycle.*
import androidx.work.*
import com.kdjj.domain.model.Recipe
import com.kdjj.domain.model.RecipeState
import com.kdjj.domain.model.RecipeStepType
import com.kdjj.domain.model.RecipeType
import com.kdjj.domain.model.request.*
import com.kdjj.domain.model.response.ValidateRecipeFlowResponse
import com.kdjj.domain.model.response.ValidateRecipeResponse
import com.kdjj.domain.model.response.ValidateRecipeStepFlowResponse
import com.kdjj.domain.usecase.ResultUseCase
import com.kdjj.domain.usecase.UseCase
import com.kdjj.presentation.common.*
import com.kdjj.presentation.common.extensions.throttleFirst
import com.kdjj.presentation.mapper.toDomain
import com.kdjj.presentation.mapper.toEditorDto
import com.kdjj.presentation.model.RecipeEditorDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

@HiltViewModel
internal class RecipeEditorViewModel @Inject constructor(
    private val validateRecipeUseCase: UseCase<ValidateRecipeRequest, ValidateRecipeResponse>,
    private val validateRecipeFlowUseCase: UseCase<ValidateRecipeFlowRequest, ValidateRecipeFlowResponse>,
    private val validateRecipeStepFlowUseCase: UseCase<ValidateRecipeStepFlowRequest, ValidateRecipeStepFlowResponse>,
    private val saveRecipeUseCase: ResultUseCase<SaveRecipeRequest, Unit>,
    private val fetchRecipeTypesUseCase: ResultUseCase<EmptyRequest, List<RecipeType>>,
    private val getMyRecipeUseCase: ResultUseCase<GetMyRecipeRequest, Recipe>,
    private val fetchRecipeTempUseCase: ResultUseCase<FetchRecipeTempRequest, Recipe?>,
    private val saveRecipeTempUseCase: ResultUseCase<SaveRecipeTempRequest, Unit>,
    private val deleteRecipeTempUseCase: ResultUseCase<DeleteRecipeTempRequest, Unit>,
    private val workManager: WorkManager,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private lateinit var recipeMetaDto: RecipeEditorDto.RecipeMetaDto
    private val _liveStepDtoList = MutableLiveData<List<RecipeEditorDto.RecipeStepDto>>()

    val liveRecipeDtoList: LiveData<List<RecipeEditorDto>> = _liveStepDtoList.map {
        listOf(recipeMetaDto) + it + RecipeEditorDto.PlusButton
    }

    val stepTypes = RecipeStepType.values()
    private val _liveRecipeTypes = MutableLiveData<List<RecipeType>>()
    val liveRecipeTypes: LiveData<List<RecipeType>> get() = _liveRecipeTypes

    private val _liveImgTarget = MutableLiveData<RecipeEditorDto?>()
    val liveImgTarget: LiveData<RecipeEditorDto?> get() = _liveImgTarget

    private val _liveRegisterHasPressed = MutableLiveData(false)
    val liveRegisterHasPressed: LiveData<Boolean> get() = _liveRegisterHasPressed

    private val _liveLoading = MutableLiveData(false)
    val liveLoading: LiveData<Boolean> get() = _liveLoading

    private val _liveEditing = MutableLiveData(false)
    val liveEditing: LiveData<Boolean> get() = _liveEditing

    private val _eventRecipeEditor = MutableLiveData<Event<RecipeEditorEvent>>()
    val eventRecipeEditor: LiveData<Event<RecipeEditorEvent>> get() = _eventRecipeEditor

    sealed class RecipeEditorEvent {
        class MoveToPosition(val idx: Int) : RecipeEditorEvent()
        class SaveResult(val isSuccess: Boolean) : RecipeEditorEvent()
        class TempDialog(val recipeId: String) : RecipeEditorEvent()
        object Error : RecipeEditorEvent()
        object ExitDialog : RecipeEditorEvent()
        object Exit : RecipeEditorEvent()
    }

    private lateinit var tempRecipe: Recipe
    private var tempJob: Job? = null

    enum class ButtonClick {
        SAVE,
        ADD_STEP
    }

    val clickFlow = MutableSharedFlow<ButtonClick>(extraBufferCapacity = 1)

    private val tempFlow = MutableSharedFlow<Unit>()
    private var oldRecipe: Recipe? = null

    private val _liveTempLoading = MutableLiveData(false)
    val liveTempLoading: LiveData<Boolean> get() = _liveTempLoading

    init {
        viewModelScope.launch {
            clickFlow.throttleFirst()
                .collect {
                    when (it) {
                        ButtonClick.SAVE -> {
                            saveRecipe()
                        }
                        ButtonClick.ADD_STEP -> {
                            addRecipeStep()
                        }
                    }
                }
        }

        viewModelScope.launch {
            tempFlow.debounce(3000)
                .collect {
                    saveTempRecipe()
                }
        }
    }

    fun doEdit() {
        viewModelScope.launch {
            tempFlow.emit(Unit)
        }
    }

    private fun isSameWithOld(): Boolean {
        oldRecipe?.let { oldRecipe ->
            if (
                recipeMetaDto.titleFlow.value != oldRecipe.title ||
                recipeMetaDto.stuffFlow.value != oldRecipe.stuff ||
                recipeMetaDto.imgPathFlow.value != oldRecipe.imgPath ||
                recipeMetaDto.typeIntFlow.value != liveRecipeTypes.value?.indexOf(oldRecipe.type)
            ) {
                return false
            }

            if (_liveStepDtoList.value?.size != oldRecipe.stepList.size) {
                return false
            }

            _liveStepDtoList.value?.let { stepList ->
                stepList.forEachIndexed { idx, model ->
                    if (
                        model.nameFlow.value != oldRecipe.stepList[idx].name ||
                        model.descriptionFlow.value != oldRecipe.stepList[idx].description ||
                        model.imgPathFlow.value != oldRecipe.stepList[idx].imgPath ||
                        model.minutesFlow.value != oldRecipe.stepList[idx].seconds / 60 ||
                        model.secondsFlow.value != oldRecipe.stepList[idx].seconds % 60 ||
                        model.typeIntFlow.value != oldRecipe.stepList[idx].type.ordinal
                    ) {
                        return false
                    }
                }
            }
            return true
        } ?: run {
            if (
                recipeMetaDto.titleFlow.value.isNotEmpty() ||
                recipeMetaDto.stuffFlow.value.isNotEmpty() ||
                recipeMetaDto.imgPathFlow.value.isNotEmpty() ||
                recipeMetaDto.typeIntFlow.value != 0
            ) {
                return false
            }

            _liveStepDtoList.value?.get(0)?.let { model ->
                if (
                    model.nameFlow.value.isNotEmpty() ||
                    model.descriptionFlow.value.isNotEmpty() ||
                    model.imgPathFlow.value.isNotEmpty() ||
                    model.minutesFlow.value != 0 ||
                    model.secondsFlow.value != 0 ||
                    model.typeIntFlow.value != 0
                ) {
                    return false
                }
            }

            return true
        }
    }

    fun initializeWith(loadingRecipeId: String?) {
        if (_liveStepDtoList.value != null) return

        _liveLoading.value = true
        val recipeId = loadingRecipeId?.also {
            if (loadingRecipeId.isNotEmpty()) {
                _liveEditing.value = true
            }
        } ?: ""

        viewModelScope.launch {
            fetchRecipeTypesUseCase(EmptyRequest)
                .onSuccess { recipeTypes ->
                    _liveRecipeTypes.value = recipeTypes
                    fetchRecipeTempUseCase(FetchRecipeTempRequest(recipeId)).getOrNull()?.let {
                        _liveLoading.value = false
                        tempRecipe = it
                        _eventRecipeEditor.value = Event(RecipeEditorEvent.TempDialog(recipeId))
                    } ?: if (recipeId.isEmpty()) {
                        createNewRecipe()
                    } else {
                        loadFromLocal(recipeId)
                    }
                }
                .onFailure {
                    _liveLoading.value = false
                    _eventRecipeEditor.value = Event(RecipeEditorEvent.Error)
                }
        }
    }

    fun showRecipeFromTemp() {
        if (_liveStepDtoList.value != null) return

        _liveLoading.value = true
        val (metaModel, stepList) =
            tempRecipe.toEditorDto(
                recipeTypeList = _liveRecipeTypes.value ?: listOf(),
                validateRecipeUseCase = validateRecipeFlowUseCase,
                validateStepUseCase = validateRecipeStepFlowUseCase,
                scope = viewModelScope
            )
        recipeMetaDto = metaModel
        _liveStepDtoList.value = stepList

        if (tempRecipe.recipeId.isNotEmpty()) {
            viewModelScope.launch {
                oldRecipe = getMyRecipeUseCase(GetMyRecipeRequest(tempRecipe.recipeId)).getOrNull()
                _liveLoading.value = false
            }
        } else {
            _liveLoading.value = false
        }
    }

    fun showRecipeFromLocal(recipeId: String) {
        if (_liveStepDtoList.value != null) return
        if (recipeId.isEmpty()) {
            createNewRecipe()
        } else {
            viewModelScope.launch {
                loadFromLocal(recipeId)
            }
        }
    }

    private fun createNewRecipe() {
        recipeMetaDto =
            RecipeEditorDto.RecipeMetaDto.create(validateRecipeFlowUseCase, viewModelScope, idGenerator)
        _liveStepDtoList.value = listOf(
            RecipeEditorDto.RecipeStepDto.create(validateRecipeStepFlowUseCase, viewModelScope)
        )
        viewModelScope.launch {
            deleteRecipeTempUseCase(DeleteRecipeTempRequest(""))
        }
        _liveLoading.value = false
    }

    private suspend fun loadFromLocal(recipeId: String) {
        getMyRecipeUseCase(GetMyRecipeRequest(recipeId))
            .onSuccess { recipe ->
                oldRecipe = recipe
                val (metaDto, stepDtoList) =
                    recipe.toEditorDto(
                        recipeTypeList = _liveRecipeTypes.value ?: listOf(),
                        validateRecipeUseCase = validateRecipeFlowUseCase,
                        validateStepUseCase = validateRecipeStepFlowUseCase,
                        scope = viewModelScope
                    )
                recipeMetaDto = metaDto
                _liveStepDtoList.value = stepDtoList
                deleteRecipeTempUseCase(DeleteRecipeTempRequest(recipe.recipeId))
            }
            .onFailure {
                _eventRecipeEditor.value = Event(RecipeEditorEvent.Error)
            }
        _liveLoading.value = false

    }

    private fun saveTempRecipe() {
        if (
            _liveStepDtoList.value == null || isSameWithOld() ||
            (_eventRecipeEditor.value?.peekContent() as? RecipeEditorEvent.SaveResult)?.isSuccess == true
        ) {
            return
        }

        _liveTempLoading.value = true
        tempJob?.cancel()
        tempJob = viewModelScope.launch {
            val recipe = recipeMetaDto.toDomain(
                stepDtoList = _liveStepDtoList.value ?: listOf(),
                recipeTypeList = liveRecipeTypes.value ?: listOf()
            )
            saveRecipeTempUseCase(SaveRecipeTempRequest(recipe))
            _liveTempLoading.value = false
        }
    }

    fun showExitDialog() {
        if (isSameWithOld()) {
            deleteTemp(true)
        } else {
            _eventRecipeEditor.value = Event(RecipeEditorEvent.ExitDialog)
        }
    }

    fun deleteTemp(finish: Boolean) {
        tempJob?.cancel()
        _liveTempLoading.value = false
        _liveLoading.value = true
        viewModelScope.launch {
            deleteRecipeTempUseCase(DeleteRecipeTempRequest(recipeMetaDto.recipeId))
            _liveLoading.value = false
            if (finish) {
                _eventRecipeEditor.value = Event(RecipeEditorEvent.Exit)
            }
        }
    }

    fun startSelectImage(dto: RecipeEditorDto) {
        _liveImgTarget.value = dto
    }

    fun setImage(uri: String) {
        liveImgTarget.value?.let { model ->
            when (model) {
                is RecipeEditorDto.RecipeMetaDto ->
                    model.imgPathFlow.value = uri
                is RecipeEditorDto.RecipeStepDto ->
                    model.imgPathFlow.value = uri
            }
            doEdit()
        }
        _liveImgTarget.value = null
    }

    fun cancelSelectImage() {
        _liveImgTarget.value = null
    }

    fun setImageEmpty() {
        liveImgTarget.value?.let { model ->
            when (model) {
                is RecipeEditorDto.RecipeMetaDto -> model.imgPathFlow.value = ""
                is RecipeEditorDto.RecipeStepDto -> model.imgPathFlow.value = ""
            }
        }
        _liveImgTarget.value = null
    }

    private fun addRecipeStep() {
        _liveStepDtoList.value = (_liveStepDtoList.value ?: listOf()) +
                RecipeEditorDto.RecipeStepDto.create(validateRecipeStepFlowUseCase, viewModelScope)
        _eventRecipeEditor.value = Event(
            RecipeEditorEvent.MoveToPosition((_liveStepDtoList.value?.size ?: 0) + 2)
        )
        doEdit()
    }

    fun removeRecipeStep(position: Int) {
        _liveStepDtoList.value?.let { modelList ->
            _liveStepDtoList.value = modelList.subList(0, position - 1) +
                    modelList.subList(position, modelList.size)
            doEdit()
        }
    }

    fun changeRecipeStepPosition(from: Int, to: Int) {
        _liveStepDtoList.value?.let { modelList ->
            _liveStepDtoList.value = modelList.toMutableList().apply {
                set(from - 1, set(to - 1, get(from - 1)))
            }
            doEdit()
        }
    }

    private fun saveRecipe() {
        _liveRegisterHasPressed.value = true
        val validation = validateRecipeUseCase(
            ValidateRecipeRequest(
                recipeMetaDto.toDomain(
                    stepDtoList = _liveStepDtoList.value ?: listOf(),
                    recipeTypeList = _liveRecipeTypes.value ?: listOf()
                )
            )
        )

        when (validation) {
            is ValidateRecipeResponse.Valid -> {
                if (isSameWithOld()) {
                    _eventRecipeEditor.value = Event(RecipeEditorEvent.SaveResult(true))
                    return
                }
                _liveLoading.value = true
                viewModelScope.launch {
                    val recipe = recipeMetaDto.toDomain(
                        _liveStepDtoList.value ?: listOf(),
                        liveRecipeTypes.value ?: listOf()
                    )

                    saveRecipeUseCase(SaveRecipeRequest(recipe))
                        .onSuccess {
                            if (recipe.state == RecipeState.UPLOAD) registerUploadTask(recipe.recipeId)
                            _eventRecipeEditor.value =
                                Event(RecipeEditorEvent.SaveResult(true))
                            deleteTemp(false)
                        }
                        .onFailure {
                            _eventRecipeEditor.value =
                                Event(RecipeEditorEvent.SaveResult(false))
                        }
                    _liveLoading.value = false
                }
            }
            is ValidateRecipeResponse.Invalid -> {
                _eventRecipeEditor.value =
                    Event(RecipeEditorEvent.MoveToPosition(validation.firstInvalidPosition))
            }
        }
    }

    private fun registerUploadTask(updatedRecipeId: String) {
        workManager.cancelAllWorkByTag(updatedRecipeId)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val updateWorkerRequest = OneTimeWorkRequestBuilder<RecipeUploadWorker>().apply {
            setInputData(workDataOf(UPDATED_RECIPE_ID to updatedRecipeId))
        }
            .setConstraints(constraints)
            .addTag(updatedRecipeId)
            .build()
        workManager.enqueue(updateWorkerRequest)
    }
}
