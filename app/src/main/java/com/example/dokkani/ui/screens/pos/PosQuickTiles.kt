package com.example.dokkani.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dokkani.domain.pos.QuickTileIconType
import com.example.dokkani.domain.pos.QuickTileItem

/**
 * شبكة الأصناف السريعة للأصناف التي لا تحتوي على باركود (Quick Tiles)
 * مثل الخضار المشكل والخبز بأسعار مفتوحة أو محددة
 */
@Composable
fun PosQuickTilesGrid(
    tiles: List<QuickTileItem>,
    onTileClick: (QuickTileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocalMall,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "الأصناف السريعة (بدون باركود)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "خضار / مخابز / سعر مفتوح",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(tiles, key = { it.id }) { tile ->
                QuickTileCard(
                    tile = tile,
                    onClick = { onTileClick(tile) }
                )
            }
        }
    }
}

@Composable
fun QuickTileCard(
    tile: QuickTileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, bgCol, iconCol) = getIconAndColors(tile.iconType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .testTag("tile_${tile.id}")
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgCol),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tile.titleArabic,
                        tint = iconCol,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (tile.isWeighted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = "موزون",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "وزن",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tile.titleArabic,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (tile.isOpenPrice) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = if (tile.isOpenPrice) "سعر حر" else "%.2f ر.س".format(tile.price),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tile.isOpenPrice) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun getIconAndColors(iconType: QuickTileIconType): Triple<ImageVector, Color, Color> {
    return when (iconType) {
        QuickTileIconType.BREAD -> Triple(
            Icons.Default.BakeryDining,
            Color(0xFFFFF3E0),
            Color(0xFFE65100)
        )
        QuickTileIconType.TAMEES -> Triple(
            Icons.Default.Restaurant,
            Color(0xFFFFECB3),
            Color(0xFFFF8F00)
        )
        QuickTileIconType.PRODUCE -> Triple(
            Icons.Default.Eco,
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
        QuickTileIconType.TOMATO -> Triple(
            Icons.Default.Eco,
            Color(0xFFFFEBEE),
            Color(0xFFC62828)
        )
        QuickTileIconType.CUCUMBER -> Triple(
            Icons.Default.Eco,
            Color(0xFFE8F8F5),
            Color(0xFF1B5E20)
        )
        QuickTileIconType.HERBS -> Triple(
            Icons.Default.LocalFlorist,
            Color(0xFFF1F8E9),
            Color(0xFF558B2F)
        )
        QuickTileIconType.WATER -> Triple(
            Icons.Default.LocalDrink,
            Color(0xFFE1F5FE),
            Color(0xFF0288D1)
        )
        QuickTileIconType.ICE -> Triple(
            Icons.Default.AcUnit,
            Color(0xFFE0F7FA),
            Color(0xFF0097A7)
        )
        QuickTileIconType.OPEN_PRICE -> Triple(
            Icons.Default.AddShoppingCart,
            Color(0xFFF3E5F5),
            Color(0xFF7B1FA2)
        )
        QuickTileIconType.CUSTOM -> Triple(
            Icons.Default.MoreHoriz,
            Color(0xFFECEFF1),
            Color(0xFF455A64)
        )
    }
}
