package com.example.lms.util

sealed class PaymentEvent {
    data class ShowError(val message: String) : PaymentEvent()
    data class CheckoutSuccess(val orderId: String) : PaymentEvent()
}

