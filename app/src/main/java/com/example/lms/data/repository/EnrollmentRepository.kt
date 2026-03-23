package com.example.lms.data.repository

import com.example.lms.data.model.Enrollment
import com.example.lms.util.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EnrollmentRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val enrollmentsCollection = firestore.collection("enrollments")

    suspend fun enrollCourse(userId: String, courseId: String): ResultState<Unit> {
        return try {
            val id = "${userId}_${courseId}"
            val enrollment = Enrollment(
                id = id,
                userId = userId,
                courseId = courseId,
                enrolledAt = System.currentTimeMillis()
            )
            enrollmentsCollection.document(id).set(enrollment).await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Đăng ký khóa học thất bại")
        }
    }

    suspend fun isEnrolled(userId: String, courseId: String): ResultState<Boolean> {
        return try {
            val id = "${userId}_${courseId}"
            val snapshot = enrollmentsCollection.document(id).get().await()
            ResultState.Success(snapshot.exists())
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Kiểm tra đăng ký thất bại")
        }
    }

    suspend fun getEnrolledCourseIds(userId: String): ResultState<List<String>> {
        return try {
            val snapshot = enrollmentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val courseIds = snapshot.toObjects(Enrollment::class.java).map { it.courseId }
            ResultState.Success(courseIds)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lấy danh sách khóa học đã đăng ký thất bại")
        }
    }
}