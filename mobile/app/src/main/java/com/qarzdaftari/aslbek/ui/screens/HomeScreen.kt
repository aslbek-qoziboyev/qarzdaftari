package com.qarzdaftari.aslbek.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qarzdaftari.aslbek.data.db.DebtDatabaseHelper
import com.qarzdaftari.aslbek.data.model.Debt
import com.qarzdaftari.aslbek.data.model.User
import com.qarzdaftari.aslbek.ui.components.AddDebtDialog
import com.qarzdaftari.aslbek.ui.components.DeleteConfirmDialog
import com.qarzdaftari.aslbek.ui.components.PaymentDialog
import com.qarzdaftari.aslbek.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User,
    dbHelper: DebtDatabaseHelper,
    onLogout: () -> Unit
) {
    var debts by remember { mutableStateOf<List<Debt>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // all, given, received, paid

    var showAddDialog by remember { mutableStateOf(false) }
    var paymentDebtTarget by remember { mutableStateOf<Debt?>(null) }
    var deleteDebtTarget by remember { mutableStateOf<Debt?>(null) }

    fun refreshDebts() {
        debts = dbHelper.getDebtsForUser(currentUser.id)
    }

    LaunchedEffect(currentUser.id) {
        refreshDebts()
    }

    // Filter calculations
    val filteredDebts = debts.filter { debt ->
        val matchesSearch = searchQuery.isBlank() ||
                debt.name.contains(searchQuery, ignoreCase = true) ||
                (debt.phone != null && debt.phone.contains(searchQuery))

        val matchesFilter = when (selectedFilter) {
            "given" -> debt.direction == "given" && !debt.isFullyPaid
            "received" -> debt.direction == "received" && !debt.isFullyPaid
            "paid" -> debt.isFullyPaid
            else -> true
        }

        matchesSearch && matchesFilter
    }

    // Calculations for summary stats
    val totalGivenRemaining = debts.filter { it.direction == "given" }.sumOf { it.remaining }
    val totalReceivedRemaining = debts.filter { it.direction == "received" }.sumOf { it.remaining }
    val netBalance = totalGivenRemaining - totalReceivedRemaining

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Qarz Daftari",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(
                            text = "Chiqish",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yangi qarz qo'shish")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                // Summary Section
                SummaryCards(
                    totalGivenRemaining = totalGivenRemaining,
                    totalReceivedRemaining = totalReceivedRemaining,
                    netBalance = netBalance,
                    totalCount = debts.size
                )
            }

            item {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Ism yoki telefon bo'yicha qidirish...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Qidiruv")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            item {
                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("Barchasi (${debts.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "given",
                        onClick = { selectedFilter = "given" },
                        label = { Text("Berganlarim") }
                    )
                    FilterChip(
                        selected = selectedFilter == "received",
                        onClick = { selectedFilter = "received" },
                        label = { Text("Olganlarim") }
                    )
                    FilterChip(
                        selected = selectedFilter == "paid",
                        onClick = { selectedFilter = "paid" },
                        label = { Text("Yopilganlar") }
                    )
                }
            }

            if (filteredDebts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (debts.isEmpty()) "Hozircha qarzlar mavjud emas" else "Qidiruv bo'yicha natija topilmadi",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Pastdagi '+' tugmasini bosib yangi qarz yozuvini qo'shing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredDebts, key = { it.id }) { debt ->
                    DebtCard(
                        debt = debt,
                        onPaymentClick = { paymentDebtTarget = debt },
                        onDeleteClick = { deleteDebtTarget = debt }
                    )
                }
            }

            item {
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    // Add Debt Dialog
    if (showAddDialog) {
        AddDebtDialog(
            userId = currentUser.id,
            onDismiss = { showAddDialog = false },
            onSave = { newDebt ->
                dbHelper.insertDebt(newDebt)
                showAddDialog = false
                refreshDebts()
            }
        )
    }

    // Payment Dialog
    paymentDebtTarget?.let { target ->
        PaymentDialog(
            debt = target,
            onDismiss = { paymentDebtTarget = null },
            onConfirmPayment = { amount ->
                dbHelper.recordPayment(target.id, amount)
                paymentDebtTarget = null
                refreshDebts()
            }
        )
    }

    // Delete Confirmation Dialog
    deleteDebtTarget?.let { target ->
        DeleteConfirmDialog(
            debt = target,
            onDismiss = { deleteDebtTarget = null },
            onConfirmDelete = {
                dbHelper.deleteDebt(target.id)
                deleteDebtTarget = null
                refreshDebts()
            }
        )
    }
}

@Composable
fun SummaryCards(
    totalGivenRemaining: Double,
    totalReceivedRemaining: Double,
    netBalance: Double,
    totalCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Menga berishlari kerak", fontSize = 12.sp, color = Color(0xFF065F46))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatMoney(totalGivenRemaining),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF047857)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Men berishim kerak", fontSize = 12.sp, color = Color(0xFF991B1B))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatMoney(totalReceivedRemaining),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFB91C1C)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (netBalance >= 0) Color(0xFFF0FDF4) else Color(0xFFFFF1F2)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sof balans (qoldiq)", fontSize = 12.sp, color = Color(0xFF475569))
                    Text(
                        formatMoney(netBalance),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                }
                Text(
                    "$totalCount ta yozuv",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    onPaymentClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isGiven = debt.direction == "given"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Avatar, Name, and Delete icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isGiven) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = debt.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = if (isGiven) Color(0xFF047857) else Color(0xFFBE123C),
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (!debt.phone.isNullOrBlank()) {
                        Text(
                            text = debt.phone,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delete system action
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "O'chirish",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Direction & Status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGiven) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
                ) {
                    Text(
                        text = if (isGiven) "Menga qarzdor" else "Men qarzdorman",
                        color = if (isGiven) Color(0xFF065F46) else Color(0xFF991B1B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (debt.isFullyPaid) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "To'liq yopilgan",
                            color = Color(0xFF15803D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!debt.note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Izoh: ${debt.note}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            // Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jami summa:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(debt.amount),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Qaytarilgan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(debt.returned),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF059669)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Qoldiq qarz:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatMoney(debt.remaining),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (debt.remaining > 0) Color(0xFFDC2626) else Color(0xFF059669)
                    )
                }
            }

            // Payment action button (if not fully paid)
            if (!debt.isFullyPaid) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPaymentClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("To'lovni qayd qilish")
                }
            }
        }
    }
}
