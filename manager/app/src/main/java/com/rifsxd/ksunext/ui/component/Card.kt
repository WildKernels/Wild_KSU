package com.rifsxd.ksunext.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rifsxd.ksunext.ui.theme.getCardElevation

/**
 * Card Design System Constants
 * These values ensure consistent spacing and sizing across all card components
 */
object CardConstants {
    // Spacing constants
    val CARD_SPACING = 16.dp
    val CARD_PADDING_LARGE = 24.dp
    val CARD_PADDING_MEDIUM = 16.dp
    val ITEM_SPACING_LARGE = 16.dp
    val ITEM_SPACING_MEDIUM = 10.dp
    val ITEM_SPACING_SMALL = 4.dp
    val ITEM_SPACING_EXTRA_SMALL = 2.dp
    val ITEM_SPACING_TINY = 2.dp
    val ICON_TO_TEXT_SPACING = 20.dp
    val ICON_TO_TEXT_SPACING_SMALL = 16.dp
    val ACTION_SPACING = 8.dp
    
    // Icon sizes
    val ICON_SIZE = 24.dp
    
    // Card dimensions (flexible, not fixed)
    val CARD_MIN_HEIGHT = 80.dp
    val CARD_MAX_WIDTH = Dp.Unspecified
    
    // Animation constants
    val ANIMATION_DURATION = 300
}

/**
 * Card container types with predefined color schemes
 */
enum class CardType {
    PRIMARY,        // primaryContainer - for status/main cards
    SECONDARY,      // secondaryContainer - for action cards (superuser, module)
    SURFACE,        // surfaceContainer - for info/content cards
    ERROR,          // errorContainer - for warning/error cards
    CUSTOM          // custom color provided
}

/**
 * Standardized card container that serves as the base for all card components
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    cardType: CardType = CardType.SURFACE,
    customColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    animateContentSize: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = when (cardType) {
        CardType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        CardType.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        CardType.SURFACE -> MaterialTheme.colorScheme.surfaceContainer
        CardType.ERROR -> MaterialTheme.colorScheme.errorContainer
        CardType.CUSTOM -> customColor ?: MaterialTheme.colorScheme.surfaceContainer
    }
    
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = getCardElevation(),
        modifier = modifier
            .clip(CardDefaults.elevatedShape)
            .then(
                if (animateContentSize) {
                    Modifier.animateContentSize(
                        animationSpec = tween(durationMillis = CardConstants.ANIMATION_DURATION)
                    )
                } else Modifier
            )
            .then(
                when {
                    onClick != null && onLongClick != null -> Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardConstants.CARD_PADDING_LARGE),
            content = content
        )
    }
}

/**
 * Compact card container with smaller padding for action cards
 */
@Composable
fun CompactCard(
    modifier: Modifier = Modifier,
    cardType: CardType = CardType.SECONDARY,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val containerColor = when (cardType) {
        CardType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
        CardType.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
        CardType.SURFACE -> MaterialTheme.colorScheme.surfaceContainer
        CardType.ERROR -> MaterialTheme.colorScheme.errorContainer
        CardType.CUSTOM -> MaterialTheme.colorScheme.surfaceContainer
    }
    
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = getCardElevation(),
        onClick = onClick ?: {},
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CardConstants.CARD_PADDING_MEDIUM),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Card content layout for items with icon, label, and content
 */
@Composable
fun CardItem(
    label: String,
    content: String,
    icon: Any? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            when (icon) {
                is ImageVector -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = CardConstants.ICON_TO_TEXT_SPACING)
                )
                is Painter -> Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = CardConstants.ICON_TO_TEXT_SPACING)
                )
            }
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = CardConstants.ITEM_SPACING_SMALL)
            )
        }
    }
}

/**
 * Centered card content for count/status displays
 */
@Composable
fun CenteredCardContent(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_MEDIUM)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Row layout for card content with icon and text
 */
@Composable
fun CardRowContent(
    text: String,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = CardConstants.ICON_TO_TEXT_SPACING_SMALL)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        actions()
    }
}

/**
 * Card content with title and body text
 */
@Composable
fun CardTextContent(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_SMALL)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Spacer component for consistent spacing between card items
 */
@Composable
fun CardItemSpacer(size: Dp = CardConstants.ITEM_SPACING_LARGE) {
    Spacer(modifier = Modifier.height(size))
}

/**
 * Column arrangement for multiple card items with consistent spacing
 */
@Composable
fun CardItemsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CardConstants.ITEM_SPACING_LARGE),
        content = content
    )
}

/**
 * Row arrangement for side-by-side cards with consistent spacing
 */
@Composable
fun CardRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CardConstants.CARD_SPACING),
        content = content
    )
}

/**
 * Column arrangement for stacked cards with consistent spacing
 */
@Composable
fun CardColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CardConstants.CARD_SPACING),
        content = content
    )
}