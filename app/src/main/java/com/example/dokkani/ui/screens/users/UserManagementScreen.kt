package com.example.dokkani.ui.screens.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dokkani.data.local.entities.UserEntity
import com.example.dokkani.data.local.entities.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(viewModel: UserManagementViewModel) {
    val users by viewModel.allUsers.collectAsState()
    val showDialog by viewModel.showAddEditDialog.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مستخدم")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                text = "إدارة المستخدمين والصلاحيات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { user ->
                    UserCard(user = user, onEdit = { viewModel.openEditDialog(user) }, onToggleStatus = { viewModel.toggleUserStatus(user) })
                }
            }
        }
    }

    if (showDialog) {
        UserAddEditDialog(viewModel)
    }
}

@Composable
fun UserCard(user: UserEntity, onEdit: () -> Unit, onToggleStatus: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(text = "اسم الدخول: ${user.username}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "الصلاحية: ${user.role.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector = if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = "تغيير الحالة",
                        tint = if (user.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAddEditDialog(viewModel: UserManagementViewModel) {
    val username by viewModel.username.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val pinCode by viewModel.pinCode.collectAsState()
    val role by viewModel.role.collectAsState()
    val isActive by viewModel.isActive.collectAsState()
    val editingUser by viewModel.editingUser.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { viewModel.closeDialog() },
        title = { Text(if (editingUser == null) "إضافة مستخدم جديد" else "تعديل المستخدم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { viewModel.updateFullName(it) },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.updateUsername(it) },
                    label = { Text("اسم الدخول (Username)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) viewModel.updatePinCode(it) 
                    },
                    label = { Text("رمز PIN (4 أرقام)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = role.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الصلاحية") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        UserRole.entries.forEach { userRole ->
                            DropdownMenuItem(
                                text = { Text(userRole.name) },
                                onClick = {
                                    viewModel.updateRole(userRole)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { viewModel.updateIsActive(it) })
                    Text("حساب مفعل")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveUser() },
                enabled = username.isNotBlank() && fullName.isNotBlank() && pinCode.length == 4
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeDialog() }) {
                Text("إلغاء")
            }
        }
    )
}
