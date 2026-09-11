import re
with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'r') as f:
    content = f.read()

old_when = "when (uiState.selectedTab) {"
new_when = """
                val isCashier = currentUserRole == com.example.dokkani.data.local.entities.UserRole.CASHIER
                val isInventory = currentUserRole == com.example.dokkani.data.local.entities.UserRole.INVENTORY
                val isAdmin = currentUserRole == com.example.dokkani.data.local.entities.UserRole.ADMIN
                
                val hasAccess = when (uiState.selectedTab) {
                    0, 1, 2 -> isAdmin || isCashier
                    4, 5, 6, 7 -> isAdmin || isInventory
                    else -> isAdmin
                }

                if (!hasAccess) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.Text("عذراً، ليس لديك صلاحية للوصول إلى هذه الشاشة.")
                    }
                } else when (uiState.selectedTab) {
"""

content = content.replace(old_when, new_when)
with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'w') as f:
    f.write(content)
