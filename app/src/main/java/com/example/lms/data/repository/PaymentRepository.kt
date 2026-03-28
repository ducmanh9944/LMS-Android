package com.example.lms.data.repository

import com.example.lms.data.model.Cart
import com.example.lms.data.model.CartItem
import com.example.lms.data.model.CartStatus
import com.example.lms.data.model.Course
import com.example.lms.data.model.Enrollment
import com.example.lms.data.model.Order
import com.example.lms.data.model.OrderItem
import com.example.lms.data.model.PaymentMethod
import com.example.lms.data.model.PaymentStatus
import com.example.lms.util.ResultState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PaymentRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val cartsCollection = firestore.collection("carts")
    private val cartItemsCollection = firestore.collection("cartItems")
    private val ordersCollection = firestore.collection("orders")
    private val orderItemsCollection = firestore.collection("orderItems")
    private val enrollmentsCollection = firestore.collection("enrollments")
    private val coursesCollection = firestore.collection("courses")

    private data class ResolvedCheckoutItem(
        val courseId: String,
        val courseTitle: String,
        val coursePrice: Double,
        val courseRefPath: String,
        val enrollmentCount: Long,
        val cartItemPriceToRemove: Double?
    )

    suspend fun checkoutCart(
        userId: String,
        paymentMethod: PaymentMethod,
        selectedCourseIds: List<String> = emptyList()
    ): ResultState<Order> {
        if (userId.isBlank()) return ResultState.Error("Thiếu thông tin người dùng")

        return try {
            val snapshot = cartItemsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val cartItems = snapshot.documents
                .mapNotNull { it.toObject(CartItem::class.java) }

            if (cartItems.isEmpty()) {
                return ResultState.Error("Giỏ hàng đang trống")
            }

            val selectedItems = if (selectedCourseIds.isEmpty()) {
                cartItems
            } else {
                val selectedSet = selectedCourseIds.toSet()
                cartItems.filter { it.courseId in selectedSet }
            }

            if (selectedCourseIds.isNotEmpty()) {
                val requestedSet = selectedCourseIds.toSet()
                val cartCourseIds = cartItems.map { it.courseId }.toSet()
                val missingCourseIds = requestedSet - cartCourseIds
                if (missingCourseIds.isNotEmpty()) {
                    return ResultState.Error("Một số khóa học đã không còn trong giỏ hàng, vui lòng tải lại")
                }
            }

            if (selectedItems.isEmpty()) {
                return ResultState.Error("Không tìm thấy khóa học được chọn trong giỏ hàng")
            }

            val selectedCourseIdSet = selectedItems.map { it.courseId }.toSet()

            if (selectedCourseIdSet.size != selectedItems.size) {
                return ResultState.Error("Danh sách khóa học thanh toán không hợp lệ")
            }

            executeCheckout(
                userId = userId,
                paymentMethod = paymentMethod,
                selectedCourseIds = selectedCourseIdSet.toList(),
                fromCart = true
            )
        } catch (e: IllegalStateException) {
            ResultState.Error(e.message ?: "Thanh toán thất bại")
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Thanh toán thất bại")
        }
    }

    suspend fun checkoutCoursesDirect(
        userId: String,
        paymentMethod: PaymentMethod,
        courseIds: List<String>
    ): ResultState<Order> {
        if (userId.isBlank()) return ResultState.Error("Thiếu thông tin người dùng")

        val normalizedCourseIds = courseIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (normalizedCourseIds.isEmpty()) {
            return ResultState.Error("Không có khóa học để thanh toán")
        }

        return executeCheckout(
            userId = userId,
            paymentMethod = paymentMethod,
            selectedCourseIds = normalizedCourseIds,
            fromCart = false
        )
    }

    private suspend fun executeCheckout(
        userId: String,
        paymentMethod: PaymentMethod,
        selectedCourseIds: List<String>,
        fromCart: Boolean
    ): ResultState<Order> {
        return try {
            val now = System.currentTimeMillis()
            val orderRef = ordersCollection.document()
            val orderId = orderRef.id
            val cartRef = cartsCollection.document(userId)

            val createdOrder = firestore.runTransaction { transaction ->
                // Read phase: all reads must happen before any write in Firestore transaction.
                val currentCart = if (fromCart) {
                    transaction.get(cartRef).toObject(Cart::class.java)
                } else {
                    transaction.get(cartRef).toObject(Cart::class.java)
                }

                val resolvedItems = mutableListOf<ResolvedCheckoutItem>()
                var totalAmount = 0.0

                selectedCourseIds.forEach { courseId ->
                    val courseRef = coursesCollection.document(courseId)
                    val courseSnapshot = transaction.get(courseRef)
                    val course = courseSnapshot.toObject(Course::class.java)
                        ?: throw IllegalStateException("Không tìm thấy khóa học, vui lòng tải lại")

                    val enrollmentRef = enrollmentsCollection.document(buildEnrollmentId(userId, courseId))
                    if (transaction.get(enrollmentRef).exists()) {
                        throw IllegalStateException("Có khóa học bạn đã đăng ký, vui lòng tải lại")
                    }

                    val itemData = if (fromCart) {
                        val cartItemRef = cartItemsCollection.document(buildCartItemId(userId, courseId))
                        val cartItem = transaction.get(cartItemRef)
                            .toObject(CartItem::class.java)
                            ?: throw IllegalStateException("Có khóa học không còn trong giỏ hàng")
                        ResolvedCheckoutItem(
                            courseId = courseId,
                            courseTitle = cartItem.courseTitle,
                            coursePrice = cartItem.coursePrice,
                            courseRefPath = courseRef.path,
                            enrollmentCount = course.enrollmentCount.toLong(),
                            cartItemPriceToRemove = cartItem.coursePrice
                        )
                    } else {
                        val cartItemRef = cartItemsCollection.document(buildCartItemId(userId, courseId))
                        val cartItem = transaction.get(cartItemRef).toObject(CartItem::class.java)

                        ResolvedCheckoutItem(
                            courseId = courseId,
                            courseTitle = course.title,
                            coursePrice = course.price,
                            courseRefPath = courseRef.path,
                            enrollmentCount = course.enrollmentCount.toLong(),
                            cartItemPriceToRemove = cartItem?.coursePrice
                        )
                    }

                    totalAmount += itemData.coursePrice
                    resolvedItems.add(itemData)
                }

                val order = Order(
                    id = orderId,
                    userId = userId,
                    itemCount = resolvedItems.size,
                    paymentMethod = paymentMethod,
                    paymentStatus = PaymentStatus.SUCCESS,
                    totalAmount = totalAmount,
                    createdAt = now
                )
                transaction.set(orderRef, order)

                resolvedItems.forEach { item ->
                    val orderItem = OrderItem(
                        id = buildOrderItemId(orderId, item.courseId),
                        orderId = orderId,
                        userId = userId,
                        courseId = item.courseId,
                        courseTitle = item.courseTitle,
                        coursePrice = item.coursePrice,
                        createdAt = now
                    )
                    transaction.set(orderItemsCollection.document(orderItem.id), orderItem)

                    val enrollment = Enrollment(
                        id = buildEnrollmentId(userId, item.courseId),
                        userId = userId,
                        courseId = item.courseId,
                        enrolledAt = now
                    )
                    transaction.set(enrollmentsCollection.document(enrollment.id), enrollment)

                    val courseRef = firestore.document(item.courseRefPath)
                    transaction.update(courseRef, "enrollmentCount", item.enrollmentCount + 1)

                    if (item.cartItemPriceToRemove != null) {
                        transaction.delete(cartItemsCollection.document(buildCartItemId(userId, item.courseId)))
                    }
                }

                val removedCartItemCount = resolvedItems.count { it.cartItemPriceToRemove != null }
                val removedCartAmount = resolvedItems.sumOf { it.cartItemPriceToRemove ?: 0.0 }

                if (removedCartItemCount > 0) {
                    val remainingItemCount = ((currentCart?.itemCount ?: removedCartItemCount) - removedCartItemCount)
                        .coerceAtLeast(0)
                    val remainingTotalAmount = ((currentCart?.totalAmount ?: removedCartAmount) - removedCartAmount)
                        .coerceAtLeast(0.0)

                    val updatedCart = Cart(
                        id = userId,
                        userId = userId,
                        status = if (remainingItemCount == 0) CartStatus.CHECKED_OUT else CartStatus.ACTIVE,
                        itemCount = remainingItemCount,
                        totalAmount = remainingTotalAmount,
                        createdAt = currentCart?.createdAt ?: now,
                        updatedAt = now
                    )
                    transaction.set(cartRef, updatedCart)
                }

                order
            }.await()

            ResultState.Success(createdOrder)
        } catch (e: IllegalStateException) {
            ResultState.Error(e.message ?: "Thanh toán thất bại")
        } catch (e: Exception) {
            ResultState.Error(e.message ?: "Thanh toán thất bại")
        }
    }

    private fun buildOrderItemId(orderId: String, courseId: String): String = "${orderId}_${courseId}"

    private fun buildEnrollmentId(userId: String, courseId: String): String = "${userId}_${courseId}"

    private fun buildCartItemId(userId: String, courseId: String): String = "${userId}_${courseId}"
}

