package com.example.studyplatform.ui

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
import androidx.savedstate.read
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.studyplatform.api.ApiClient
import com.example.studyplatform.data.AppData
import com.example.studyplatform.model.QuizAttemptResponse
import com.example.studyplatform.ui.components.AnimatedEntry
import com.example.studyplatform.ui.components.Motion
import com.example.studyplatform.ui.components.pressScale
import com.example.studyplatform.ui.screens.PomodoroScreen
import com.example.studyplatform.ui.screens.auth.LoginScreen
import com.example.studyplatform.ui.screens.auth.OnboardingScreen
import com.example.studyplatform.ui.screens.auth.RegisterScreen
import com.example.studyplatform.ui.screens.auth.VerifyOtpScreen
import com.example.studyplatform.ui.screens.auth.WelcomeScreen
import com.example.studyplatform.ui.screens.dashboard.DashboardScreen
import com.example.studyplatform.ui.screens.documents.DocumentsScreen
import com.example.studyplatform.ui.screens.explanations.ExplanationScreen
import com.example.studyplatform.ui.screens.groups.GroupDetailScreen
import com.example.studyplatform.ui.screens.groups.GroupListScreen
import com.example.studyplatform.ui.screens.guest.CourseReaderScreen
import com.example.studyplatform.ui.screens.guest.GuestBrowseScreen
import com.example.studyplatform.ui.screens.guides.GuideCreateScreen
import com.example.studyplatform.ui.screens.guides.GuideListScreen
import com.example.studyplatform.ui.screens.guides.GuideViewScreen
import com.example.studyplatform.ui.screens.notes.NotesScreen
import com.example.studyplatform.ui.screens.notifications.NotificationsScreen
import com.example.studyplatform.ui.screens.quizzes.FlashcardScreen
import com.example.studyplatform.ui.screens.quizzes.QuizCreateScreen
import com.example.studyplatform.ui.screens.quizzes.QuizListScreen
import com.example.studyplatform.ui.screens.quizzes.QuizResultScreen
import com.example.studyplatform.ui.screens.quizzes.QuizTakeScreen
import com.example.studyplatform.ui.screens.search.SearchScreen
import com.example.studyplatform.ui.screens.settings.SettingsScreen
import com.example.studyplatform.ui.screens.stats.StatsScreen
import com.example.studyplatform.ui.screens.tournaments.TournamentCompeteScreen
import com.example.studyplatform.ui.screens.tournaments.TournamentDetailScreen
import com.example.studyplatform.ui.screens.tournaments.TournamentsScreen
import com.example.studyplatform.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import studyplatform.shared.generated.resources.*

data class BottomNavItem(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

@Composable
fun EduNovaApp(
    /**
     * A route the platform resolved from a link, or null.
     *
     * A route rather than a URI: parsing one is the host platform's job — an Intent on
     * Android, a UIApplication callback on iOS — and the closed set of destinations a
     * link may name is already enforced there.
     */
    pendingRoute: String? = null,
    onRouteHandled: () -> Unit = {},
    /**
     * The platform's federated sign-in button, if it has one.
     *
     * Takes the success callback rather than owning it, because where a successful
     * sign-in should land differs by screen — the login screen goes to the dashboard,
     * registration goes to onboarding, since a Google account arrives with a name and a
     * verified address and nothing else. Navigation belongs to this graph, and the
     * platform only supplies the button.
     */
    googleButton: @Composable (onSignedIn: () -> Unit) -> Unit = {}
) {
    val navController = rememberNavController()
    // Signed out lands on the pitch, not the form. Someone who already has an account
    // reaches login in one tap from there.
    val startDest = if (ApiClient.isLoggedIn()) "dashboard" else "welcome"
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute)
            onRouteHandled()
        }
    }

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
        listOf(
            "welcome", "login", "register", "verify", "onboarding", "guest",
            "guides/create", "quizzes/create", "quizzes/flashcards", "tournaments/compete"
        )
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
            composable("welcome") {
                WelcomeScreen(
                    onCreateAccount = { navController.navigate("register") },
                    onSignIn = { navController.navigate("login") },
                    onBrowse = { navController.navigate("guest") },
                    // A demo session is a real session, so it clears the stack the same
                    // way a normal sign-in does — back must not return to the pitch.
                    onDemoSignedIn = {
                        navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { navController.navigate("dashboard") { popUpTo(0) { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate("register") },
                    // Correct credentials but an unconfirmed address — the backend has
                    // already re-sent a code, so go straight to verification.
                    onEmailNotVerified = { email -> navController.navigate("verify/$email") },
                    onBrowseAsGuest = { navController.navigate("guest") },
                    // Credential Manager is Android's, so the shared screen takes the
                    // button as a slot rather than owning it. Where it lands is decided
                    // here, not by the platform supplying the button.
                    googleButton = {
                        googleButton {
                            navController.navigate("dashboard") { popUpTo(0) { inclusive = true } }
                        }
                    }
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
                    // Google gives us a name and a verified address and nothing else,
                    // so this is exactly the account that most needs onboarding.
                    onGoogleSignUp = {
                        navController.navigate("onboarding") { popUpTo(0) { inclusive = true } }
                    },
                    googleButton = {
                        googleButton {
                            navController.navigate("onboarding") { popUpTo(0) { inclusive = true } }
                        }
                    }
                )
            }
            composable("verify/{email}", arguments = listOf(navArgument("email") { type = NavType.StringType })) {
                VerifyOtpScreen(
                    email = it.argOrNull("email") ?: "",
                    // Verification is what establishes the session, so it is the first
                    // moment onboarding can save anything.
                    onVerified = { navController.navigate("onboarding") { popUpTo(0) { inclusive = true } } },
                    onBack = { navController.navigate("register") { popUpTo("login") } }
                )
            }
            composable("onboarding") {
                OnboardingScreen(
                    onDone = { navController.navigate("dashboard") { popUpTo(0) { inclusive = true } } }
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
                    slug = it.argOrNull("slug") ?: "",
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
                GuideViewScreen(guideId = it.argOrNull("guideId") ?: "", onBack = { navController.popBackStack() })
            }

            // ── Quizzes ──
            composable(
                "quizzes/flashcards/{quizId}",
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                FlashcardScreen(
                    quizId = it.argOrNull("quizId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
            composable("quizzes") {
                QuizListScreen(
                    onFlashcards = { id -> navController.navigate("quizzes/flashcards/$id") },
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
                    quizId = it.argOrNull("quizId") ?: "",
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
                GroupDetailScreen(groupId = it.argOrNull("groupId") ?: "", onBack = { navController.popBackStack() })
            }

            // ── More menu routes ──
            composable("more") {
                MoreScreen(
                    onExplanations = { navController.navigate("explanations") },
                    onNotes = { navController.navigate("notes") },
                    onStats = { navController.navigate("stats") },
                    onDocuments = { navController.navigate("documents") },
                    onPomodoro = { navController.navigate("pomodoro") },
                    onNotifications = { navController.navigate("notifications") },
                    onTournaments = { navController.navigate("tournaments") },
                    onSearch = { navController.navigate("search") },
                    onSettings = { navController.navigate("settings") }
                )
            }
            composable("search") {
                SearchScreen(
                    onOpenGuide = { id -> navController.navigate("guides/view/$id") },
                    onOpenQuiz = { id -> navController.navigate("quizzes/take/$id") },
                    onOpenGroup = { id -> navController.navigate("groups/detail/$id") },
                    onOpenCourse = { slug -> navController.navigate("guest/course/$slug") }
                )
            }
            composable("tournaments") {
                TournamentsScreen(onOpen = { id -> navController.navigate("tournaments/detail/$id") })
            }
            composable(
                "tournaments/detail/{tournamentId}",
                arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
            ) {
                TournamentDetailScreen(
                    tournamentId = it.argOrNull("tournamentId") ?: "",
                    onBack = { navController.popBackStack() },
                    onCompete = { id -> navController.navigate("tournaments/compete/$id") }
                )
            }
            composable(
                "tournaments/compete/{tournamentId}",
                arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
            ) {
                TournamentCompeteScreen(
                    tournamentId = it.argOrNull("tournamentId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
            composable("explanations") { ExplanationScreen() }
            composable("notes") { NotesScreen() }
            composable("stats") { StatsScreen() }
            composable("documents") { DocumentsScreen() }
            composable("pomodoro") { PomodoroScreen() }
            composable("notifications") {
                NotificationsScreen(
                    onOpenGroup = { groupId -> navController.navigate("groups/detail/$groupId") }
                )
            }
            composable("settings") {
                // Signing out returns to the same place a fresh install starts, so a
                // demo session ends where the next person would begin.
                SettingsScreen(onLogout = { navController.navigate("welcome") { popUpTo(0) { inclusive = true } } })
            }
        }
    }
}

@Composable
fun MoreScreen(
    onExplanations: () -> Unit,
    onNotes: () -> Unit,
    onStats: () -> Unit,
    onDocuments: () -> Unit,
    onPomodoro: () -> Unit,
    onNotifications: () -> Unit,
    onTournaments: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    // These were English literals while every other screen went through strings.xml —
    // the i18n pass missed this file, so a French device saw a French app with an
    // English menu in the middle of it.
    val items = listOf(
        // First: it is the fastest route to anything already made, which is what people
        // open this menu looking for.
        Triple(
            stringResource(Res.string.more_search),
            stringResource(Res.string.more_search_sub),
            Icons.Filled.Search
        ) to onSearch,
        Triple(
            stringResource(Res.string.more_explanations),
            stringResource(Res.string.more_explanations_sub),
            Icons.Filled.Lightbulb
        ) to onExplanations,
        Triple(
            stringResource(Res.string.more_notes),
            stringResource(Res.string.more_notes_sub),
            Icons.Filled.EditNote
        ) to onNotes,
        Triple(
            stringResource(Res.string.more_documents),
            stringResource(Res.string.more_documents_sub),
            Icons.Filled.Folder
        ) to onDocuments,
        Triple(
            stringResource(Res.string.more_pomodoro),
            stringResource(Res.string.more_pomodoro_sub),
            Icons.Filled.Timer
        ) to onPomodoro,
        Triple(
            stringResource(Res.string.more_notifications),
            stringResource(Res.string.more_notifications_sub),
            Icons.Filled.Notifications
        ) to onNotifications,
        Triple(
            stringResource(Res.string.more_tournaments),
            stringResource(Res.string.more_tournaments_sub),
            Icons.Filled.EmojiEvents
        ) to onTournaments,
        Triple(
            stringResource(Res.string.more_stats),
            stringResource(Res.string.more_stats_sub),
            Icons.Filled.BarChart
        ) to onStats,
        Triple(
            stringResource(Res.string.more_settings),
            stringResource(Res.string.more_settings_sub),
            Icons.Filled.Settings
        ) to onSettings,
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
                    Text(
                        stringResource(Res.string.more_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
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

/**
 * A string route argument, or null.
 *
 * `entry.arguments` is an Android `Bundle` under the androidx navigation library and a
 * multiplatform `SavedState` under the JetBrains one, so the direct `getString` the
 * Android app used does not exist in common code. Reading it through one helper keeps
 * that difference in a single place rather than at every destination.
 */
private fun NavBackStackEntry.argOrNull(name: String): String? =
    arguments?.read { if (contains(name)) getString(name) else null }
