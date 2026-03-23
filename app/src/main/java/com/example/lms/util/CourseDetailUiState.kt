package com.example.lms.util

import com.example.lms.data.model.Category
import com.example.lms.data.model.Course
import com.example.lms.data.model.CurriculumItem
import com.example.lms.data.model.Progress
import com.example.lms.data.model.User

data class CourseDetailUiState(
    val isLoading: Boolean = false,
    val course: Course? = null,
    val curriculum: List<CurriculumItem> = emptyList(),
    val isEnrolled: Boolean = false,
    val isEnrolling: Boolean = false,
    val categories: List<Category> = emptyList(),
    val instructor: User? = null,
    val progress: Progress? = null
)