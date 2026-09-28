package com.sashtech.mehndidesignsimple.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.ui.components.CategoryCard
import com.sashtech.mehndidesignsimple.ui.components.MehndiTopBar
import com.sashtech.mehndidesignsimple.ui.components.ShimmerGrid
import com.sashtech.mehndidesignsimple.ui.components.StepTutorialCard
import com.sashtech.mehndidesignsimple.ui.theme.NaturalGoldSecondary
import com.sashtech.mehndidesignsimple.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onCategoryClick: (MehndiCategory) -> Unit,
    onTutorialClick: (StepByStepTutorial) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories = uiState.categories
    val stepTutorials = uiState.stepTutorials
    val stepByStepCategory = categories.firstOrNull { it.isStepByStep }
        ?: if (stepTutorials.isNotEmpty()) {
            MehndiCategory(
                id = "step_by_step",
                name = "Step-by-Step Mehndi",
                description = "Learn intricate mehndi designs step by step with clear guides.",
                isStepByStep = true,
                defaultDesignCount = stepTutorials.size
            )
        } else null
    val regularCategories = categories.filter { !it.isStepByStep }

    val hasAnyContent = categories.isNotEmpty() || stepTutorials.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MehndiTopBar(
            title = "Categories",
            subtitle = "Explore by Style & Occasion",
            showSearch = true,
            onSearchClick = onSearchClick
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .testTag("categories_pull_refresh")
        ) {
            if (uiState.isLoading && !hasAnyContent) {
                ShimmerGrid(itemCount = 6)
            } else if (!hasAnyContent) {
                // Validated empty state when Firebase Realtime Database has no categories or tutorials
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No categories available",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull down to refresh or check Firebase Realtime Database.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("categories_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Dynamic Step-by-Step Tutorials Section from Firebase
                    if (stepTutorials.isNotEmpty() || stepByStepCategory != null) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STEP-BY-STEP TUTORIALS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )

                                    if (stepByStepCategory != null) {
                                        Text(
                                            text = "View All (${if (stepTutorials.isNotEmpty()) stepTutorials.size else stepByStepCategory.defaultDesignCount})",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = NaturalGoldSecondary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { onCategoryClick(stepByStepCategory) }
                                                .padding(2.dp)
                                        )
                                    }
                                }

                                if (stepTutorials.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(
                                            items = stepTutorials,
                                            key = { "cat_tut_${it.id}" }
                                        ) { tutorial ->
                                            StepTutorialCard(
                                                tutorial = tutorial,
                                                onTutorialClick = onTutorialClick
                                            )
                                        }
                                    }
                                } else if (stepByStepCategory != null) {
                                    CategoryCard(
                                        category = stepByStepCategory,
                                        designCount = stepByStepCategory.defaultDesignCount,
                                        onCategoryClick = onCategoryClick
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: Regular Categories Header
                    if (regularCategories.isNotEmpty()) {
                        item {
                            Text(
                                text = "ALL CATEGORIES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        // 2-column paired layout for regular categories from Firebase
                        val paired = regularCategories.chunked(2)
                        items(
                            items = paired,
                            key = { pair -> pair.joinToString("-") { it.id } }
                        ) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                for (cat in pair) {
                                    CategoryCard(
                                        category = cat,
                                        designCount = cat.defaultDesignCount,
                                        onCategoryClick = onCategoryClick,
                                        modifier = Modifier.weight(1f)
                                    )
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
}
