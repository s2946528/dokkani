package com.example.dokkani.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.dokkani.data.local.entities.UserRole

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val requiredRoles: Set<UserRole>,
    val badgeText: String? = null
)

data class NavDepartmentGroup(
    val id: String,
    val titleArabic: String,
    val titleEnglish: String,
    val icon: ImageVector,
    val headerColor: Color,
    val items: List<NavTabItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleNavDrawerSheet(
    currentUserRole: UserRole,
    activeTabTitle: String,
    allDepartments: List<NavDepartmentGroup>,
    onSelectTabByTitle: (String) -> Unit,
    onOpenOnboardingWizard: () -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // خريطة لتتبع أطواء وتوسعات كل قسم (Expanded/Collapsed Map)
    val expandedDepartmentsMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allDepartments.forEach { dept ->
                // فتح القسم تلقائياً إذا كان يحوي الشاشة النشطة حالياً
                val containsActive = dept.items.any { it.title == activeTabTitle }
                this[dept.id] = containsActive || dept.id == "general_ledger" || dept.id == "sales"
            }
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // ==========================================
            // 1. ترويسة النظام الاحترافية (Header Branding)
            // ==========================================
            Surface(
                color = Color(0xFF133E32),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dokkani_unified_logo),
                            contentDescription = "شعار دكاني الموحد",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نظام دكاني POS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2E7D32)
                            ) {
                                Text(
                                    text = currentUserRole.labelArabic,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ERP System v2.1",
                                fontSize = 10.sp,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. حقل البحث السريع في القائمة الجانبية
            // ==========================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن قسم أو شاشة...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1B5E20),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // أزرار التحكم السريع بالطوي والتوسيع لجميع الإدارات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        allDepartments.forEach { expandedDepartmentsMap[it.id] = true }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.UnfoldMore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("توسيع الكل", fontSize = 10.sp)
                }

                TextButton(
                    onClick = {
                        allDepartments.forEach { expandedDepartmentsMap[it.id] = false }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.UnfoldLess, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("طي الكل", fontSize = 10.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ==========================================
            // 3. القائمة الجانبية المكونة من الإدارات القابلة للطي
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allDepartments.forEach { dept ->
                    // تصفية العناصر المسموح بها للصلاحيات وتطبيق البحث
                    val allowedItems = dept.items.filter { currentUserRole in it.requiredRoles }
                    val filteredItems = allowedItems.filter { item ->
                        searchQuery.isBlank() ||
                                item.title.contains(searchQuery, ignoreCase = true) ||
                                dept.titleArabic.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredItems.isNotEmpty()) {
                        val isDeptExpanded = expandedDepartmentsMap[dept.id] ?: true
                        val hasActiveChild = filteredItems.any { it.title == activeTabTitle }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasActiveChild) dept.headerColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (hasActiveChild) dept.headerColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // ترويسة الإدارة (Department Accordion Header)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            expandedDepartmentsMap[dept.id] = !isDeptExpanded
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = dept.headerColor,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = dept.icon,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = dept.titleArabic,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (hasActiveChild) dept.headerColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${filteredItems.size} عناصر مهارية",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (hasActiveChild) {
                                            Surface(
                                                shape = CircleShape,
                                                color = dept.headerColor,
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Icon(
                                            imageVector = if (isDeptExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // محتوى العناصر التابعة للإدارة (Animated Collapsible List)
                                AnimatedVisibility(
                                    visible = isDeptExpanded || searchQuery.isNotBlank(),
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 6.dp, bottom = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        filteredItems.forEach { item ->
                                            val isSelected = item.title == activeTabTitle

                                            NavigationDrawerItem(
                                                label = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = item.title,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp
                                                        )
                                                        if (item.badgeText != null) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFC62828)
                                                            ) {
                                                                Text(
                                                                    text = item.badgeText,
                                                                    fontSize = 9.sp,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.title,
                                                        tint = if (isSelected) dept.headerColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                selected = isSelected,
                                                onClick = {
                                                    onSelectTabByTitle(item.title)
                                                    onCloseDrawer()
                                                },
                                                colors = NavigationDrawerItemDefaults.colors(
                                                    selectedContainerColor = dept.headerColor.copy(alpha = 0.15f),
                                                    selectedIconColor = dept.headerColor,
                                                    selectedTextColor = dept.headerColor
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(42.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // ==========================================
            // 4. خيارات أسفل القائمة (معالج التهيئة وتسجيل الخروج)
            // ==========================================
            if (currentUserRole == UserRole.ADMIN) {
                NavigationDrawerItem(
                    label = { Text("معالج التهيئة الأولى والرقابة", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp)) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onOpenOnboardingWizard()
                    },
                    modifier = Modifier.height(40.dp)
                )
            }

            NavigationDrawerItem(
                label = { Text("تسجيل الخروج", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onLogout()
                },
                modifier = Modifier.height(40.dp)
            )
        }
    }
}
