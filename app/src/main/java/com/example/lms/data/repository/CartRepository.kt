package com.example.lms.data.repository

import com.example.lms.data.model.Cart
import com.example.lms.data.model.CartItem
import com.example.lms.data.model.CartStatus
import com.example.lms.data.model.Course
import com.example.lms.util.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CartRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val cartsCollection = firestore.collection("carts")
    private val cartItemsCollection = firestore.collection("cartItems")
    private val coursesCollection = firestore.collection("courses")
    private val enrollmentsCollection = firestore.collection("enrollments")

    suspend fun getOrCreateActiveCart(userId: String): ResultState<Cart> {
        if (userId.isBlank()) return ResultState.Error("Thiếu thông tin người dùng")

        return try {
            val cartRef = cartsCollection.document(buildCartId(userId))
            val snapshot = cartRef.get().await()
            val now = System.currentTimeMillis()

            val cart = if (snapshot.exists()) {
                snapshot.toObject(Cart::class.java)?.copy(updatedAt = now)
                    ?: Cart(
                        id = buildCartId(userId),
                        userId = userId,
                        status = CartStatus.ACTIVE,
                        itemCount = 0,
                        totalAmount = 0.0,
                        createdAt = now,
                        updatedAt = now
                    )
            } else {
                Cart(
                    id = buildCartId(userId),
                    userId = userId,
                    status = CartStatus.ACTIVE,
                    itemCount = 0,
                    totalAmount = 0.0,
                    createdAt = now,
                    updatedAt = now
                )
            }

            cartRef.set(cart).await()
            ResultState.Success(cart)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lấy giỏ hàng thất bại")
        }
    }

    suspend fun getCartItems(userId: String): ResultState<List<CartItem>> {
        if (userId.isBlank()) return ResultState.Error("Thiếu thông tin người dùng")

        return try {
            val snapshot = cartItemsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val items = snapshot.toObjects(CartItem::class.java)
                .sortedByDescending { it.addedAt }

            ResultState.Success(items)
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Lấy danh sách giỏ hàng thất bại")
        }
    }

    suspend fun isCourseInCart(userId: String, courseId: String): ResultState<Boolean> {
        if (userId.isBlank() || courseId.isBlank()) {
            return ResultState.Error("Thiếu thông tin kiểm tra giỏ hàng")
        }

        return try {
            val snapshot = cartItemsCollection
                .document(buildCartItemId(userId, courseId))
                .get()
                .await()
            ResultState.Success(snapshot.exists())
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Kiểm tra giỏ hàng thất bại")
        }
    }

    suspend fun isCourseEnrolled(userId: String, courseId: String): ResultState<Boolean> {
        if (userId.isBlank() || courseId.isBlank()) {
            return ResultState.Error("Thiếu thông tin kiểm tra đăng ký")
        }

        return try {
            val snapshot = enrollmentsCollection
                .document(buildEnrollmentId(userId, courseId))
                .get()
                .await()
            ResultState.Success(snapshot.exists())
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Kiểm tra đăng ký khóa học thất bại")
        }
    }

    suspend fun addCourseToCart(userId: String, courseId: String): ResultState<CartItem> {
        if (userId.isBlank() || courseId.isBlank()) {
            return ResultState.Error("Thiếu thông tin khóa học hoặc người dùng")
        }

        return try {
            val now = System.currentTimeMillis()
            val cartId = buildCartId(userId)
            val cartRef = cartsCollection.document(cartId)
            val cartItemRef = cartItemsCollection.document(buildCartItemId(userId, courseId))
            val enrollmentRef = enrollmentsCollection.document(buildEnrollmentId(userId, courseId))
            val courseRef = coursesCollection.document(courseId)

            val newCartItem = firestore.runTransaction { transaction ->
                val enrollmentSnapshot = transaction.get(enrollmentRef)
                if (enrollmentSnapshot.exists()) {
                    throw IllegalStateException("Bạn đã đăng ký khóa học này")
                }

                val existingCartItemSnapshot = transaction.get(cartItemRef)
                if (existingCartItemSnapshot.exists()) {
                    throw IllegalStateException("Khóa học đã có trong giỏ hàng")
                }

                val courseSnapshot = transaction.get(courseRef)
                val course = courseSnapshot.toObject(Course::class.java)
                    ?: throw IllegalStateException("Không tìm thấy khóa học")

                val cartSnapshot = transaction.get(cartRef)
                val currentCart = cartSnapshot.toObject(Cart::class.java)
                val createdAt = currentCart?.createdAt ?: now
                val newItemCount = (currentCart?.itemCount ?: 0) + 1
                val newTotalAmount = (currentCart?.totalAmount ?: 0.0) + course.price

                val cart = Cart(
                    id = cartId,
                    userId = userId,
                    status = CartStatus.ACTIVE,
                    itemCount = newItemCount,
                    totalAmount = newTotalAmount,
                    createdAt = createdAt,
                    updatedAt = now
                )

                val cartItem = CartItem(
                    id = cartItemRef.id,
                    cartId = cartId,
                    userId = userId,
                    courseId = course.id,
                    courseThumbnail = course.thumbnailUrl,
                    courseTitle = course.title,
                    coursePrice = course.price,
                    addedAt = now
                )

                transaction.set(cartRef, cart)
                transaction.set(cartItemRef, cartItem)
                cartItem
            }.await()

            ResultState.Success(newCartItem)
        } catch (e: IllegalStateException) {
            ResultState.Error(e.message ?: "Không thể thêm vào giỏ hàng")
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Thêm vào giỏ hàng thất bại")
        }
    }

    suspend fun removeCourseFromCart(userId: String, courseId: String): ResultState<Unit> {
        if (userId.isBlank() || courseId.isBlank()) {
            return ResultState.Error("Thiếu thông tin khóa học hoặc người dùng")
        }

        return try {
            val now = System.currentTimeMillis()
            val cartId = buildCartId(userId)
            val cartRef = cartsCollection.document(cartId)
            val cartItemRef = cartItemsCollection.document(buildCartItemId(userId, courseId))

            firestore.runTransaction { transaction ->
                val cartItemSnapshot = transaction.get(cartItemRef)
                val cartItem = cartItemSnapshot.toObject(CartItem::class.java)
                    ?: return@runTransaction

                val cartSnapshot = transaction.get(cartRef)
                val currentCart = cartSnapshot.toObject(Cart::class.java)

                val currentItemCount = currentCart?.itemCount ?: 0
                val currentTotalAmount = currentCart?.totalAmount ?: 0.0

                val newItemCount = (currentItemCount - 1).coerceAtLeast(0)
                val newTotalAmount = (currentTotalAmount - cartItem.coursePrice).coerceAtLeast(0.0)
                val createdAt = currentCart?.createdAt ?: now

                val updatedCart = Cart(
                    id = cartId,
                    userId = userId,
                    status = CartStatus.ACTIVE,
                    itemCount = newItemCount,
                    totalAmount = newTotalAmount,
                    createdAt = createdAt,
                    updatedAt = now
                )

                transaction.delete(cartItemRef)
                transaction.set(cartRef, updatedCart)
            }.await()

            ResultState.Success(Unit)
        } catch (e: IllegalStateException) {
            ResultState.Error(e.message ?: "Không thể xóa khóa học khỏi giỏ hàng")
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Xóa khóa học khỏi giỏ hàng thất bại")
        }
    }

    private fun buildCartId(userId: String): String = userId

    private fun buildCartItemId(userId: String, courseId: String): String = "${userId}_${courseId}"

    private fun buildEnrollmentId(userId: String, courseId: String): String = "${userId}_${courseId}"
}

