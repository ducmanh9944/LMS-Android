package com.example.lms.util

import com.example.lms.data.model.Cart
import com.example.lms.data.model.CartItem

data class CartUiState(
    val isLoading: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val cart: Cart? = null,
    val items: List<CartItem> = emptyList(),
    val selectedCourseIds: Set<String> = emptySet()
) {
    val selectedItems: List<CartItem>
        get() = items.filter { it.courseId in selectedCourseIds }

    val selectedItemCount: Int
        get() = selectedItems.size

    val selectedTotalAmount: Double
        get() = selectedItems.sumOf { it.coursePrice }
}

