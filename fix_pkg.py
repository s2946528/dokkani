with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'r') as f:
    content = f.read()

# Fix the invalid package order
if content.startswith("import androidx.compose.material.icons.automirrored.filled.ExitToApp"):
    content = content.replace(
        "import androidx.compose.material.icons.automirrored.filled.ExitToApp\nimport androidx.compose.material.icons.filled.Person\npackage com.example.dokkani.ui.screens\n",
        "package com.example.dokkani.ui.screens\nimport androidx.compose.material.icons.automirrored.filled.ExitToApp\nimport androidx.compose.material.icons.filled.Person\n"
    )

with open('./app/src/main/java/com/example/dokkani/ui/screens/DokkaniApp.kt', 'w') as f:
    f.write(content)
