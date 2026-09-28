package com.sashtech.mehndidesignsimple.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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

// Color definitions tailored for the Mehndi premium dark navigation palette
private val NavContainerBg = Color(0xFF121714)
private val NavContainerBorder = Color(0xFF243026)
private val NavInactiveCircleBg = Color(0xFF1B231D)
private val NavInactiveIconTint = Color(0xFF8B9B90)
private val NavActivePillBg = Color(0xFF1F2922)
private val NavActivePillBorder = Color(0xFF2E3D31)
private val NavActiveBadgeBg = Color(0xFF4EE07B) // Vibrant Mehndi emerald green
private val NavActiveBadgeIconTint = Color(0xFF0A2212)
private val NavActiveLabelColor = Color(0xFFF3F7F4)

@Composable
fun MehndiBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(36.dp),
                    spotColor = Color(0x66000000),
                    ambientColor = Color(0x33000000)
                ),
            shape = RoundedCornerShape(36.dp),
            color = NavContainerBg,
            border = BorderStroke(1.dp, NavContainerBorder),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BOTTOM_NAV_ITEMS.forEach { item ->
                    val isSelected = currentRoute == item.route

                    AnimatedBottomNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                onNavigate(item.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val pillBackground by animateColorAsState(
        targetValue = if (isSelected) NavActivePillBg else NavInactiveCircleBg,
        animationSpec = tween(durationMillis = 260),
        label = "pillBackground_${item.route}"
    )

    val pillBorderColor by animateColorAsState(
        targetValue = if (isSelected) NavActivePillBorder else Color(0xFF222C24),
        animationSpec = tween(durationMillis = 260),
        label = "pillBorder_${item.route}"
    )

    val shape = if (isSelected) RoundedCornerShape(24.dp) else CircleShape

    Surface(
        modifier = modifier
            .testTag(item.testTag)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = NavActiveBadgeBg.copy(alpha = 0.2f), bounded = true),
                onClick = onClick
            ),
        shape = shape,
        color = pillBackground,
        border = BorderStroke(0.5.dp, pillBorderColor)
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(horizontal = if (isSelected) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                // Active circular badge with vibrant henna green background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NavActiveBadgeBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.selectedIcon,
                        contentDescription = item.title,
                        tint = NavActiveBadgeIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Active text label with smooth fade & horizontal expand
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(220, delayMillis = 40)) +
                            expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                    exit = fadeOut(animationSpec = tween(120)) +
                            shrinkHorizontally(animationSpec = tween(120))
                ) {
                    Text(
                        text = item.title,
                        color = NavActiveLabelColor,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp, end = 14.dp)
                    )
                }
            } else {
                // Inactive circular icon button
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.unselectedIcon,
                        contentDescription = item.title,
                        tint = NavInactiveIconTint,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}
