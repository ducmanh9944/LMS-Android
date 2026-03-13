package com.example.lms.data.repository

import com.example.lms.data.model.Course
import com.example.lms.util.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CourseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val coursesCollection = firestore.collection("courses")
    private val lessonsCollection = firestore.collection("lessons")
    private val quizzesCollection = firestore.collection("quizzes")

    suspend fun createCourse(course: Course): ResultState<String> {
        return try {
            val docRef = coursesCollection.document()
            val newCourse = course.copy(
                id = docRef.id,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            docRef.set(newCourse).await()
            ResultState.Success(docRef.id)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Tạo khóa học thất bại")
        }
    }

    suspend fun updateCourse(course: Course): ResultState<Unit> {
        return try {
            coursesCollection
                .document(course.id)
                .update(
                    mapOf(
                        "title" to course.title,
                        "description" to course.description,
                        "thumbnailUrl" to course.thumbnailUrl,
                        "price" to course.price,
                        "categoryId" to course.categoryId,
                        "level" to course.level,
                        "duration" to course.duration,
                        "isPublished" to course.isPublished,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Cập nhật khóa học thất bại")
        }
    }

    suspend fun deleteCourse(courseId: String): ResultState<Unit> {
        return try {
            val batch = firestore.batch()

            batch.delete(coursesCollection.document(courseId))

            val lessonsSnapshot = lessonsCollection.whereEqualTo("courseId", courseId).get().await()
            lessonsSnapshot.documents.forEach { batch.delete(it.reference) }

            val quizzesSnapshot = quizzesCollection.whereEqualTo("courseId", courseId).get().await()
            quizzesSnapshot.documents.forEach { batch.delete(it.reference) }

            batch.commit().await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Xóa khóa học thất bại")
        }
    }

    suspend fun getCourseById(courseId: String): ResultState<Course> {
        return try {
            val snapshot = coursesCollection
                .document(courseId)
                .get()
                .await()
            val course = snapshot.toObject(Course::class.java)
            if (course != null) {
                ResultState.Success(course)
            } else {
                ResultState.Error("Không tìm thấy khóa học")
            }
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lấy thông tin khóa học thất bại")
        }
    }

    suspend fun getCoursesByInstructor(instructorId: String): ResultState<List<Course>> {
        return try {
            val snapshot = coursesCollection
                .whereEqualTo("instructorId", instructorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val courses = snapshot.toObjects(Course::class.java)
            ResultState.Success(courses)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lấy danh sách khóa học của giảng viên thất bại")
        }
    }

    suspend fun updatePublishStatus(courseId: String, isPublished: Boolean): ResultState<Unit> {
        return try {
            coursesCollection
                .document(courseId)
                .update(
                    mapOf(
                        "isPublished" to isPublished,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Cập nhật trạng thái khóa học thất bại")
        }
    }

    suspend fun incrementEnrollment(courseId: String): ResultState<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = coursesCollection.document(courseId)
                val snapshot = transaction.get(docRef)
                val current = snapshot.getLong("enrollmentCount") ?: 0
                transaction.update(
                    docRef,
                    "enrollmentCount",
                    current + 1
                )
            }.await()
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Cập nhật số lượng học viên thất bại")
        }
    }
}
