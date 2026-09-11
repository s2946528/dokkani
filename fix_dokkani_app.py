import re

with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'r') as f:
    content = f.read()

# Add imports if not present
if "com.example.dokkani.data.local.entities.UserRole" not in content:
    content = re.sub(
        r'package com.example.dokkani.ui.screens\n',
        'package com.example.dokkani.ui.screens\n\n'
        'import androidx.lifecycle.viewmodel.compose.viewModel\n'
        'import androidx.compose.material.icons.filled.Person\n'
        'import androidx.compose.material.icons.automirrored.filled.ExitToApp\n'
        'import androidx.compose.material3.IconButton\n'
        'import androidx.compose.material3.TopAppBar\n'
        'import androidx.compose.material3.TopAppBarDefaults\n'
        'import com.example.dokkani.data.local.entities.UserRole\n'
        'import com.example.dokkani.ui.screens.users.UserManagementScreen\n'
        'import com.example.dokkani.ui.screens.users.UserManagementViewModel\n',
        content
    )

# Fix signature
content = re.sub(
    r'fun DokkaniApp\(viewModel: DokkaniViewModel\) \{',
    r'fun DokkaniApp(viewModel: DokkaniViewModel, currentUserRole: UserRole = UserRole.ADMIN, onLogout: () -> Unit = {}) {',
    content
)

# Fix TopBar Actions
content = re.sub(
    r'actions = \{(\s*)// شارة حالة الترخيص والحماية',
    r'actions = {\n'
    r'                        IconButton(onClick = onLogout) {\n'
    r'                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج", tint = androidx.compose.ui.graphics.Color.White)\n'
    r'                        }\n\1// شارة حالة الترخيص والحماية',
    content
)

with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'w') as f:
    f.write(content)
