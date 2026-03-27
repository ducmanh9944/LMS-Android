package com.example.lms.util

import com.example.lms.data.model.MyLearningItem

enum class MyLearningTab {
    IN_PROGRESS,
    COMPLETED
}

data class MyLearningUiState(
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val selectedTab: MyLearningTab = MyLearningTab.IN_PROGRESS,
    val inProgressCourses: List<MyLearningItem> = emptyList(),
    val completedCourses: List<MyLearningItem> = emptyList()
) {
    val visibleCourses: List<MyLearningItem>
        get() = when (selectedTab) {
            MyLearningTab.IN_PROGRESS -> inProgressCourses
            MyLearningTab.COMPLETED -> completedCourses
        }
}

