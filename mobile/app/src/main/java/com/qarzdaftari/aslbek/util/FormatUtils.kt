package com.qarzdaftari.aslbek.util

fun formatMoney(value: Double): String {
    return String.format("%,.0f", value).replace(",", " ") + " so'm"
}
