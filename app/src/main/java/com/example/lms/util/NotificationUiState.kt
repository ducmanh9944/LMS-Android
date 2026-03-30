package com.example.lms.util

import com.example.lms.data.model.NotificationItem

data class NotificationUiState(
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val notifications: List<NotificationItem> = emptyList()
) {
    val unreadCount: Int
        get() = notifications.count { !it.isRead }
}

