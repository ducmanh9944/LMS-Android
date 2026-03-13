package com.example.lms.data.repository

import com.example.lms.data.model.Category
import com.example.lms.util.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val categoriesCollection = firestore.collection("categories")

    suspend fun getCategories(): ResultState<List<Category>> {
        return try {
            val snapshot = categoriesCollection.get().await()
            val categories = snapshot.toObjects(Category::class.java)
            ResultState.Success(categories)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lỗi khi lấy danh mục")
        }
    }
}
