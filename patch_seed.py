import re

with open('/app/applet/app/src/main/java/com/example/dokkani/data/local/DokkaniDatabase.kt', 'r') as f:
    content = f.read()

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
    r'(private suspend fun populateInitialGroceryData\(db: DokkaniDatabase\) \{)',
    r'\1\n' + seed_code,
    content
)

with open('/app/applet/app/src/main/java/com/example/dokkani/data/local/DokkaniDatabase.kt', 'w') as f:
    f.write(content)
