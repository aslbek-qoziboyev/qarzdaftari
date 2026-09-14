package com.qarzdaftari.aslbek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qarzdaftari.aslbek.data.db.DebtDatabaseHelper
import com.qarzdaftari.aslbek.data.model.User
import com.qarzdaftari.aslbek.data.session.SessionManager

@Composable
fun AuthScreen(
    dbHelper: DebtDatabaseHelper,
    sessionManager: SessionManager,
    onAuthSuccess: (User) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Logo / Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Q",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Qarz Daftari",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isRegisterMode) "Yangi hisob yaratish" else "Hisobingizga kiring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Toggle tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            isRegisterMode = false
                            errorMessage = null
                            successMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (!isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kirish", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            isRegisterMode = true
                            errorMessage = null
                            successMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isRegisterMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ro'yxatdan o'tish", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Register-only name field
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Ismingiz") },
                        placeholder = { Text("Masalan: Aslbek") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email manzili") },
                    placeholder = { Text("namuna@mail.uz") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Parol") },
                    placeholder = { Text("••••••••") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(if (passwordVisible) "Yashirish" else "Ko'rsatish", fontSize = 12.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Main Submit Button
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        if (isRegisterMode) {
                            val result = dbHelper.registerUser(name, email, password)
                            result.onSuccess { registeredUser ->
                                sessionManager.saveUser(registeredUser)
                                onAuthSuccess(registeredUser)
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Ro'yxatdan o'tishda xatolik yuz berdi"
                            }
                        } else {
                            val result = dbHelper.loginUser(email, password)
                            result.onSuccess { loggedInUser ->
                                sessionManager.saveUser(loggedInUser)
                                onAuthSuccess(loggedInUser)
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Kirishda xatolik yuz berdi"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Hisob yaratish" else "Kirish",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // Quick Guest Access
                OutlinedButton(
                    onClick = {
                        val guestUser = sessionManager.loginAsGuest()
                        onAuthSuccess(guestUser)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mehmon sifatida tezkor kirish")
                }
            }
        }
    }
}
