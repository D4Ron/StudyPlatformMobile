package com.example.studyplatform.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.android.ui.auth.LoginScreen
import com.example.studyplatform.android.ui.auth.RegisterScreen
import com.example.studyplatform.android.ui.auth.VerifyOtpScreen
import com.example.studyplatform.android.ui.dashboard.DashboardScreen
import com.example.studyplatform.android.ui.explanations.ExplanationScreen
import com.example.studyplatform.android.ui.groups.GroupDetailScreen
import com.example.studyplatform.android.ui.guest.CourseReaderScreen
import com.example.studyplatform.android.ui.guest.GuestBrowseScreen
import com.example.studyplatform.android.ui.groups.GroupListScreen
import com.example.studyplatform.android.ui.guides.GuideCreateScreen
import com.example.studyplatform.android.ui.guides.GuideListScreen
import com.example.studyplatform.android.ui.guides.GuideViewScreen
import com.example.studyplatform.android.ui.notes.NotesScreen
import com.example.studyplatform.android.ui.quizzes.QuizCreateScreen
import com.example.studyplatform.android.ui.quizzes.QuizListScreen
import com.example.studyplatform.android.ui.quizzes.QuizResultScreen
import com.example.studyplatform.android.ui.quizzes.QuizTakeScreen
import com.example.studyplatform.android.ui.settings.SettingsScreen
import com.example.studyplatform.android.ui.stats.StatsScreen
import com.example.studyplatform.android.components.AnimatedEntry
import com.example.studyplatform.android.components.Motion
import com.example.studyplatform.android.components.pressScale
import com.example.studyplatform.android.sync.SyncWorker
import com.example.studyplatform.api.ApiClient
import tg.edunova.app.BuildConfig
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.DatabaseDriverFactory
import com.example.studyplatform.model.QuizAttemptResponse

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Before anything can make a request. Blank in a normal build, so this is a
        // no-op unless someone has pointed it at a local backend.
        ApiClient.useBaseUrl(BuildConfig.API_BASE_URL)

        // Opened before any screen composes: the notes list reads from it immediately,
        // and it has to exist whether or not there is a connection.
        AppData.init(DatabaseDriverFactory(applicationContext))
        SyncWorker.schedule(applicationContext)
        if (ApiClient.isLoggedIn()) SyncWorker.syncNow(applicationContext)

        setContent { StudyPlatformTheme { StudyPlatformApp() } }
    }
}

data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

@Composable
fun StudyPlatformApp() {
    val navController = rememberNavController()
    val startDest = if (ApiClient.isLoggedIn()) "dashboard" else "login"
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Store quiz result for result screen
    var lastQuizResult by remember { mutableStateOf<QuizAttemptResponse?>(null) }

    val navItems = listOf(
        BottomNavItem("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("guides", "Guides", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        BottomNavItem("quizzes", "Quizzes", Icons.Filled.Quiz, Icons.Outlined.Quiz),
        BottomNavItem("groups", "Groups", Icons.Filled.Group, Icons.Outlined.Group),
        BottomNavItem("more", "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz),
    )

    // Guest mode has no bottom bar: every tab behind it needs an account, and offering
    // them to a visitor only to refuse would be worse than not offering them.
    val hideBottomBarRoutes =
        listOf("login", "register", "verify", "guest", "guides/create", "quizzes/create")
    val showBottomBar = currentRoute != null &&
            hideBottomBarRoutes.none { currentRoute.startsWith(it) } &&
            !currentRoute.contains("view/") &&
            !currentRoute.contains("take/") &&
            !currentRoute.contains("result") &&
            !currentRoute.contains("detail/")

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Surface, tonalElevation = 2.dp) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route || currentRoute?.startsWith(item.route + "/") == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true; restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, item.label) },
                            label = { Text(item.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary, selectedTextColor = Primary,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = PrimaryLight
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding),
            // Screens slide in from the side they came from, so forward and back are
            // distinguishable without reading the header.
            enterTransition = {
                slideInHorizontally(tween(Motion.NORMAL, easing = EaseOutCubic)) { it / 5 } +
                        fadeIn(tween(Motion.NORMAL))
            },
            exitTransition = { fadeOut(tween(Motion.QUICK)) },
            popEnterTransition = {
                slideInHorizontally(tween(Motion.NORMAL, easing = EaseOutCubic)) { -it / 5 } +
                        fadeIn(tween(Motion.NORMAL))
            },
            popExitTransition = { fadeOut(tween(Motion.QUICK)) }
        ) {
            // ── Auth ──
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { navController.navigate("dashboard") { popUpTo(0) { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate("register") },
                    // Correct credentials but an unconfirmed address — the backend has
                    // already re-sent a code, so go straight to verification.
                    onEmailNotVerified = { email -> navController.navigate("verify/$email") },
                    onBrowseAsGuest = { navController.navigate("guest") }
                )
            }
            composable("register") {
                RegisterScreen(
                    // Registration creates an unverified account and issues no session;
                    // verification is what signs the user in.
                    onRegisterSuccess = { email -> navController.navigate("verify/$email") },
                    onNavigateToLogin = { navController.popBackStack() },
                    // Google has already verified the address, so this lands on the
                    // dashboard rather than the code screen — there is no code.
                    onGoogleSignUp = {
                        navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("verify/{email}", arguments = listOf(navArgument("email") { type = NavType.StringType })) {
                VerifyOtpScreen(
                    email = it.arguments?.getString("email") ?: "",
                    onVerified = { navController.navigate("dashboard") { popUpTo(0) { inclusive = true } } },
                    onBack = { navController.navigate("register") { popUpTo("login") } }
                )
            }

            // ── Guest mode ──
            //
            // Reachable without a session. Sign-up from here goes to register and clears
            // the guest screens off the stack: someone who has just made an account
            // should not be able to press back into the anonymous view.
            composable("guest") {
                GuestBrowseScreen(
                    onOpenCourse = { slug -> navController.navigate("guest/course/$slug") },
                    onSignUp = { navController.navigate("register") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "guest/course/{slug}",
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) {
                CourseReaderScreen(
                    slug = it.arguments?.getString("slug") ?: "",
                    onBack = { navController.popBackStack() },
                    onSignUp = { navController.navigate("register") }
                )
            }

            // ── Dashboard ──
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToGuides = { navController.navigate("guides/create") },
                    onNavigateToQuizzes = { navController.navigate("quizzes/create") },
                    onNavigateToExplanations = { navController.navigate("explanations") }
                )
            }

            // ── Guides ──
            composable("guides") {
                GuideListScreen(
                    onCreateGuide = { navController.navigate("guides/create") },
                    onViewGuide = { id -> navController.navigate("guides/view/$id") }
                )
            }
            composable("guides/create") {
                GuideCreateScreen(
                    onGuideCreated = { id -> navController.navigate("guides/view/$id") { popUpTo("guides") } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("guides/view/{guideId}", arguments = listOf(navArgument("guideId") { type = NavType.StringType })) {
                GuideViewScreen(guideId = it.arguments?.getString("guideId") ?: "", onBack = { navController.popBackStack() })
            }

            // ── Quizzes ──
            composable("quizzes") {
                QuizListScreen(
                    onCreateQuiz = { navController.navigate("quizzes/create") },
                    onTakeQuiz = { id -> navController.navigate("quizzes/take/$id") }
                )
            }
            composable("quizzes/create") {
                QuizCreateScreen(
                    onQuizCreated = { id -> navController.navigate("quizzes/take/$id") { popUpTo("quizzes") } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("quizzes/take/{quizId}", arguments = listOf(navArgument("quizId") { type = NavType.StringType })) {
                QuizTakeScreen(
                    quizId = it.arguments?.getString("quizId") ?: "",
                    onFinished = { result -> lastQuizResult = result; navController.navigate("quizzes/result") { popUpTo("quizzes") } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("quizzes/result") {
                lastQuizResult?.let { result ->
                    QuizResultScreen(
                        attempt = result,
                        onRetake = { navController.navigate("quizzes/take/${result.quizId}") { popUpTo("quizzes") } },
                        onDashboard = { navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } } }
                    )
                }
            }

            // ── Groups ──
            composable("groups") {
                GroupListScreen(onGroupClick = { id -> navController.navigate("groups/detail/$id") })
            }
            composable("groups/detail/{groupId}", arguments = listOf(navArgument("groupId") { type = NavType.StringType })) {
                GroupDetailScreen(groupId = it.arguments?.getString("groupId") ?: "", onBack = { navController.popBackStack() })
            }

            // ── More menu routes ──
            composable("more") {
                MoreScreen(
                    onExplanations = { navController.navigate("explanations") },
                    onNotes = { navController.navigate("notes") },
                    onStats = { navController.navigate("stats") },
                    onSettings = { navController.navigate("settings") }
                )
            }
            composable("explanations") { ExplanationScreen() }
            composable("notes") { NotesScreen() }
            composable("stats") { StatsScreen() }
            composable("settings") {
                SettingsScreen(onLogout = { navController.navigate("login") { popUpTo(0) { inclusive = true } } })
            }
        }
    }
}

@Composable
fun MoreScreen(onExplanations: () -> Unit, onNotes: () -> Unit, onStats: () -> Unit, onSettings: () -> Unit) {
    val items = listOf(
        Triple("Explain a Concept", "AI-powered explanations", Icons.Filled.Lightbulb) to onExplanations,
        Triple("My Notes", "Personal study notes", Icons.Filled.EditNote) to onNotes,
        Triple("Statistics", "Track your progress", Icons.Filled.BarChart) to onStats,
        Triple("Settings", "Account & preferences", Icons.Filled.Settings) to onSettings,
    )

    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp)
        ) {
            item {
                AnimatedEntry {
                    Text("More", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                }
            }
            items.forEachIndexed { index, (triple, action) ->
                item {
                    AnimatedEntry(index = index + 1) {
                        val interaction = remember { MutableInteractionSource() }
                        Card(
                            onClick = action,
                            interactionSource = interaction,
                            modifier = Modifier.fillMaxWidth().pressScale(interaction),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            androidx.compose.foundation.layout.Row(
                                Modifier.padding(18.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(triple.third, null, tint = Primary)
                                androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                    Text(triple.first, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(triple.second, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
