package com.aidebate.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aidebate.presentation.debate.DebateScreen
import com.aidebate.presentation.donation.DonationScreen
import com.aidebate.presentation.history.DebateHistoryScreen
import com.aidebate.presentation.home.HomeScreen
import com.aidebate.presentation.result.DebateResultScreen
import com.aidebate.presentation.settings.SettingsScreen
import com.aidebate.presentation.settings.provider.ProviderConfigScreen
import com.aidebate.presentation.setup.DebateSetupScreen
import com.aidebate.presentation.topic.TopicSelectionScreen
import com.aidebate.presentation.tools.ToolsScreen
import com.aidebate.presentation.argumentmap.ArgumentMapScreen
import com.aidebate.presentation.rebuttal.RebuttalTrainerScreen
import com.aidebate.presentation.fallacy.FallacyDetectorScreen
import com.aidebate.presentation.facetoface.FaceToFaceScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object TopicSelection : Screen("topic_selection")
    data object DebateSetup : Screen("debate_setup/{topicId}") {
        fun createRoute(topicId: String) = "debate_setup/$topicId"
    }
    data object Debate : Screen("debate/{sessionId}") {
        fun createRoute(sessionId: String) = "debate/$sessionId"
    }
    data object DebateResult : Screen("debate_result/{sessionId}") {
        fun createRoute(sessionId: String) = "debate_result/$sessionId"
    }
    data object History : Screen("history")
    data object Tools : Screen("tools")
    data object Settings : Screen("settings")
    data object ProviderConfig : Screen("provider_config/{providerName}") {
        fun createRoute(providerName: String) = "provider_config/$providerName"
    }
    data object ArgumentMap : Screen("argument_map/{topicId}") {
        fun createRoute(topicId: String) = "argument_map/$topicId"
    }
    data object RebuttalTrainer : Screen("rebuttal_trainer?sessionId={sessionId}") {
        fun createRoute(sessionId: String) = "rebuttal_trainer?sessionId=$sessionId"
        fun createRoute() = "rebuttal_trainer"
    }
    data object FallacyDetector : Screen("fallacy_detector")
    data object FaceToFace : Screen("facetoface/{sessionId}") {
        fun createRoute(sessionId: String) = "facetoface/$sessionId"
    }
    data object Donation : Screen("donation")
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(tween(300)) +
                slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 400f)) { it / 4 }
        },
        exitTransition = {
            fadeOut(tween(200)) +
                slideOutHorizontally(tween(250)) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(250)) +
                slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 400f)) { -it / 4 }
        },
        popExitTransition = {
            fadeOut(tween(200)) +
                slideOutHorizontally(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 4 }
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNewDebate = { navController.navigate(Screen.TopicSelection.route) },
                onHistory = { navController.navigate(Screen.History.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onDonation = { navController.navigate(Screen.Donation.route) },
                onArgumentMap = {
                    navController.navigate(Screen.TopicSelection.route + "?for=argument_map")
                },
                onRebuttalTrainer = { navController.navigate(Screen.RebuttalTrainer.route) },
                onFallacyDetector = { navController.navigate(Screen.FallacyDetector.route) },
                onFaceToFace = { navController.navigate(Screen.TopicSelection.route) },
                onTools = { navController.navigate(Screen.Tools.route) }
            )
        }

        composable(Screen.TopicSelection.route) {
            TopicSelectionScreen(
                onTopicSelected = { topicId ->
                    navController.navigate(Screen.DebateSetup.createRoute(topicId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DebateSetup.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: return@composable
            DebateSetupScreen(
                topicId = topicId,
                onStartDebate = { sessionId ->
                    navController.navigate(Screen.Debate.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onStartFaceToFace = { sessionId ->
                    navController.navigate(Screen.FaceToFace.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Debate.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            DebateScreen(
                sessionId = sessionId,
                onViewResult = { navController.navigate(Screen.DebateResult.createRoute(sessionId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DebateResult.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            DebateResultScreen(
                sessionId = sessionId,
                onBackToHome = {
                    navController.popBackStack(Screen.Home.route, false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            DebateHistoryScreen(
                onDebateSelected = { sessionId ->
                    navController.navigate(Screen.Debate.createRoute(sessionId))
                },
                onRebuttalSelected = { sessionId ->
                    navController.navigate(Screen.RebuttalTrainer.createRoute(sessionId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tools.route) {
            ToolsScreen(
                onFallacyDetector = { navController.navigate(Screen.FallacyDetector.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onProviderSelected = { providerName ->
                    navController.navigate(Screen.ProviderConfig.createRoute(providerName))
                },
                onBack = { navController.popBackStack() },
                onDonation = { navController.navigate(Screen.Donation.route) }
            )
        }

        composable(
            route = Screen.ProviderConfig.route,
            arguments = listOf(navArgument("providerName") { type = NavType.StringType })
        ) { backStackEntry ->
            val providerName = backStackEntry.arguments?.getString("providerName") ?: return@composable
            ProviderConfigScreen(
                providerName = providerName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ArgumentMap.route,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId") ?: return@composable
            ArgumentMapScreen(
                topicId = topicId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RebuttalTrainer.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            RebuttalTrainerScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FallacyDetector.route) {
            FallacyDetectorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FaceToFace.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            FaceToFaceScreen(
                sessionId = sessionId,
                onViewResult = { sId ->
                    navController.navigate(Screen.DebateResult.createRoute(sId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Donation.route) {
            DonationScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
