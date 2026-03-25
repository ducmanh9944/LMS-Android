package com.example.lms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lms.data.repository.MyLearningRepository
import com.example.lms.util.MyLearningEvent
import com.example.lms.util.MyLearningTab
import com.example.lms.util.MyLearningUiState
import com.example.lms.util.ResultState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyLearningViewModel(
    private val myLearningRepository: MyLearningRepository = MyLearningRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MyLearningEvent>()
    val event = _event.asSharedFlow()

    fun loadMyLearning(userId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = myLearningRepository.getMyLearning(userId)) {
                is ResultState.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inProgressCourses = result.data.inProgress,
                            completedCourses = result.data.completed
                        )
                    }
                }

                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(MyLearningEvent.ShowError(result.message))
                }

                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun refreshMyLearning(userId: String) {
        loadMyLearning(userId)
    }

    fun selectTab(tab: MyLearningTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
}

