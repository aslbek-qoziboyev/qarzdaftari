package com.qarzdaftari.aslbek.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qarzdaftari.aslbek.data.model.Debt
import com.qarzdaftari.aslbek.util.formatMoney

@Composable
fun PaymentDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double) -> Unit
) {
    var payAmountText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val remaining = debt.remaining

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "To'lovni qayd etish",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${debt.name} bo'yicha qoldiq qarz:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatMoney(remaining),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = payAmountText,
                    onValueChange = {
                        payAmountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        errorMsg = null
                    },
                    label = { Text("To'lanayotgan summa (so'm)") },
                    placeholder = { Text("Masalan: 100000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = {
                        payAmountText = if (remaining % 1.0 == 0.0) remaining.toLong().toString() else remaining.toString()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("To'liq summani kiritish (${formatMoney(remaining)})")
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = payAmountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorMsg = "Iltimos, to'g'ri to'lov summasini kiriting"
                        return@Button
                    }
                    if (amount > remaining) {
                        errorMsg = "To'lov summasi qoldiqdan (${formatMoney(remaining)}) oshib ketmasligi kerak"
                        return@Button
                    }
                    onConfirmPayment(amount)
                }
            ) {
                Text("To'lovni saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
