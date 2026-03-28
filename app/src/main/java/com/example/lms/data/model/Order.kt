package com.example.lms.data.model

data class Order(
    val id: String = "",
    val userId: String = "",
    val itemCount: Int = 0,
    val paymentMethod: PaymentMethod = PaymentMethod.E_WALLET,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val totalAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PaymentMethod {
    E_WALLET,
    BANK_TRANSFER
}

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELED
}
