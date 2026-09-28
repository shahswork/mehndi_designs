package com.sashtech.mehndidesignsimple.data.repository

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sashtech.mehndidesignsimple.data.local.CachedCategoryEntity
import com.sashtech.mehndidesignsimple.data.local.CachedDesignEntity
import com.sashtech.mehndidesignsimple.data.local.CachedTutorialEntity
import com.sashtech.mehndidesignsimple.data.local.CachedTutorialStepEntity
import com.sashtech.mehndidesignsimple.data.local.CategoryDao
import com.sashtech.mehndidesignsimple.data.local.DesignDao
import com.sashtech.mehndidesignsimple.data.local.FavoriteDao
import com.sashtech.mehndidesignsimple.data.local.FavoriteDesignEntity
import com.sashtech.mehndidesignsimple.data.local.TutorialDao
import com.sashtech.mehndidesignsimple.data.model.MehndiCategory
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.data.model.StepByStepTutorial
import com.sashtech.mehndidesignsimple.data.model.TutorialStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope

class MehndiRealtimeRepository(
    private val favoriteDao: FavoriteDao,
    private val designDao: DesignDao? = null,
    private val tutorialDao: TutorialDao? = null,
    private val categoryDao: CategoryDao? = null,
    databaseProvider: (() -> FirebaseDatabase?)? = null
) {
    companion object {
        const val NODE_DESIGNS = "mehndi_designs"
        const val NODE_CATEGORIES = "categories"
        const val NODE_STEP_BY_STEP = "step_by_step"
        const val NODE_STEPS = "steps"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: FirebaseDatabase? by lazy {
        databaseProvider?.invoke() ?: runCatching {
            val db = FirebaseDatabase.getInstance()
            try {
                db.setPersistenceEnabled(true)
            } catch (_: Exception) {
                // Persistence can only be set before any other usage
            }
            db
        }.getOrNull()
    }

    init {
        // Initialize background synchronization listeners on active references
        syncDesignsFromFirebase()
        syncTutorialsFromFirebase()
    }

    /**
     * Checks if local Room database contains any cached categories, designs, or tutorials.
     */
    suspend fun hasCachedHomeData(): Boolean {
        return try {
            val catCount = categoryDao?.getCategoryCount() ?: 0
            val designCount = designDao?.getDesignCount() ?: 0
            val tutCount = tutorialDao?.getTutorialCount() ?: 0
            catCount > 0 || designCount > 0 || tutCount > 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Preloads essential Home screen data from Firebase Realtime Database with a timeout.
     * Updates Room database cache and warms Coil disk/memory cache for initial thumbnails.
     */
    suspend fun preloadInitialHomeData(context: Context, timeoutMs: Long = 3500L): Boolean {
        val db = database ?: return hasCachedHomeData()

        return withTimeoutOrNull(timeoutMs) {
            try {
                // Ensure nodes are marked for offline sync
                runCatching {
                    db.getReference(NODE_CATEGORIES).keepSynced(true)
                    db.getReference(NODE_DESIGNS).keepSynced(true)
                    db.getReference(NODE_STEP_BY_STEP).keepSynced(true)
                }

                // 1. Fetch Categories
                val catDeferred = async(Dispatchers.IO) {
                    try {
                        val snapshot = db.getReference(NODE_CATEGORIES).get().await()
                        if (snapshot.exists()) {
                            val items = mutableListOf<MehndiCategory>()
                            val entityList = mutableListOf<CachedCategoryEntity>()
                            var order = 0
                            for (child in snapshot.children) {
                                val catId = child.key ?: child.child("id").getValue(String::class.java) ?: continue
                                val catObj = child.getValue(MehndiCategory::class.java)
                                val isActive = child.child("isActive").getValue(Boolean::class.java)
                                    ?: catObj?.isActive ?: true
                                if (!isActive) continue

                                val name = child.child("name").getValue(String::class.java)
                                    ?: catObj?.name?.takeIf { it.isNotBlank() }
                                    ?: MehndiCategory.formatCategoryName(catId)
                                val desc = child.child("description").getValue(String::class.java)
                                    ?: catObj?.description ?: ""
                                val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    ?: child.child("image").getValue(String::class.java)
                                    ?: child.child("thumbnailUrl").getValue(String::class.java)
                                    ?: child.child("coverImageUrl").getValue(String::class.java)
                                    ?: catObj?.imageUrl ?: ""
                                val isStep = child.child("isStepByStep").getValue(Boolean::class.java)
                                    ?: catObj?.isStepByStep ?: (catId.equals("step_by_step", ignoreCase = true))
                                val count = child.child("defaultDesignCount").getValue(Int::class.java)
                                    ?: child.child("designCount").getValue(Int::class.java)
                                    ?: child.child("count").getValue(Int::class.java)
                                    ?: catObj?.defaultDesignCount ?: 0

                                val category = MehndiCategory(
                                    id = catId,
                                    name = name,
                                    description = desc,
                                    imageUrl = imageUrl,
                                    isStepByStep = isStep,
                                    defaultDesignCount = count,
                                    isActive = isActive
                                )
                                items.add(category)
                                entityList.add(CachedCategoryEntity.fromMehndiCategory(category, order++))
                            }
                            if (items.isNotEmpty()) {
                                categoryDao?.let { dao ->
                                    dao.removeStaleCategories(items.map { it.id })
                                    dao.insertCategories(entityList)
                                }
                            }
                            items
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                // 2. Fetch Designs
                val designsDeferred = async(Dispatchers.IO) {
                    try {
                        val snapshot = db.getReference(NODE_DESIGNS).get().await()
                        if (snapshot.exists()) {
                            val items = mutableListOf<MehndiDesign>()
                            for (child in snapshot.children) {
                                val item = parseDesignSnapshot(child)
                                if (item != null) items.add(item)
                            }
                            if (items.isNotEmpty()) {
                                designDao?.let { dao ->
                                    dao.removeStaleDesigns(items.map { it.id })
                                    dao.insertDesigns(items.map { CachedDesignEntity.fromMehndiDesign(it) })
                                }
                            }
                            items
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                // 3. Fetch Step-by-Step Tutorials
                val tutDeferred = async(Dispatchers.IO) {
                    try {
                        val snapshot = db.getReference(NODE_STEP_BY_STEP).get().await()
                        if (snapshot.exists()) {
                            val tutorials = mutableListOf<StepByStepTutorial>()
                            val allSteps = mutableListOf<CachedTutorialStepEntity>()
                            for (child in snapshot.children) {
                                val tut = parseTutorialSnapshot(child)
                                if (tut != null) {
                                    tutorials.add(tut)
                                    val stepsSnapshot = child.child(NODE_STEPS)
                                    if (stepsSnapshot.exists()) {
                                        for (stepChild in stepsSnapshot.children) {
                                            val stepId = stepChild.key ?: ""
                                            val stepNumber = stepChild.child("stepNumber").getValue(Int::class.java)
                                                ?: stepChild.child("number").getValue(Int::class.java)
                                                ?: stepChild.child("step").getValue(Int::class.java)
                                                ?: stepChild.key?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                                            val stepTitle = stepChild.child("title").getValue(String::class.java)
                                                ?: stepChild.child("name").getValue(String::class.java)
                                                ?: "Step $stepNumber"
                                            val stepDesc = stepChild.child("description").getValue(String::class.java)
                                                ?: stepChild.child("desc").getValue(String::class.java) ?: ""
                                            val stepImg = stepChild.child("imageUrl").getValue(String::class.java)
                                                ?: stepChild.child("image").getValue(String::class.java)
                                                ?: stepChild.child("url").getValue(String::class.java)
                                                ?: stepChild.child("thumbnailUrl").getValue(String::class.java) ?: ""
                                            val stepThumb = stepChild.child("thumbnailUrl").getValue(String::class.java)
                                                ?: stepChild.child("thumb").getValue(String::class.java)
                                                ?: stepImg
                                            val step = TutorialStep(
                                                id = stepId,
                                                stepNumber = stepNumber,
                                                title = stepTitle,
                                                description = stepDesc,
                                                imageUrl = stepImg,
                                                thumbnailUrl = stepThumb
                                            )
                                            allSteps.add(CachedTutorialStepEntity.fromTutorialStep(tut.id, step))
                                        }
                                    }
                                }
                            }
                            if (tutorials.isNotEmpty()) {
                                tutorialDao?.let { dao ->
                                    dao.removeStaleTutorials(tutorials.map { it.id })
                                    dao.insertTutorials(tutorials.map { CachedTutorialEntity.fromStepByStepTutorial(it) })
                                    if (allSteps.isNotEmpty()) {
                                        dao.insertTutorialSteps(allSteps)
                                    }
                                }
                            }
                            tutorials
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                val categories = catDeferred.await()
                val designs = designsDeferred.await()
                val tutorials = tutDeferred.await()

                // Warm Coil cache with top critical thumbnails asynchronously
                val imagesToWarm = mutableListOf<String>()
                tutorials.firstOrNull()?.coverImageUrl?.takeIf { it.isNotBlank() }?.let { imagesToWarm.add(it) }
                categories.take(4).mapNotNull { it.imageUrl.takeIf { url -> url.isNotBlank() } }.forEach { imagesToWarm.add(it) }
                designs.take(4).mapNotNull { it.getDisplayThumbnailUrl().takeIf { url -> url.isNotBlank() } }.forEach { imagesToWarm.add(it) }

                if (imagesToWarm.isNotEmpty()) {
                    runCatching {
                        val imageLoader = context.imageLoader
                        imagesToWarm.forEach { url ->
                            val request = ImageRequest.Builder(context)
                                .data(url)
                                .build()
                            imageLoader.enqueue(request)
                        }
                    }
                }

                hasCachedHomeData()
            } catch (_: Exception) {
                hasCachedHomeData()
            }
        } ?: hasCachedHomeData()
    }

    // Default sample fallback designs
    private val fallbackDesigns: List<MehndiDesign> = listOf(
        MehndiDesign(
            id = "bridal_sample_01",
            title = "Royal Heritage Bridal Full Arm",
            description = "Intricate traditional bridal henna with delicate dulhan motifs, floral jaal, and shaded peacocks.",
            categoryId = "bridal",
            categoryName = "Bridal Mehndi",
            imageUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=1200&q=80",
            mediumImageUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=800&q=80",
            thumbnailUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=400&q=80",
            featured = true,
            popular = true,
            difficulty = "Advanced",
            estimatedTime = "60 minutes",
            tags = listOf("bridal", "wedding", "heavy", "traditional"),
            searchKeywords = listOf("bridal mehndi", "wedding mehndi", "dulhan mehndi", "full arm"),
            type = "design"
        ),
        MehndiDesign(
            id = "front_hand_sample_01",
            title = "Lotus Blossom Front Palm",
            description = "Contemporary front palm mandala featuring shaded lotus petals and detailed fingertip lace.",
            categoryId = "front_hand",
            categoryName = "Front Hand Mehndi",
            imageUrl = "https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?auto=format&fit=crop&w=1200&q=80",
            mediumImageUrl = "https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?auto=format&fit=crop&w=800&q=80",
            thumbnailUrl = "https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?auto=format&fit=crop&w=400&q=80",
            featured = true,
            popular = true,
            difficulty = "Medium",
            estimatedTime = "30 minutes",
            tags = listOf("front_hand", "lotus", "mandala", "floral"),
            searchKeywords = listOf("front hand mehndi", "lotus mehndi", "palm mehndi"),
            type = "design"
        ),
        MehndiDesign(
            id = "back_hand_sample_01",
            title = "Geometric Lace Back Hand Trail",
            description = "Sleek diagonal geometric grid trail with floral wrist cuff and ring finger connection.",
            categoryId = "back_hand",
            categoryName = "Back Hand Mehndi",
            imageUrl = "https://images.unsplash.com/photo-1599839575945-a9e5af0c3fa5?auto=format&fit=crop&w=1200&q=80",
            mediumImageUrl = "https://images.unsplash.com/photo-1599839575945-a9e5af0c3fa5?auto=format&fit=crop&w=800&q=80",
            thumbnailUrl = "https://images.unsplash.com/photo-1599839575945-a9e5af0c3fa5?auto=format&fit=crop&w=400&q=80",
            featured = true,
            popular = true,
            difficulty = "Medium",
            estimatedTime = "25 minutes",
            tags = listOf("back_hand", "lace", "trail", "modern"),
            searchKeywords = listOf("back hand mehndi", "hathphool", "bel"),
            type = "design"
        ),
        MehndiDesign(
            id = "simple_sample_01",
            title = "Classic 5-Petal Mandala & Dots",
            description = "Crisp, clean beginner-friendly circular central flower with beaded outer border.",
            categoryId = "simple",
            categoryName = "Simple Mehndi",
            imageUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=1200&q=80",
            mediumImageUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=800&q=80",
            thumbnailUrl = "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=400&q=80",
            featured = true,
            popular = true,
            difficulty = "Easy",
            estimatedTime = "15 minutes",
            tags = listOf("simple", "beginner", "quick", "easy"),
            searchKeywords = listOf("simple mehndi", "easy mehndi", "beginner mehndi"),
            type = "design"
        ),
        MehndiDesign(
            id = "arabic_sample_01",
            title = "Modern Diagonal Arabic Bel",
            description = "Bold shaded Arabic paisley curves flowing diagonally with leafy clusters and open space.",
            categoryId = "arabic",
            categoryName = "Arabic Mehndi",
            imageUrl = "https://images.unsplash.com/photo-1596704017254-9b121068fb31?auto=format&fit=crop&w=1200&q=80",
            mediumImageUrl = "https://images.unsplash.com/photo-1596704017254-9b121068fb31?auto=format&fit=crop&w=800&q=80",
            thumbnailUrl = "https://images.unsplash.com/photo-1596704017254-9b121068fb31?auto=format&fit=crop&w=400&q=80",
            featured = true,
            popular = true,
            difficulty = "Easy",
            estimatedTime = "20 minutes",
            tags = listOf("arabic", "bel", "diagonal", "paisley"),
            searchKeywords = listOf("arabic mehndi", "arabic design", "bel mehndi"),
            type = "design"
        )
    )

    private val fallbackTutorials: List<StepByStepTutorial> = emptyList()

    private val fallbackTutorialSteps: Map<String, List<TutorialStep>> = emptyMap()

    private fun parseDesignSnapshot(snapshot: DataSnapshot): MehndiDesign? {
        val docId = snapshot.key ?: return null
        val design = snapshot.getValue(MehndiDesign::class.java)
        val catId = snapshot.child("categoryId").getValue(String::class.java)
            ?: snapshot.child("category").getValue(String::class.java)
            ?: design?.categoryId ?: "simple"
        val catName = snapshot.child("categoryName").getValue(String::class.java)
            ?: MehndiCategory.formatCategoryName(catId)
        val rawDifficulty = snapshot.child("difficulty").getValue(String::class.java)
            ?: design?.difficulty ?: "Easy"
        val normalizedDifficulty = MehndiDesign.normalizeDifficulty(rawDifficulty)

        return (design ?: MehndiDesign(
            id = docId,
            title = snapshot.child("title").getValue(String::class.java) ?: "",
            description = snapshot.child("description").getValue(String::class.java) ?: "",
            categoryId = catId,
            categoryName = catName,
            imageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: "",
            thumbnailUrl = snapshot.child("thumbnailUrl").getValue(String::class.java) ?: "",
            mediumImageUrl = snapshot.child("mediumImageUrl").getValue(String::class.java) ?: "",
            featured = snapshot.child("featured").getValue(Boolean::class.java) ?: false,
            popular = snapshot.child("popular").getValue(Boolean::class.java) ?: false,
            difficulty = normalizedDifficulty,
            estimatedTime = snapshot.child("estimatedTime").getValue(String::class.java) ?: "20 mins"
        )).copy(
            id = docId,
            categoryId = catId,
            categoryName = catName,
            difficulty = normalizedDifficulty
        )
    }

    private fun parseTutorialSnapshot(snapshot: DataSnapshot): StepByStepTutorial? {
        val docId = snapshot.key ?: return null
        val tut = snapshot.getValue(StepByStepTutorial::class.java)
        val title = snapshot.child("title").getValue(String::class.java)
            ?: tut?.title?.takeIf { it.isNotBlank() }
            ?: "Tutorial $docId"
        val desc = snapshot.child("description").getValue(String::class.java)
            ?: snapshot.child("desc").getValue(String::class.java)
            ?: tut?.description ?: ""
        val coverImageUrl = snapshot.child("coverImageUrl").getValue(String::class.java)
            ?: snapshot.child("imageUrl").getValue(String::class.java)
            ?: snapshot.child("image").getValue(String::class.java)
            ?: snapshot.child("thumbnailUrl").getValue(String::class.java)
            ?: snapshot.child("coverImage").getValue(String::class.java)
            ?: tut?.coverImageUrl ?: ""
        val rawDifficulty = snapshot.child("difficulty").getValue(String::class.java)
            ?: tut?.difficulty ?: "Easy"
        val normalizedDifficulty = MehndiDesign.normalizeDifficulty(rawDifficulty)
        val estimatedTime = snapshot.child("estimatedTime").getValue(String::class.java)
            ?: snapshot.child("time").getValue(String::class.java)
            ?: tut?.estimatedTime ?: "25 mins"
        val stepCount = snapshot.child("stepCount").getValue(Int::class.java)
            ?: snapshot.child("stepsCount").getValue(Int::class.java)
            ?: snapshot.child(NODE_STEPS).childrenCount.toInt().takeIf { it > 0 }
            ?: tut?.stepCount ?: 0
        val featured = snapshot.child("featured").getValue(Boolean::class.java)
            ?: tut?.featured ?: false

        return StepByStepTutorial(
            id = docId,
            title = title,
            description = desc,
            categoryId = "step_by_step",
            coverImageUrl = coverImageUrl,
            difficulty = normalizedDifficulty,
            estimatedTime = estimatedTime,
            stepCount = stepCount,
            featured = featured,
            type = "step_by_step"
        )
    }

    /**
     * Start background synchronization with Firebase Realtime Database.
     * When new data arrives, upserts to Room so collectors get reactive updates.
     */
    private fun syncDesignsFromFirebase() {
        val db = database ?: return
        val ref: DatabaseReference = db.getReference(NODE_DESIGNS)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val items = mutableListOf<MehndiDesign>()
                for (child in snapshot.children) {
                    val item = parseDesignSnapshot(child)
                    if (item != null) items.add(item)
                }
                repositoryScope.launch {
                    val validIds = items.map { it.id }
                    if (validIds.isEmpty()) {
                        designDao?.clearAll()
                    } else {
                        designDao?.removeStaleDesigns(validIds)
                        designDao?.insertDesigns(items.map { CachedDesignEntity.fromMehndiDesign(it) })
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Offline or permission issue, Room cache continues to serve seamlessly
            }
        })
    }

    private fun syncTutorialsFromFirebase() {
        val db = database ?: return
        val ref: DatabaseReference = db.getReference(NODE_STEP_BY_STEP)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val tutorials = mutableListOf<StepByStepTutorial>()
                val allSteps = mutableListOf<CachedTutorialStepEntity>()
                for (child in snapshot.children) {
                    val tut = parseTutorialSnapshot(child)
                    if (tut != null) {
                        tutorials.add(tut)
                        val stepsSnapshot = child.child(NODE_STEPS)
                        if (stepsSnapshot.exists()) {
                            for (stepChild in stepsSnapshot.children) {
                                val stepId = stepChild.key ?: ""
                                val stepNumber = stepChild.child("stepNumber").getValue(Int::class.java)
                                    ?: stepChild.child("number").getValue(Int::class.java)
                                    ?: stepChild.child("step").getValue(Int::class.java)
                                    ?: stepChild.key?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                                val stepTitle = stepChild.child("title").getValue(String::class.java)
                                    ?: stepChild.child("name").getValue(String::class.java)
                                    ?: "Step $stepNumber"
                                val stepDesc = stepChild.child("description").getValue(String::class.java)
                                    ?: stepChild.child("desc").getValue(String::class.java) ?: ""
                                val stepImg = stepChild.child("imageUrl").getValue(String::class.java)
                                    ?: stepChild.child("image").getValue(String::class.java)
                                    ?: stepChild.child("url").getValue(String::class.java)
                                    ?: stepChild.child("thumbnailUrl").getValue(String::class.java) ?: ""
                                val stepThumb = stepChild.child("thumbnailUrl").getValue(String::class.java)
                                    ?: stepChild.child("thumb").getValue(String::class.java)
                                    ?: stepImg
                                val step = TutorialStep(
                                    id = stepId,
                                    stepNumber = stepNumber,
                                    title = stepTitle,
                                    description = stepDesc,
                                    imageUrl = stepImg,
                                    thumbnailUrl = stepThumb
                                )
                                allSteps.add(CachedTutorialStepEntity.fromTutorialStep(tut.id, step))
                            }
                        }
                    }
                }
                repositoryScope.launch {
                    val validIds = tutorials.map { it.id }
                    if (validIds.isEmpty()) {
                        tutorialDao?.clearAllTutorials()
                    } else {
                        tutorialDao?.removeStaleTutorials(validIds)
                        tutorialDao?.insertTutorials(tutorials.map { CachedTutorialEntity.fromStepByStepTutorial(it) })
                        if (allSteps.isNotEmpty()) {
                            tutorialDao?.insertTutorialSteps(allSteps)
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Offline-first Latest Designs flow backed by Room Database Caching
     */
    fun getLatestDesigns(limit: Int = 30): Flow<List<MehndiDesign>> {
        syncDesignsFromFirebase()
        return if (designDao != null) {
            designDao.getLatestDesigns(limit).map { cachedList ->
                if (cachedList.isNotEmpty()) {
                    cachedList.map { it.toMehndiDesign() }
                } else {
                    fallbackDesigns.take(limit)
                }
            }.catch {
                emit(fallbackDesigns.take(limit))
            }
        } else {
            flow { emit(fallbackDesigns.take(limit)) }
        }
    }

    /**
     * Offline-first Featured Designs flow backed by Room Database Caching
     */
    fun getFeaturedDesigns(limit: Int = 10): Flow<List<MehndiDesign>> {
        syncDesignsFromFirebase()
        return if (designDao != null) {
            designDao.getFeaturedDesigns(limit).map { cachedList ->
                if (cachedList.isNotEmpty()) {
                    cachedList.map { it.toMehndiDesign() }
                } else {
                    fallbackDesigns.filter { it.featured }.take(limit)
                }
            }.catch {
                emit(fallbackDesigns.filter { it.featured }.take(limit))
            }
        } else {
            flow { emit(fallbackDesigns.filter { it.featured }.take(limit)) }
        }
    }

    /**
     * Offline-first Popular Designs flow backed by Room Database Caching
     */
    fun getPopularDesigns(limit: Int = 20): Flow<List<MehndiDesign>> {
        syncDesignsFromFirebase()
        return if (designDao != null) {
            designDao.getPopularDesigns(limit).map { cachedList ->
                if (cachedList.isNotEmpty()) {
                    cachedList.map { it.toMehndiDesign() }
                } else {
                    fallbackDesigns.filter { it.popular }.take(limit)
                }
            }.catch {
                emit(fallbackDesigns.filter { it.popular }.take(limit))
            }
        } else {
            flow { emit(fallbackDesigns.filter { it.popular }.take(limit)) }
        }
    }

    /**
     * Offline-first Category Designs flow backed by Room Database Caching
     */
    fun getDesignsByCategory(categoryId: String, limit: Int = 40): Flow<List<MehndiDesign>> {
        syncDesignsFromFirebase()
        val matchingFallback = fallbackDesigns.filter { it.categoryId.equals(categoryId, ignoreCase = true) }
        val defaultFallback = if (matchingFallback.isNotEmpty()) matchingFallback else fallbackDesigns.take(6)

        return if (designDao != null) {
            designDao.getDesignsByCategory(categoryId, limit).map { cachedList ->
                if (cachedList.isNotEmpty()) {
                    cachedList.map { it.toMehndiDesign() }
                } else {
                    defaultFallback
                }
            }.catch {
                emit(defaultFallback)
            }
        } else {
            flow { emit(defaultFallback) }
        }
    }

    /**
     * Offline-first Single Design by ID backed by Room Database Caching
     */
    fun getDesignById(designId: String): Flow<MehndiDesign?> {
        val fallback = fallbackDesigns.firstOrNull { it.id == designId } ?: fallbackDesigns.firstOrNull()

        // Background single fetch from Firebase
        val db = database
        if (db != null) {
            repositoryScope.launch {
                try {
                    val snapshot = db.getReference(NODE_DESIGNS).child(designId).get().await()
                    if (snapshot.exists()) {
                        val item = parseDesignSnapshot(snapshot)
                        if (item != null) {
                            designDao?.insertDesign(CachedDesignEntity.fromMehndiDesign(item))
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return if (designDao != null) {
            designDao.getDesignById(designId).map { cached ->
                cached?.toMehndiDesign() ?: fallback
            }.catch {
                emit(fallback)
            }
        } else {
            flow { emit(fallback) }
        }
    }

    /**
     * Categories flow - Single Source of Truth from Firebase Realtime Database
     * Synchronized with local Room categoryDao cache.
     * Never falls back to hardcoded categories.
     */
    fun getCategories(): Flow<List<MehndiCategory>> = callbackFlow {
        val db = database
        if (db == null) {
            // Offline / DB unavailable: read from local Room cache if present
            repositoryScope.launch {
                val cached = categoryDao?.getAllCategories()?.firstOrNull()?.map { it.toMehndiCategory() }
                trySend(cached ?: emptyList())
            }
            awaitClose { }
            return@callbackFlow
        }

        val ref: DatabaseReference = db.getReference(NODE_CATEGORIES)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    repositoryScope.launch {
                        categoryDao?.clearAllCategories()
                    }
                    trySend(emptyList())
                    return
                }
                val items = mutableListOf<MehndiCategory>()
                val entityList = mutableListOf<CachedCategoryEntity>()
                var order = 0
                for (child in snapshot.children) {
                    val catId = child.key ?: child.child("id").getValue(String::class.java) ?: continue
                    val catObj = child.getValue(MehndiCategory::class.java)
                    val isActive = child.child("isActive").getValue(Boolean::class.java)
                        ?: catObj?.isActive ?: true

                    // Ignore deactivated categories
                    if (!isActive) continue

                    val name = child.child("name").getValue(String::class.java)
                        ?: catObj?.name?.takeIf { it.isNotBlank() }
                        ?: MehndiCategory.formatCategoryName(catId)
                    val desc = child.child("description").getValue(String::class.java)
                        ?: catObj?.description ?: ""
                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                        ?: child.child("image").getValue(String::class.java)
                        ?: child.child("thumbnailUrl").getValue(String::class.java)
                        ?: child.child("coverImageUrl").getValue(String::class.java)
                        ?: catObj?.imageUrl ?: ""
                    val isStep = child.child("isStepByStep").getValue(Boolean::class.java)
                        ?: catObj?.isStepByStep ?: (catId.equals("step_by_step", ignoreCase = true))
                    val count = child.child("defaultDesignCount").getValue(Int::class.java)
                        ?: child.child("designCount").getValue(Int::class.java)
                        ?: child.child("count").getValue(Int::class.java)
                        ?: catObj?.defaultDesignCount ?: 0

                    val category = MehndiCategory(
                        id = catId,
                        name = name,
                        description = desc,
                        imageUrl = imageUrl,
                        isStepByStep = isStep,
                        defaultDesignCount = count,
                        isActive = isActive
                    )
                    items.add(category)
                    entityList.add(CachedCategoryEntity.fromMehndiCategory(category, order++))
                }

                // Sync with local Room cache
                repositoryScope.launch {
                    categoryDao?.let { dao ->
                        val validIds = items.map { it.id }
                        if (validIds.isEmpty()) {
                            dao.clearAllCategories()
                        } else {
                            dao.removeStaleCategories(validIds)
                            dao.insertCategories(entityList)
                        }
                    }
                }

                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) {
                repositoryScope.launch {
                    val cached = categoryDao?.getAllCategories()?.firstOrNull()?.map { it.toMehndiCategory() }
                    trySend(cached ?: emptyList())
                }
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.catch {
        emit(emptyList())
    }

    /**
     * Manually forces a full fresh synchronization with Firebase Realtime Database
     * for Categories, Step-by-Step Tutorials, and Designs, updating Room local cache.
     */
    suspend fun refreshAllData(): Boolean = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext false
        try {
            coroutineScope {
                // 1. Refresh Categories
                val catDeferred = async {
                try {
                    val snapshot = db.getReference(NODE_CATEGORIES).get().await()
                    if (snapshot.exists()) {
                        val items = mutableListOf<MehndiCategory>()
                        val entityList = mutableListOf<CachedCategoryEntity>()
                        var order = 0
                        for (child in snapshot.children) {
                            val catId = child.key ?: child.child("id").getValue(String::class.java) ?: continue
                            val catObj = child.getValue(MehndiCategory::class.java)
                            val isActive = child.child("isActive").getValue(Boolean::class.java)
                                ?: catObj?.isActive ?: true
                            if (!isActive) continue

                            val name = child.child("name").getValue(String::class.java)
                                ?: catObj?.name?.takeIf { it.isNotBlank() }
                                ?: MehndiCategory.formatCategoryName(catId)
                            val desc = child.child("description").getValue(String::class.java)
                                ?: catObj?.description ?: ""
                            val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                ?: child.child("image").getValue(String::class.java)
                                ?: child.child("thumbnailUrl").getValue(String::class.java)
                                ?: child.child("coverImageUrl").getValue(String::class.java)
                                ?: catObj?.imageUrl ?: ""
                            val isStep = child.child("isStepByStep").getValue(Boolean::class.java)
                                ?: catObj?.isStepByStep ?: (catId.equals("step_by_step", ignoreCase = true))
                            val count = child.child("defaultDesignCount").getValue(Int::class.java)
                                ?: child.child("designCount").getValue(Int::class.java)
                                ?: child.child("count").getValue(Int::class.java)
                                ?: catObj?.defaultDesignCount ?: 0

                            val category = MehndiCategory(
                                id = catId,
                                name = name,
                                description = desc,
                                imageUrl = imageUrl,
                                isStepByStep = isStep,
                                defaultDesignCount = count,
                                isActive = isActive
                            )
                            items.add(category)
                            entityList.add(CachedCategoryEntity.fromMehndiCategory(category, order++))
                        }
                        if (items.isNotEmpty()) {
                            categoryDao?.let { dao ->
                                dao.removeStaleCategories(items.map { it.id })
                                dao.insertCategories(entityList)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Refresh Designs
            val designDeferred = async {
                try {
                    val snapshot = db.getReference(NODE_DESIGNS).get().await()
                    if (snapshot.exists()) {
                        val items = mutableListOf<MehndiDesign>()
                        for (child in snapshot.children) {
                            val item = parseDesignSnapshot(child)
                            if (item != null) items.add(item)
                        }
                        if (items.isNotEmpty()) {
                            designDao?.let { dao ->
                                dao.removeStaleDesigns(items.map { it.id })
                                dao.insertDesigns(items.map { CachedDesignEntity.fromMehndiDesign(it) })
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Refresh Step-by-Step Tutorials
            val tutDeferred = async {
                try {
                    val snapshot = db.getReference(NODE_STEP_BY_STEP).get().await()
                    if (snapshot.exists()) {
                        val tutorials = mutableListOf<StepByStepTutorial>()
                        val allSteps = mutableListOf<CachedTutorialStepEntity>()
                        for (child in snapshot.children) {
                            val tut = parseTutorialSnapshot(child)
                            if (tut != null) {
                                tutorials.add(tut)
                                val stepsSnapshot = child.child(NODE_STEPS)
                                if (stepsSnapshot.exists()) {
                                    for (stepChild in stepsSnapshot.children) {
                                        val stepId = stepChild.key ?: ""
                                        val stepNumber = stepChild.child("stepNumber").getValue(Int::class.java)
                                            ?: stepChild.child("number").getValue(Int::class.java)
                                            ?: stepChild.child("step").getValue(Int::class.java)
                                            ?: stepChild.key?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                                        val stepTitle = stepChild.child("title").getValue(String::class.java)
                                            ?: stepChild.child("name").getValue(String::class.java)
                                            ?: "Step $stepNumber"
                                        val stepDesc = stepChild.child("description").getValue(String::class.java)
                                            ?: stepChild.child("desc").getValue(String::class.java) ?: ""
                                        val stepImg = stepChild.child("imageUrl").getValue(String::class.java)
                                            ?: stepChild.child("image").getValue(String::class.java)
                                            ?: stepChild.child("url").getValue(String::class.java)
                                            ?: stepChild.child("thumbnailUrl").getValue(String::class.java) ?: ""
                                        val stepThumb = stepChild.child("thumbnailUrl").getValue(String::class.java)
                                            ?: stepChild.child("thumb").getValue(String::class.java)
                                            ?: stepImg
                                        val step = TutorialStep(
                                            id = stepId,
                                            stepNumber = stepNumber,
                                            title = stepTitle,
                                            description = stepDesc,
                                            imageUrl = stepImg,
                                            thumbnailUrl = stepThumb
                                        )
                                        allSteps.add(CachedTutorialStepEntity.fromTutorialStep(tut.id, step))
                                    }
                                }
                            }
                        }
                        if (tutorials.isNotEmpty()) {
                            tutorialDao?.let { dao ->
                                dao.removeStaleTutorials(tutorials.map { it.id })
                                dao.insertTutorials(tutorials.map { CachedTutorialEntity.fromStepByStepTutorial(it) })
                                if (allSteps.isNotEmpty()) {
                                    dao.insertTutorialSteps(allSteps)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            catDeferred.await()
            designDeferred.await()
            tutDeferred.await()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Manually triggers a one-time fresh read from Firebase Realtime Database and updates the local cache
     */
    suspend fun refreshCategories() {
        val db = database ?: return
        val ref = db.getReference(NODE_CATEGORIES)
        try {
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                categoryDao?.clearAllCategories()
                return
            }
            val items = mutableListOf<MehndiCategory>()
            val entityList = mutableListOf<CachedCategoryEntity>()
            var order = 0
            for (child in snapshot.children) {
                val catId = child.key ?: child.child("id").getValue(String::class.java) ?: continue
                val catObj = child.getValue(MehndiCategory::class.java)
                val isActive = child.child("isActive").getValue(Boolean::class.java)
                    ?: catObj?.isActive ?: true

                if (!isActive) continue

                val name = child.child("name").getValue(String::class.java)
                    ?: catObj?.name?.takeIf { it.isNotBlank() }
                    ?: MehndiCategory.formatCategoryName(catId)
                val desc = child.child("description").getValue(String::class.java)
                    ?: catObj?.description ?: ""
                val imageUrl = child.child("imageUrl").getValue(String::class.java)
                    ?: child.child("image").getValue(String::class.java)
                    ?: child.child("thumbnailUrl").getValue(String::class.java)
                    ?: child.child("coverImageUrl").getValue(String::class.java)
                    ?: catObj?.imageUrl ?: ""
                val isStep = child.child("isStepByStep").getValue(Boolean::class.java)
                    ?: catObj?.isStepByStep ?: (catId.equals("step_by_step", ignoreCase = true))
                val count = child.child("defaultDesignCount").getValue(Int::class.java)
                    ?: child.child("designCount").getValue(Int::class.java)
                    ?: child.child("count").getValue(Int::class.java)
                    ?: catObj?.defaultDesignCount ?: 0

                val category = MehndiCategory(
                    id = catId,
                    name = name,
                    description = desc,
                    imageUrl = imageUrl,
                    isStepByStep = isStep,
                    defaultDesignCount = count,
                    isActive = isActive
                )
                items.add(category)
                entityList.add(CachedCategoryEntity.fromMehndiCategory(category, order++))
            }

            categoryDao?.let { dao ->
                val validIds = items.map { it.id }
                if (validIds.isEmpty()) {
                    dao.clearAllCategories()
                } else {
                    dao.removeStaleCategories(validIds)
                    dao.insertCategories(entityList)
                }
            }

            // Also refresh step-by-step tutorials from Firebase Realtime Database
            val tutSnapshot = db.getReference(NODE_STEP_BY_STEP).get().await()
            if (tutSnapshot.exists()) {
                val tutorials = mutableListOf<StepByStepTutorial>()
                val allSteps = mutableListOf<CachedTutorialStepEntity>()
                for (child in tutSnapshot.children) {
                    val tut = parseTutorialSnapshot(child)
                    if (tut != null) {
                        tutorials.add(tut)
                        val stepsSnapshot = child.child(NODE_STEPS)
                        if (stepsSnapshot.exists()) {
                            for (stepChild in stepsSnapshot.children) {
                                val stepId = stepChild.key ?: ""
                                val stepNumber = stepChild.child("stepNumber").getValue(Int::class.java)
                                    ?: stepChild.child("number").getValue(Int::class.java)
                                    ?: stepChild.child("step").getValue(Int::class.java)
                                    ?: stepChild.key?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                                val stepTitle = stepChild.child("title").getValue(String::class.java)
                                    ?: stepChild.child("name").getValue(String::class.java)
                                    ?: "Step $stepNumber"
                                val stepDesc = stepChild.child("description").getValue(String::class.java)
                                    ?: stepChild.child("desc").getValue(String::class.java) ?: ""
                                val stepImg = stepChild.child("imageUrl").getValue(String::class.java)
                                    ?: stepChild.child("image").getValue(String::class.java)
                                    ?: stepChild.child("url").getValue(String::class.java)
                                    ?: stepChild.child("thumbnailUrl").getValue(String::class.java) ?: ""
                                val stepThumb = stepChild.child("thumbnailUrl").getValue(String::class.java)
                                    ?: stepChild.child("thumb").getValue(String::class.java)
                                    ?: stepImg
                                val step = TutorialStep(
                                    id = stepId,
                                    stepNumber = stepNumber,
                                    title = stepTitle,
                                    description = stepDesc,
                                    imageUrl = stepImg,
                                    thumbnailUrl = stepThumb
                                )
                                allSteps.add(CachedTutorialStepEntity.fromTutorialStep(tut.id, step))
                            }
                        }
                    }
                }
                if (tutorials.isNotEmpty()) {
                    tutorialDao?.removeStaleTutorials(tutorials.map { it.id })
                    tutorialDao?.insertTutorials(tutorials.map { CachedTutorialEntity.fromStepByStepTutorial(it) })
                    if (allSteps.isNotEmpty()) {
                        tutorialDao?.insertTutorialSteps(allSteps)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Fetch a specific category dynamically by its ID from Firebase Realtime Database
     */
    fun getCategoryById(categoryId: String): Flow<MehndiCategory?> = callbackFlow {
        val db = database
        if (db == null) {
            repositoryScope.launch {
                val cached = categoryDao?.getCategoryByIdSync(categoryId)?.toMehndiCategory()
                trySend(cached)
            }
            awaitClose { }
            return@callbackFlow
        }

        val ref = db.getReference(NODE_CATEGORIES).child(categoryId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    repositoryScope.launch {
                        val cached = categoryDao?.getCategoryByIdSync(categoryId)?.toMehndiCategory()
                        trySend(cached)
                    }
                    return
                }
                val catObj = snapshot.getValue(MehndiCategory::class.java)
                val isActive = snapshot.child("isActive").getValue(Boolean::class.java)
                    ?: catObj?.isActive ?: true

                if (!isActive) {
                    trySend(null)
                    return
                }

                val name = snapshot.child("name").getValue(String::class.java)
                    ?: catObj?.name?.takeIf { it.isNotBlank() }
                    ?: MehndiCategory.formatCategoryName(categoryId)
                val desc = snapshot.child("description").getValue(String::class.java)
                    ?: catObj?.description ?: ""
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
                    ?: snapshot.child("image").getValue(String::class.java)
                    ?: snapshot.child("thumbnailUrl").getValue(String::class.java)
                    ?: snapshot.child("coverImageUrl").getValue(String::class.java)
                    ?: catObj?.imageUrl ?: ""
                val isStep = snapshot.child("isStepByStep").getValue(Boolean::class.java)
                    ?: catObj?.isStepByStep ?: (categoryId.equals("step_by_step", ignoreCase = true))
                val count = snapshot.child("defaultDesignCount").getValue(Int::class.java)
                    ?: snapshot.child("designCount").getValue(Int::class.java)
                    ?: catObj?.defaultDesignCount ?: 0

                trySend(
                    MehndiCategory(
                        id = categoryId,
                        name = name,
                        description = desc,
                        imageUrl = imageUrl,
                        isStepByStep = isStep,
                        defaultDesignCount = count,
                        isActive = isActive
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                repositoryScope.launch {
                    val cached = categoryDao?.getCategoryByIdSync(categoryId)?.toMehndiCategory()
                    trySend(cached)
                }
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }.catch {
        emit(null)
    }

    /**
     * Offline-first Step-by-Step Tutorials flow backed by Room Database Caching
     */
    fun getStepByStepTutorials(limit: Int = 20): Flow<List<StepByStepTutorial>> {
        syncTutorialsFromFirebase()
        return if (tutorialDao != null) {
            tutorialDao.getTutorials(limit).map { cachedList ->
                cachedList.map { it.toStepByStepTutorial() }
            }.catch {
                emit(emptyList())
            }
        } else {
            flow { emit(emptyList()) }
        }
    }

    /**
     * Offline-first Tutorial by ID backed by Room Database Caching
     */
    fun getTutorialById(tutorialId: String): Flow<StepByStepTutorial?> {
        val db = database
        if (db != null) {
            repositoryScope.launch {
                try {
                    val snapshot = db.getReference(NODE_STEP_BY_STEP).child(tutorialId).get().await()
                    if (snapshot.exists()) {
                        val item = parseTutorialSnapshot(snapshot)
                        if (item != null) {
                            tutorialDao?.insertTutorial(CachedTutorialEntity.fromStepByStepTutorial(item))
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return if (tutorialDao != null) {
            tutorialDao.getTutorialById(tutorialId).map { cached ->
                cached?.toStepByStepTutorial()
            }.catch {
                emit(null)
            }
        } else {
            flow { emit(null) }
        }
    }

    /**
     * Offline-first Tutorial Steps flow backed by Room Database Caching
     */
    fun getTutorialSteps(tutorialId: String): Flow<List<TutorialStep>> {
        val db = database
        if (db != null) {
            repositoryScope.launch {
                try {
                    val snapshot = db.getReference(NODE_STEP_BY_STEP).child(tutorialId).child(NODE_STEPS).get().await()
                    if (snapshot.exists()) {
                        val items = mutableListOf<TutorialStep>()
                        for (child in snapshot.children) {
                            val stepId = child.key ?: ""
                            val stepNumber = child.child("stepNumber").getValue(Int::class.java)
                                ?: child.child("number").getValue(Int::class.java)
                                ?: child.child("step").getValue(Int::class.java)
                                ?: child.key?.filter { it.isDigit() }?.toIntOrNull() ?: 1
                            val title = child.child("title").getValue(String::class.java)
                                ?: child.child("name").getValue(String::class.java)
                                ?: "Step $stepNumber"
                            val desc = child.child("description").getValue(String::class.java)
                                ?: child.child("desc").getValue(String::class.java) ?: ""
                            val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                ?: child.child("image").getValue(String::class.java)
                                ?: child.child("url").getValue(String::class.java)
                                ?: child.child("thumbnailUrl").getValue(String::class.java) ?: ""
                            val thumbUrl = child.child("thumbnailUrl").getValue(String::class.java)
                                ?: child.child("thumb").getValue(String::class.java)
                                ?: imageUrl
                            items.add(
                                TutorialStep(
                                    id = stepId,
                                    stepNumber = stepNumber,
                                    title = title,
                                    description = desc,
                                    imageUrl = imageUrl,
                                    thumbnailUrl = thumbUrl
                                )
                            )
                        }
                        if (items.isNotEmpty()) {
                            tutorialDao?.insertTutorialSteps(
                                items.map { CachedTutorialStepEntity.fromTutorialStep(tutorialId, it) }
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return if (tutorialDao != null) {
            tutorialDao.getStepsForTutorial(tutorialId).map { cachedSteps ->
                cachedSteps.map { it.toTutorialStep() }
            }.catch {
                emit(emptyList())
            }
        } else {
            flow { emit(emptyList()) }
        }
    }

    /**
     * Offline-first Search across cached Room database with remote fallback
     */
    fun searchDesigns(query: String): Flow<List<MehndiDesign>> {
        if (query.isBlank()) {
            return flow { emit(emptyList()) }
        }
        val cleanQuery = query.trim().lowercase()

        return if (designDao != null) {
            designDao.searchDesigns(cleanQuery).map { cachedResults ->
                if (cachedResults.isNotEmpty()) {
                    cachedResults.map { it.toMehndiDesign() }
                } else {
                    // In-memory fallback matching
                    fallbackDesigns.filter { design ->
                        design.title.lowercase().contains(cleanQuery) ||
                            design.categoryId.lowercase().contains(cleanQuery) ||
                            design.categoryName.lowercase().contains(cleanQuery) ||
                            design.description.lowercase().contains(cleanQuery) ||
                            design.tags.any { it.lowercase().contains(cleanQuery) } ||
                            design.searchKeywords.any { it.lowercase().contains(cleanQuery) }
                    }
                }
            }.catch {
                emit(emptyList())
            }
        } else {
            flow {
                val results = fallbackDesigns.filter { design ->
                    design.title.lowercase().contains(cleanQuery) ||
                        design.categoryId.lowercase().contains(cleanQuery) ||
                        design.categoryName.lowercase().contains(cleanQuery) ||
                        design.description.lowercase().contains(cleanQuery) ||
                        design.tags.any { it.lowercase().contains(cleanQuery) } ||
                        design.searchKeywords.any { it.lowercase().contains(cleanQuery) }
                }
                emit(results)
            }
        }
    }

    // Room Favorites
    val allFavorites: Flow<List<FavoriteDesignEntity>> = favoriteDao.getAllFavorites()

    fun isFavorite(designId: String): Flow<Boolean> = favoriteDao.isFavorite(designId)

    suspend fun toggleFavorite(design: MehndiDesign): Boolean {
        val isFav = favoriteDao.isFavoriteSync(design.id)
        if (isFav) {
            favoriteDao.deleteFavorite(design.id)
            return false
        } else {
            favoriteDao.insertFavorite(FavoriteDesignEntity.fromMehndiDesign(design))
            return true
        }
    }

    suspend fun removeFavorite(designId: String) {
        favoriteDao.deleteFavorite(designId)
    }
}
