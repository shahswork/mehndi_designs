package com.sashtech.mehndidesignsimple.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.ui.components.DesignCard
import com.sashtech.mehndidesignsimple.ui.components.MehndiAsyncImage
import com.sashtech.mehndidesignsimple.ui.components.MehndiTopBar
import com.sashtech.mehndidesignsimple.ui.components.ShimmerGrid
import com.sashtech.mehndidesignsimple.ui.components.StepTutorialCard
import com.sashtech.mehndidesignsimple.ui.theme.NaturalCatCream
import com.sashtech.mehndidesignsimple.ui.theme.NaturalCatGold
import com.sashtech.mehndidesignsimple.ui.theme.NaturalCatGoldBorder
import com.sashtech.mehndidesignsimple.ui.theme.NaturalCatMint
import com.sashtech.mehndidesignsimple.ui.theme.NaturalCatSage
import com.sashtech.mehndidesignsimple.ui.theme.NaturalCatSageBorder
import com.sashtech.mehndidesignsimple.ui.theme.NaturalGoldSecondary
import com.sashtech.mehndidesignsimple.ui.theme.NaturalGreenPrimary
import com.sashtech.mehndidesignsimple.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDesignClick: (MehndiDesign) -> Unit,
    onTutorialClick: (StepByStepTutorial) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllCategoriesClick: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.monitorNetwork(context)
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshData() },
        modifier = modifier
            .fillMaxSize()
            .testTag("home_pull_to_refresh")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Natural Tones Top Bar
            MehndiTopBar(
                title = "Mehndi Design",
                subtitle = "Browse & Learn Henna Patterns",
                showSearch = true,
                onSearchClick = onSearchClick
            )

            // Offline Banner Indicator
            if (uiState.isOffline) {
                Surface(
                    color = NaturalCatGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NaturalCatGoldBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("home_offline_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline • Showing saved designs",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar styled per Natural Tones Design
            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("home_search_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search Mehndi Designs...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            if (uiState.isLoading && uiState.latestDesigns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ShimmerGrid(itemCount = 6)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Section 1: Featured Banner (Natural Tones Gradient & Gold Accents)
                    item {
                        val featuredTutorial = uiState.stepTutorials.firstOrNull()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF2E4D2E),
                                            Color(0xFF1B3B2B)
                                        )
                                    )
                                )
                                .clickable {
                                    if (featuredTutorial != null) {
                                        onTutorialClick(featuredTutorial)
                                    } else {
                                        onViewAllCategoriesClick()
                                    }
                                }
                        ) {
                            // Decorative Gold Blur Circle
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.TopEnd)
                                    .background(NaturalGoldSecondary.copy(alpha = 0.12f), CircleShape)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp)
                            ) {
                                Text(
                                    text = "FEATURED TUTORIAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = NaturalGoldSecondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = featuredTutorial?.title ?: "Royal Bridal\nMasterclass",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        lineHeight = 24.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = NaturalGoldSecondary,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${featuredTutorial?.stepCount ?: 5} STEPS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            color = Color(0xFF1B3B2B),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "${featuredTutorial?.difficulty ?: "Advanced"} Difficulty",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Bottom Gold Accent line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(NaturalGoldSecondary)
                            )
                        }
                    }

                    // Section 2: Dynamic Categories from Firebase Realtime Database
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            NaturalSectionHeader(
                                title = "CATEGORIES",
                                actionText = "View All (${uiState.categories.size})",
                                onActionClick = onViewAllCategoriesClick
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                items(
                                    items = uiState.categories,
                                    key = { "home_cat_${it.id}" }
                                ) { category ->
                                    DynamicCategoryTile(
                                        category = category,
                                        onClick = { onCategoryClick(category.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Step-by-Step Tutorials Carousel
                    if (uiState.stepTutorials.isNotEmpty()) {
                        item {
                            NaturalSectionHeader(
                                title = "STEP-BY-STEP TUTORIALS",
                                actionText = "Explore",
                                onActionClick = onViewAllCategoriesClick
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                items(
                                    items = uiState.stepTutorials,
                                    key = { "tut_${it.id}" }
                                ) { tutorial ->
                                    StepTutorialCard(
                                        tutorial = tutorial,
                                        onTutorialClick = onTutorialClick
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Popular Designs Horizontal Showcase
                    if (uiState.popularDesigns.isNotEmpty()) {
                        item {
                            NaturalSectionHeader(
                                title = "POPULAR DESIGNS",
                                actionText = "Trending",
                                onActionClick = onViewAllCategoriesClick
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                items(
                                    items = uiState.popularDesigns,
                                    key = { "pop_${it.id}" }
                                ) { popDesign ->
                                    DesignCard(
                                        design = popDesign,
                                        isFavorite = uiState.favoriteIds.contains(popDesign.id),
                                        onDesignClick = onDesignClick,
                                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                                        modifier = Modifier.width(165.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section 5: Latest Designs Section
                    item {
                        NaturalSectionHeader(
                            title = "LATEST DESIGNS",
                            actionText = "New (${uiState.latestDesigns.size})",
                            onActionClick = onViewAllCategoriesClick
                        )
                    }

                    // Grid layout inside LazyColumn via chunked rows
                    val pairedDesigns = uiState.latestDesigns.chunked(2)
                    items(
                        items = pairedDesigns,
                        key = { pair -> pair.joinToString("-") { it.id } }
                    ) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (design in pair) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DesignCard(
                                        design = design,
                                        isFavorite = uiState.favoriteIds.contains(design.id),
                                        onDesignClick = onDesignClick,
                                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicCategoryTile(
    category: MehndiCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clickable { onClick() }
            .testTag("home_category_${category.id}")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 1.dp,
            modifier = Modifier.size(72.dp)
        ) {
            MehndiAsyncImage(
                imageUrl = category.imageUrl,
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NaturalSectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = NaturalGoldSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onActionClick() }
                    .padding(2.dp)
            )
        }
    }
}
