with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'r') as f:
    content = f.read()

old_code = """                    10 -> SystemSettingsScreen(
                        settings = settings,
                        currencies = currencies,
                        parties = parties,
                        invoices = recentInvoices,
                        onUpdateValuationMethod = { viewModel.updateSystemCostingMethod(it) }
                    )
                }"""

new_code = """                    10 -> SystemSettingsScreen(
                        settings = settings,
                        currencies = currencies,
                        parties = parties,
                        invoices = recentInvoices,
                        onUpdateValuationMethod = { viewModel.updateSystemCostingMethod(it) }
                    )
                    11 -> {
                        if (currentUserRole == com.example.dokkani.data.local.entities.UserRole.ADMIN) {
                            val userManagementViewModel: com.example.dokkani.ui.screens.users.UserManagementViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                            com.example.dokkani.ui.screens.users.UserManagementScreen(viewModel = userManagementViewModel)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Text("عذراً، ليس لديك صلاحية للوصول إلى هذه الشاشة.")
                            }
                        }
                    }
                }"""

content = content.replace(old_code, new_code)
with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'w') as f:
    f.write(content)
