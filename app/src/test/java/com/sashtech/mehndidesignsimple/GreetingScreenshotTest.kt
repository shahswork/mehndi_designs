package com.sashtech.mehndidesignsimple

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.ui.components.CategoryCard
import com.sashtech.mehndidesignsimple.ui.theme.MehndiDesignTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun category_card_screenshot() {
        val cat = MehndiCategory(
            id = "arabic",
            name = "Arabic Mehndi",
            description = "Flowing freeform vines, paisleys and bold floral contours.",
            imageUrl = "https://images.unsplash.com/photo-1596704017254-9b121068fb31?auto=format&fit=crop&w=600&q=80",
            isStepByStep = false,
            defaultDesignCount = 28
        )
        composeTestRule.setContent {
            MehndiDesignTheme {
                CategoryCard(
                    category = cat,
                    designCount = 28,
                    onCategoryClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/category_card.png")
    }
}
