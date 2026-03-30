package com.example.lms.data.repository

import com.example.lms.data.model.Instructor
import com.example.lms.util.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InstructorRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val instructorsCollection = firestore.collection("instructors")

    suspend fun getInstructorById(instructorId: String): ResultState<Instructor> {
        if (instructorId.isBlank()) return ResultState.Error("Thiếu thông tin giảng viên")

        return try {
            val snapshot = instructorsCollection.document(instructorId).get().await()
            if (!snapshot.exists()) {
                return ResultState.Error("Chưa có thông tin cá nhân giảng viên")
            }

            val instructor = snapshot.toObject(Instructor::class.java)
                ?: return ResultState.Error("Không đọc được thông tin giảng viên")

            ResultState.Success(
                if (instructor.uid.isBlank()) instructor.copy(uid = snapshot.id) else instructor
            )
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lấy thông tin giảng viên thất bại")
        }
    }
}

