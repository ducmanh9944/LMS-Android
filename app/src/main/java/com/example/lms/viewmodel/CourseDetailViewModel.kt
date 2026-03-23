package com.example.lms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lms.data.repository.CategoryRepository
import com.example.lms.data.repository.CourseRepository
import com.example.lms.data.repository.CurriculumRepository
import com.example.lms.data.repository.EnrollmentRepository
import com.example.lms.data.repository.AuthRepository
import com.example.lms.data.repository.ProgressRepository
import com.example.lms.util.CourseDetailEvent
import com.example.lms.util.CourseDetailUiState
import com.example.lms.util.ResultState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CourseDetailViewModel(
    private val courseRepository: CourseRepository = CourseRepository(),
    private val enrollmentRepository: EnrollmentRepository = EnrollmentRepository(),
    private val curriculumRepository: CurriculumRepository = CurriculumRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val progressRepository: ProgressRepository = ProgressRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CourseDetailEvent>()
    val event = _event.asSharedFlow()

    fun loadCourseDetail(courseId: String, userId: String) {
        if (_uiState.value.course?.id == courseId) {
            refreshProgressOnly(courseId, userId)
            return
        }

        viewModelScope.launch {
            _uiState.value = CourseDetailUiState(isLoading = true)

            val courseDeferred = async { courseRepository.getCourseById(courseId) }
            val enrolledDeferred = async { enrollmentRepository.isEnrolled(userId, courseId) }
            val curriculumDeferred = async { curriculumRepository.getCurriculum(courseId) }
            val categoriesDeferred = async { categoryRepository.getCategories() }
            val progressDeferred = async { progressRepository.getProgress(userId, courseId) }

            val courseResult = courseDeferred.await()
            val enrolledResult = enrolledDeferred.await()
            val curriculumResult = curriculumDeferred.await()
            val categoriesResult = categoriesDeferred.await()
            val progressResult = progressDeferred.await()

            if (courseResult is ResultState.Success) {
                val instructorId = courseResult.data.instructorId
                val instructorResult = authRepository.getUserDetails(instructorId)
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        course = courseResult.data,
                        isEnrolled = (enrolledResult as? ResultState.Success)?.data ?: false,
                        curriculum = (curriculumResult as? ResultState.Success)?.data ?: emptyList(),
                        categories = (categoriesResult as? ResultState.Success)?.data ?: emptyList(),
                        instructor = (instructorResult as? ResultState.Success)?.data,
                        progress = (progressResult as? ResultState.Success)?.data
                    )
                }
            } else if (courseResult is ResultState.Error) {
                _uiState.update { it.copy(isLoading = false) }
                _event.emit(CourseDetailEvent.ShowError(courseResult.message))
            }
        }
    }

    fun refreshProgressOnly(courseId: String, userId: String) {
        viewModelScope.launch {
            val result = progressRepository.getProgress(userId, courseId)
            if (result is ResultState.Success) {
                _uiState.update { it.copy(progress = result.data) }
            }
        }
    }

    fun enrollCourse(userId: String, courseId: String) {
        if (_uiState.value.isEnrolling) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true) }

            when (val enrollResult = enrollmentRepository.enrollCourse(userId, courseId)) {
                is ResultState.Success -> {
                    launch {
                        courseRepository.incrementEnrollment(courseId)
                    }
                    _uiState.update {
                        it.copy(isEnrolling = false, isEnrolled = true)
                    }
                    _event.emit(CourseDetailEvent.EnrollSuccess)
                }
                is ResultState.Error -> {
                    _uiState.update { it.copy(isEnrolling = false) }
                    _event.emit(CourseDetailEvent.ShowError(enrollResult.message))
                }
                else -> {}
            }
        }
    }
}