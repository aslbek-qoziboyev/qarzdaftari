package com.qarzdaftari.aslbek.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.qarzdaftari.aslbek.data.model.Debt
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    userId: String,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var returnedText by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("given") } // "given" (Menga qarzdor) or "received" (Men qarzdorman)
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isFullyPaid by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Yangi qarz yozuvi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Direction Selection
                Text(
                    text = "Qarz yo'nalishi:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = direction == "given",
                        onClick = { direction = "given" },
                        label = { Text("Menga qarzdor") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = direction == "received",
                        onClick = { direction = "received" },
                        label = { Text("Men qarzdorman") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Ism (shaxs yoki tashkilot)*") },
                    placeholder = { Text("Masalan: Alisher") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Amount field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        errorMessage = null
                    },
                    label = { Text("Summa (so'm)*") },
                    placeholder = { Text("Masalan: 500000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Fully paid checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isFullyPaid,
                        onCheckedChange = { isFullyPaid = it }
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Ushbu qarz to'liq yopilgan")
                }

                // Optional returned amount if not fully paid
                if (!isFullyPaid) {
                    OutlinedTextField(
                        value = returnedText,
                        onValueChange = {
                            returnedText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        },
                        label = { Text("Dastlabki qaytarilgan qism (ixtiyoriy)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Phone field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon raqami (ixtiyoriy)") },
                    placeholder = { Text("+998 90 123 45 67") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Note field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Izoh yoki muddat (ixtiyoriy)") },
                    placeholder = { Text("Masalan: 25-sanagacha qaytaradi") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val amountVal = amountText.toDoubleOrNull()

                    if (cleanName.isBlank()) {
                        errorMessage = "Iltimos, ismni kiriting"
                        return@Button
                    }
                    if (amountVal == null || amountVal <= 0.0) {
                        errorMessage = "Iltimos, to'g'ri summani kiriting"
                        return@Button
                    }

                    val returnedVal = if (isFullyPaid) {
                        amountVal
                    } else {
                        (returnedText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, amountVal)
                    }

                    val newDebt = Debt(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        name = cleanName,
                        amount = amountVal,
                        returned = returnedVal,
                        direction = direction,
                        phone = phone.ifBlank { null },
                        note = note.ifBlank { null }
                    )

                    onSave(newDebt)
                }
            ) {
                Text("Saqlash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Bekor qilish")
            }
        }
    )
}
