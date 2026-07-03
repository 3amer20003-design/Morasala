package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.ui.auth.*
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.chat.ChatViewModelFactory
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.home.HomeViewModelFactory
import com.example.ui.profile.ProfileViewModel
import com.example.ui.profile.ProfileViewModelFactory
import com.example.ui.profile.SettingsScreen

import com.example.ui.profile.UserProfileScreen
import com.example.ui.profile.UserProfileViewModel
import com.example.ui.profile.UserProfileViewModelFactory

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authRepository = AuthRepository()
    val chatRepository = ChatRepository()
    val currentUserId by authRepository.currentUserIdFlow.collectAsState(initial = null)
    
    val startDestination = Screen.Splash

    NavHost(navController = navController, startDestination = startDestination) {
        
        composable<Screen.Splash> {
            com.example.ui.splash.SplashScreen(
                onSplashComplete = {
                    val nextDestination = if (currentUserId != null) Screen.Home else Screen.Login
                    navController.navigate(nextDestination) {
                        popUpTo(Screen.Splash) { inclusive = true }
                    }
                }
            )
        }
        
        composable<Screen.Login> {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword) }
            )
        }

        composable<Screen.Register> {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                        popUpTo(Screen.Register) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ForgotPassword> {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Home> {
            currentUserId?.let { userId ->
                val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(userId, chatRepository))
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToChat = { peerId ->
                        navController.navigate(Screen.Chat(peerId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings)
                    }
                )
            }
        }

        composable<Screen.Chat> { backStackEntry ->
            val chatScreenData = backStackEntry.toRoute<Screen.Chat>()
            currentUserId?.let { userId ->
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(userId, chatScreenData.peerId, chatRepository)
                )
                ChatScreen(
                    viewModel = chatViewModel,
                    currentUserId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { peerId ->
                        navController.navigate(Screen.UserProfile(peerId))
                    }
                )
            }
        }

        composable<Screen.UserProfile> { backStackEntry ->
            val userProfileData = backStackEntry.toRoute<Screen.UserProfile>()
            val userProfileViewModel: UserProfileViewModel = viewModel(
                factory = UserProfileViewModelFactory(userProfileData.userId, chatRepository)
            )
            UserProfileScreen(
                viewModel = userProfileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Settings> {
            currentUserId?.let { userId ->
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(userId, authRepository, chatRepository)
                )
                SettingsScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login) {
                            popUpTo(Screen.Home) { inclusive = true }
                        }
                    },
                    onChangePassword = {
                        navController.navigate(Screen.ForgotPassword)
                    }
                )
            }
        }
    }
}
