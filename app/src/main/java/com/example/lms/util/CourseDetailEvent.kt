package com.example.lms.util

sealed class CourseDetailEvent {
    object EnrollSuccess : CourseDetailEvent()
    data class ShowError(val message: String) : CourseDetailEvent()
}