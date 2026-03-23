package com.example.lms.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lms.data.model.Course
import com.example.lms.data.repository.CategoryRepository
import com.example.lms.data.repository.CourseRepository
import com.example.lms.util.CloudinaryManager
import com.example.lms.util.CourseEvent
import com.example.lms.util.CourseUiState
import com.example.lms.util.ResultState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CourseViewModel(
    private val repository: CourseRepository = CourseRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CourseEvent>()
    val event = _event.asSharedFlow()

    init {
        getCategories()
    }

    fun getCategories() {
        viewModelScope.launch {
            when (val result = categoryRepository.getCategories()) {
                is ResultState.Success -> {
                    _uiState.update { it.copy(categories = result.data) }
                }
                is ResultState.Error -> {
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun getMyCourses(instructorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getCoursesByInstructor(instructorId)) {
                is ResultState.Success -> {
                    _uiState.update { it.copy(isLoading = false, courses = result.data) }
                }
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun getAllPublishedCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getAllPublishedCourses()) {
                is ResultState.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allPublishedCourses = result.data
                        )
                    }
                }
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun getSuggestedCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getAllPublishedCourses()) {
                is ResultState.Success -> {
                    // Tạm thời lấy 2 khóa học như yêu cầu
                    val courses = result.data.take(2)
                    _uiState.update { it.copy(isLoading = false, suggestedCourses = courses) }
                }
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun onTitleChange() = _uiState.update { it.copy(titleError = null) }
    fun onDescriptionChange() = _uiState.update { it.copy(descriptionError = null) }
    fun onPriceChange() = _uiState.update { it.copy(priceError = null) }
    fun onCategorySelected() = _uiState.update { it.copy(categoryError = null) }
    fun onDurationChange() = _uiState.update { it.copy(durationError = null) }
    fun onThumbnailSelected() = _uiState.update { it.copy(thumbnailUrlError = null) }

    private fun validate(course: Course, isFree: Boolean, priceStr: String): Boolean {
        var isValid = true
        
        _uiState.update { it.copy(
            titleError = null,
            descriptionError = null,
            priceError = null,
            categoryError = null,
            durationError = null,
            thumbnailUrlError = null
        )}

        if (course.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Tên khóa học không được để trống") }
            isValid = false
        }
        if (course.description.isBlank()) {
            _uiState.update { it.copy(descriptionError = "Mô tả không được để trống") }
            isValid = false
        }
        if (course.categoryId.isBlank()) {
            _uiState.update { it.copy(categoryError = "Vui lòng chọn danh mục") }
            isValid = false
        }
        if (course.thumbnailUrl.isBlank()) {
            _uiState.update { it.copy(thumbnailUrlError = "Vui lòng chọn ảnh đại diện") }
            isValid = false
        }
        
        // Price Validation
        if (!isFree) {
            val p = priceStr.toDoubleOrNull()
            if (priceStr.isBlank()) {
                _uiState.update { it.copy(priceError = "Vui lòng nhập giá khóa học") }
                isValid = false
            } else if (p == null || p <= 0) {
                _uiState.update { it.copy(priceError = "Giá phải lớn hơn 0") }
                isValid = false
            }
        }

        if (course.duration.isBlank()) {
            _uiState.update { it.copy(durationError = "Thời lượng không được để trống") }
            isValid = false
        }

        return isValid
    }

    fun createCourse(course: Course, isFree: Boolean, priceStr: String) {
        if (!validate(course, isFree, priceStr)) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            var finalThumbnailUrl = course.thumbnailUrl
            var finalPublicId = course.thumbnailPublicId

            if (course.thumbnailUrl.startsWith("content://")) {
                when (val uploadResult = CloudinaryManager.uploadImage(Uri.parse(course.thumbnailUrl))) {
                    is ResultState.Success -> {
                        finalThumbnailUrl = uploadResult.data.first
                        finalPublicId = uploadResult.data.second
                    }
                    is ResultState.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _event.emit(CourseEvent.ShowError(uploadResult.message))
                        return@launch
                    }
                    else -> {}
                }
            }

            val newCourse = course.copy(
                thumbnailUrl = finalThumbnailUrl,
                thumbnailPublicId = finalPublicId
            )

            when (val result = repository.createCourse(newCourse)) {
                is ResultState.Success -> {
                    _event.emit(CourseEvent.SaveSuccess)
                    getMyCourses(course.instructorId)
                }
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun updateCourse(course: Course, isFree: Boolean, priceStr: String) {
        if (!validate(course, isFree, priceStr)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            var finalThumbnailUrl = course.thumbnailUrl
            var finalPublicId = course.thumbnailPublicId

            if (course.thumbnailUrl.startsWith("content://")) {
                when (val uploadResult = CloudinaryManager.uploadImage(Uri.parse(course.thumbnailUrl))) {
                    is ResultState.Success -> {
                        finalThumbnailUrl = uploadResult.data.first
                        finalPublicId = uploadResult.data.second
                    }
                    is ResultState.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _event.emit(CourseEvent.ShowError(uploadResult.message))
                        return@launch
                    }
                    else -> {}
                }
            }

            val updatedCourse = course.copy(
                thumbnailUrl = finalThumbnailUrl,
                thumbnailPublicId = finalPublicId
            )

            when (val result = repository.updateCourse(updatedCourse)) {
                is ResultState.Success -> {
                    _event.emit(CourseEvent.SaveSuccess)
                    getMyCourses(course.instructorId)
                }
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun deleteCourse(courseId: String, instructorId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.deleteCourse(courseId)) {
                is ResultState.Success -> getMyCourses(instructorId)
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun togglePublishStatus(courseId: String, instructorId: String) {
        viewModelScope.launch {
            val course = _uiState.value.courses.find { it.id == courseId } ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updatePublishStatus(courseId, !course.isPublished)) {
                is ResultState.Success -> getMyCourses(instructorId)
                is ResultState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _event.emit(CourseEvent.ShowError(result.message))
                }
                else -> {}
            }
        }
    }

    fun onCourseSelected(course: Course?) {
        _uiState.update { 
            it.copy(
                currentCourse = course,
                titleError = null,
                descriptionError = null,
                priceError = null,
                categoryError = null,
                durationError = null,
                thumbnailUrlError = null,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
