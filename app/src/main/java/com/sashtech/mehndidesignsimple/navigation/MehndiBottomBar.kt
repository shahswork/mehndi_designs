package com.sashtech.mehndidesignsimple.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Warm brown brand accent from reference design
private val WarmBrown = Color(0xFFA88369)

// Light Mode palette
private val LightNavBg = Color(0xFFFFFFFF)
private val LightNavBorder = Color(0xFFE5E5EA)
private val LightInactiveColor = Color(0xFF757575)
private val LightCircleBg = Color(0x24A88369) // Subtle warm circular tint (~14% alpha)

// Dark Mode palette
private val DarkNavBg = Color(0xFF1C1C1E)
private val DarkNavBorder = Color(0xFF2C2C2E)
private val DarkInactiveColor = Color(0xFFB0B0B0)
private val DarkCircleBg = Color(0x38A88369) // Subtle warm dark circular tint (~22% alpha)

@Composable
fun MehndiBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val containerBg = if (isDark) DarkNavBg else LightNavBg
    val containerBorder = if (isDark) DarkNavBorder else LightNavBorder
    val inactiveColor = if (isDark) DarkInactiveColor else LightInactiveColor
    val circleBg = if (isDark) DarkCircleBg else LightCircleBg
    val shadowSpot = if (isDark) Color(0x40000000) else Color(0x1F000000)
    val shadowAmbient = if (isDark) Color(0x26000000) else Color(0x0F000000)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = shadowSpot,
                    ambientColor = shadowAmbient
                ),
            shape = RoundedCornerShape(32.dp),
            color = containerBg,
            border = BorderStroke(1.dp, containerBorder),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BOTTOM_NAV_ITEMS.forEach { item ->
                    val isSelected = currentRoute == item.route

                    BottomNavItemView(
                        item = item,
                        isSelected = isSelected,
                        inactiveColor = inactiveColor,
                        circleBg = circleBg,
                        onClick = {
                            if (currentRoute != item.route) {
                                onNavigate(item.route)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    inactiveColor: Color,
    circleBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) WarmBrown else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "iconColor_${item.route}"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) WarmBrown else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "labelColor_${item.route}"
    )

    val circleColor by animateColorAsState(
        targetValue = if (isSelected) circleBg else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "circleColor_${item.route}"
    )

    Column(
        modifier = modifier
            .testTag(item.testTag)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 28.dp,
                    color = WarmBrown.copy(alpha = 0.15f)
                ),
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Soft circular background container for selected icon (transparent for unselected)
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.unselectedIcon,
                contentDescription = item.title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label directly underneath icon
        Text(
            text = item.title,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.1.sp
            ),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Small dot indicator below the active tab label (transparent placeholder on inactive to keep alignment)
        Box(
            modifier = Modifier
                .size(4.5.dp)
                .background(
                    color = if (isSelected) WarmBrown else Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}
