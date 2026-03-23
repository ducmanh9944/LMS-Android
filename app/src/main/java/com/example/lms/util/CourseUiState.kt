package com.example.lms.util

import com.example.lms.data.model.Category
import com.example.lms.data.model.Course

data class CourseUiState(
    val isLoading: Boolean = false,
    val courses: List<Course> = emptyList(),
    val allPublishedCourses: List<Course> = emptyList(),
    val suggestedCourses: List<Course> = emptyList(),
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val currentCourse: Course? = null,
    
    // Validation Errors
    val titleError: String? = null,
    val descriptionError: String? = null,
    val priceError: String? = null,
    val categoryError: String? = null,
    val durationError: String? = null,
    val thumbnailUrlError: String? = null
)
