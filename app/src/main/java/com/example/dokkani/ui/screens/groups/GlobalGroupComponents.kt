package com.example.dokkani.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.data.local.entities.GlobalGroupEntity

/**
 * القائمة المنسدلة لاختيار المجموعة الديناميكية المرتبطة بنوع كيان معين
 */
@Composable
fun GlobalGroupDropdown(
    groups: List<GlobalGroupEntity>,
    entityType: String,
    selectedGroupId: Long?,
    onGroupSelected: (GlobalGroupEntity?) -> Unit,
    label: String = "المجموعة / التصنيف الديناميكي",
    modifier: Modifier = Modifier
) {
    val filteredGroups = remember(groups, entityType) {
        groups.filter { it.entityType.equals(entityType, ignoreCase = true) }
    }

    val selectedGroup = remember(selectedGroupId, filteredGroups) {
        filteredGroups.find { it.id == selectedGroupId }
    }

    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedGroup?.name ?: "كل المجموعات (بدون تحديد)",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                val groupColor = selectedGroup?.let { parseHexColor(it.colorHex) } ?: MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(groupColor, CircleShape)
                )
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("كل المجموعات (بدون تحديد)") },
                onClick = {
                    onGroupSelected(null)
                    expanded = false
                }
            )

            filteredGroups.forEach { group ->
                val color = parseHexColor(group.colorHex)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(group.name, fontWeight = FontWeight.Medium)
                            if (group.code.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(${group.code})", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                    onClick = {
                        onGroupSelected(group)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * شريط شرائح الفلترة السريعة للمجموعات الديناميكية
 */
@Composable
fun GlobalGroupFilterChipRow(
    groups: List<GlobalGroupEntity>,
    entityType: String,
    selectedGroupId: Long?,
    onGroupSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredGroups = remember(groups, entityType) {
        groups.filter { it.entityType.equals(entityType, ignoreCase = true) }
    }

    if (filteredGroups.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            FilterChip(
                selected = selectedGroupId == null,
                onClick = { onGroupSelected(null) },
                label = { Text("الكل") }
            )
        }

        items(filteredGroups, key = { it.id }) { group ->
            val isSelected = group.id == selectedGroupId
            val color = parseHexColor(group.colorHex)
            FilterChip(
                selected = isSelected,
                onClick = { onGroupSelected(if (isSelected) null else group.id) },
                label = { Text(group.name) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                }
            )
        }
    }
}
