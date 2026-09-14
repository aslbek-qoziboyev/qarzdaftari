package com.qarzdaftari.aslbek.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.qarzdaftari.aslbek.data.model.Debt
import com.qarzdaftari.aslbek.util.formatMoney

@Composable
fun DeleteConfirmDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Qarzni o'chirish",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Haqiqatan ham '${debt.name}' nomidagi ${formatMoney(debt.amount)} miqdoridagi qarz yozuvini o'chirib tashlamoqchimisiz? Bu amalni qaytarib bo'lmaydi."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Ha, o'chirish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
