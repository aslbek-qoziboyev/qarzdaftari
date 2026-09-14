package com.qarzdaftari.aslbek.data.model

data class Debt(
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val returned: Double = 0.0,
    val direction: String, // "given" (Menga qarzdor) or "received" (Men qarzdorman)
    val phone: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remaining: Double
        get() = (amount - returned).coerceAtLeast(0.0)

    val isFullyPaid: Boolean
        get() = remaining <= 0.0
}
