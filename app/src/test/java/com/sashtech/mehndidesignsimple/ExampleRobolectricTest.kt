package com.sashtech.mehndidesignsimple

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sashtech.mehndidesignsimple.data.local.AppDatabase
import com.sashtech.mehndidesignsimple.data.local.FavoriteDesignEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Mehndi Design", appName)
    }

    @Test
    fun `test favorites dao operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val dao = database.favoriteDao()

        val sample = FavoriteDesignEntity(
            id = "test_1",
            title = "Test Lotus",
            categoryId = "front_hand",
            imageUrl = "https://example.com/lotus.jpg",
            thumbnailUrl = "https://example.com/lotus_thumb.jpg",
            difficulty = "Easy",
            estimatedTime = "10 mins",
            isTutorial = false
        )

        dao.insertFavorite(sample)
        val isFav = dao.isFavorite("test_1").first()
        assertTrue(isFav)

        val allFavs = dao.getAllFavorites().first()
        assertEquals(1, allFavs.size)
        assertEquals("Test Lotus", allFavs[0].title)

        dao.deleteFavorite("test_1")
        val isFavAfter = dao.isFavorite("test_1").first()
        assertEquals(false, isFavAfter)
    }

    @Test
    fun `test design cache dao operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val dao = database.designDao()

        val design = com.sashtech.mehndidesignsimple.data.local.CachedDesignEntity(
            id = "cache_test_1",
            title = "Royal Bridal Henna",
            categoryId = "bridal",
            categoryName = "Bridal Mehndi",
            featured = true,
            popular = true,
            imageUrl = "https://example.com/bridal.jpg",
            thumbnailUrl = "https://example.com/bridal_thumb.jpg",
            tagsCsv = "bridal,wedding,royal",
            searchKeywordsCsv = "bridal henna,wedding"
        )

        dao.insertDesign(design)
        val count = dao.getDesignCount()
        assertTrue(count >= 1)

        val fetched = dao.getDesignById("cache_test_1").first()
        assertEquals("Royal Bridal Henna", fetched?.title)
        assertEquals(true, fetched?.featured)

        val searchResults = dao.searchDesigns("royal").first()
        assertTrue(searchResults.any { it.id == "cache_test_1" })
    }

    @Test
    fun `test mehndi design url resolution and blank handling`() {
        val designWithThumb = com.sashtech.mehndidesignsimple.data.model.MehndiDesign(
            id = "d1",
            title = "Floral Motif",
            thumbnailUrl = "https://example.com/thumb.jpg",
            imageUrl = "https://example.com/full.jpg"
        )
        assertEquals("https://example.com/thumb.jpg", designWithThumb.getDisplayThumbnailUrl())
        assertEquals("https://example.com/full.jpg", designWithThumb.getDisplayFullUrl())

        val designWithNullUrls = com.sashtech.mehndidesignsimple.data.model.MehndiDesign(
            id = "d2",
            title = "Null Image Pattern",
            thumbnailUrl = null,
            imageUrl = null,
            mediumImageUrl = ""
        )
        assertEquals("", designWithNullUrls.getDisplayThumbnailUrl())
        assertEquals("", designWithNullUrls.getDisplayMediumUrl())
        assertEquals("", designWithNullUrls.getDisplayFullUrl())
    }

    @Test
    fun `test dynamic category dao operations and cleanup`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val dao = database.categoryDao()

        val cat1 = com.sashtech.mehndidesignsimple.data.local.CachedCategoryEntity(
            id = "front_hand",
            name = "Front Hand Mehndi",
            description = "Intricate palm designs",
            imageUrl = "https://example.com/front.jpg",
            isStepByStep = false,
            defaultDesignCount = 12,
            sortOrder = 0
        )
        val cat2 = com.sashtech.mehndidesignsimple.data.local.CachedCategoryEntity(
            id = "back_hand",
            name = "Back Hand Mehndi",
            description = "Royal wrist and floral motifs",
            imageUrl = "https://example.com/back.jpg",
            isStepByStep = false,
            defaultDesignCount = 15,
            sortOrder = 1
        )

        dao.insertCategories(listOf(cat1, cat2))
        val list = dao.getAllCategories().first()
        assertEquals(2, list.size)
        assertEquals("Front Hand Mehndi", list[0].name)

        // Remove stale categories not present in active list
        dao.removeStaleCategories(listOf("front_hand"))
        val listAfterRemoval = dao.getAllCategories().first()
        assertEquals(1, listAfterRemoval.size)
        assertEquals("front_hand", listAfterRemoval[0].id)
    }

    @Test
    fun `test tutorial dao and step operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val dao = database.tutorialDao()

        val tut = com.sashtech.mehndidesignsimple.data.local.CachedTutorialEntity(
            id = "firebase_tut_1",
            title = "Arabic Flower Step Guide",
            description = "Step-by-step leaf and flower tutorial",
            coverImageUrl = "https://example.com/tut1.jpg",
            stepCount = 4,
            difficulty = "Easy",
            estimatedTime = "15 mins",
            categoryId = "step_by_step"
        )
        val step1 = com.sashtech.mehndidesignsimple.data.local.CachedTutorialStepEntity(
            id = "firebase_tut_1_s1",
            tutorialId = "firebase_tut_1",
            stepNumber = 1,
            title = "Draw Center Circle",
            description = "Start with a neat circle in the center",
            imageUrl = "https://example.com/step1.jpg",
            thumbnailUrl = "https://example.com/step1_thumb.jpg"
        )
        val step2 = com.sashtech.mehndidesignsimple.data.local.CachedTutorialStepEntity(
            id = "firebase_tut_1_s2",
            tutorialId = "firebase_tut_1",
            stepNumber = 2,
            title = "Add Petals",
            description = "Draw curved petals around the circle",
            imageUrl = "https://example.com/step2.jpg",
            thumbnailUrl = "https://example.com/step2_thumb.jpg"
        )

        dao.insertTutorial(tut)
        dao.insertTutorialSteps(listOf(step1, step2))

        val tutorials = dao.getTutorials(20).first()
        assertEquals(1, tutorials.size)
        assertEquals("Arabic Flower Step Guide", tutorials[0].title)

        val steps = dao.getStepsForTutorial("firebase_tut_1").first()
        assertEquals(2, steps.size)
        assertEquals(1, steps[0].stepNumber)
        assertEquals("Draw Center Circle", steps[0].title)
        assertEquals("Add Petals", steps[1].title)
    }

    @Test
    fun `test difficulty normalization for firebase intermediate values`() {
        // Firebase "intermediate" maps to "Medium"
        assertEquals("Medium", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("intermediate"))
        assertEquals("Medium", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("Intermediate"))
        assertEquals("Medium", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("medium"))
        assertEquals("Medium", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("Medium"))

        // Easy and Advanced mapping
        assertEquals("Easy", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("easy"))
        assertEquals("Easy", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("beginner"))
        assertEquals("Advanced", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("advanced"))
        assertEquals("Advanced", com.sashtech.mehndidesignsimple.data.model.MehndiDesign.normalizeDifficulty("hard"))

        // Matching checks
        assertTrue(com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("intermediate", "Medium"))
        assertTrue(com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("Medium", "Medium"))
        assertTrue(com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("intermediate", "All"))
        assertTrue(com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("Easy", "Easy"))
        assertTrue(com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("advanced", "Advanced"))
        assertEquals(false, com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("intermediate", "Easy"))
        assertEquals(false, com.sashtech.mehndidesignsimple.data.model.MehndiDesign.matchesDifficulty("intermediate", "Advanced"))
    }

    @Test
    fun `test app links helper dynamic url and constants`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val playStoreUrl = com.sashtech.mehndidesignsimple.utils.AppLinksHelper.getPlayStoreUrl(context)
        val expectedUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
        assertEquals(expectedUrl, playStoreUrl)
        assertTrue(playStoreUrl.contains(context.packageName))

        val marketUri = com.sashtech.mehndidesignsimple.utils.AppLinksHelper.getMarketUri(context)
        assertEquals("market://details?id=${context.packageName}", marketUri.toString())

        assertEquals("hello@sashtech.site", com.sashtech.mehndidesignsimple.utils.AppLinksHelper.SUPPORT_EMAIL)
        assertEquals("https://sashtech.site/apps/Mehndi/privacy_policy.html", com.sashtech.mehndidesignsimple.utils.AppLinksHelper.PRIVACY_POLICY_URL)
    }

    @Test
    fun `test safe invocation of all app link actions without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Test that none of these actions throw unhandled exceptions
        com.sashtech.mehndidesignsimple.utils.AppLinksHelper.shareApp(context)
        com.sashtech.mehndidesignsimple.utils.AppLinksHelper.rateApp(context)
        com.sashtech.mehndidesignsimple.utils.AppLinksHelper.launchInAppReview(context, fallbackToPlayStore = false)
        com.sashtech.mehndidesignsimple.utils.AppLinksHelper.contactUs(context)
        com.sashtech.mehndidesignsimple.utils.AppLinksHelper.sendFeedback(context)
        com.sashtech.mehndidesignsimple.utils.AppLinksHelper.openPrivacyPolicy(context)
    }

    @Test
    fun `test home viewmodel refresh data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val repository = com.sashtech.mehndidesignsimple.data.repository.MehndiRealtimeRepository(
            favoriteDao = database.favoriteDao(),
            designDao = database.designDao(),
            tutorialDao = database.tutorialDao(),
            categoryDao = database.categoryDao()
        )
        val viewModel = com.sashtech.mehndidesignsimple.viewmodel.HomeViewModel(repository)
        viewModel.refreshData()
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `test admob constants and unit id sanitization`() {
        val testBanner = com.sashtech.mehndidesignsimple.ads.AdMobConstants.BANNER_TEST_ID
        val testInterstitial = com.sashtech.mehndidesignsimple.ads.AdMobConstants.INTERSTITIAL_TEST_ID

        // Null, blank, placeholder inputs should fall back to safe official Google Test IDs
        assertEquals(testBanner, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeBannerAdUnitId(null))
        assertEquals(testBanner, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeBannerAdUnitId(""))
        assertEquals(testBanner, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeBannerAdUnitId("ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"))
        assertEquals(testBanner, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeBannerAdUnitId("invalid-format"))

        assertEquals(testInterstitial, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeInterstitialAdUnitId(null))
        assertEquals(testInterstitial, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeInterstitialAdUnitId(""))
        assertEquals(testInterstitial, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeInterstitialAdUnitId("ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"))

        // Valid custom AdMob ID should be preserved
        val validCustomId = "ca-app-pub-1234567890123456/1234567890"
        assertEquals(validCustomId, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeBannerAdUnitId(validCustomId))
        assertEquals(validCustomId, com.sashtech.mehndidesignsimple.ads.AdMobConstants.sanitizeInterstitialAdUnitId(validCustomId))
    }

    @Test
    fun `test ads config defaults and remote control logic`() {
        val defaultConfig = com.sashtech.mehndidesignsimple.data.model.AdsConfig()
        assertTrue(defaultConfig.enabled)
        assertTrue(defaultConfig.banner.enabled)
        assertTrue(defaultConfig.interstitial.enabled)
        assertEquals(3, defaultConfig.interstitial.showAfterClicks)

        val disabledConfig = defaultConfig.copy(enabled = false)
        assertEquals(false, disabledConfig.enabled)
    }

    @Test
    fun `test ad manager safe non blocking action dispatch`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val adManager = com.sashtech.mehndidesignsimple.ads.AdManager.getInstance(context)

        var actionExecuted = false
        adManager.onDesignAction(null) {
            actionExecuted = true
        }
        assertTrue(actionExecuted)
    }
}
