import re

with open('/app/applet/app/src/main/java/com/example/dokkani/data/local/DokkaniDatabase.kt', 'r') as f:
    content = f.read()

# Add UserEntity to entities list
content = re.sub(
    r'entities = \[',
    'entities = [\n        UserEntity::class,',
    content
)

# Add abstract fun userDao(): UserDao
content = re.sub(
    r'(abstract fun licenseDao\(\): LicenseDao)',
    r'\1\n    abstract fun userDao(): UserDao',
    content
)

# Add default admin to seedDatabase
seed_code = """
            // 0. Seed Default Admin
            val userDao = db.userDao()
            userDao.insertUser(
                UserEntity(
                    username = "admin",
                    fullName = "مدير النظام",
                    pinCode = "1234",
                    role = com.example.dokkani.data.local.entities.UserRole.ADMIN,
                    isActive = true
                )
            )
"""
content = re.sub(
    r'(fun seedDatabase\(db: DokkaniDatabase\) \{\n        CoroutineScope\(Dispatchers\.IO\)\.launch \{)',
    r'\1\n' + seed_code,
    content
)

# Add imports
content = re.sub(
    r'import androidx.room.Database',
    'import com.example.dokkani.data.local.entities.UserEntity\nimport com.example.dokkani.data.local.dao.UserDao\nimport androidx.room.Database',
    content
)

with open('/app/applet/app/src/main/java/com/example/dokkani/data/local/DokkaniDatabase.kt', 'w') as f:
    f.write(content)
