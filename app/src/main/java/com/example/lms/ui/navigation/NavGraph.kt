package com.example.lms.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.lms.ui.screen.auth.CheckEmailScreen
import com.example.lms.ui.screen.auth.ForgotPasswordScreen
import com.example.lms.ui.screen.auth.LoginScreen
import com.example.lms.ui.screen.auth.RegisterScreen
import com.example.lms.ui.screen.home.HomeScreen
import com.example.lms.viewmodel.AuthViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) { LoginScreen(navController) }
        composable(Routes.REGISTER) { RegisterScreen(navController) }

        navigation(route = "forgot_pwd_flow", startDestination = Routes.FORGOT_PASSWORD) {
            composable(Routes.FORGOT_PASSWORD) { entry ->
                val vm: AuthViewModel = viewModel(remember(entry) { navController.getBackStackEntry("forgot_pwd_flow") })
                ForgotPasswordScreen(navController, vm)
            }
            composable(Routes.CHECK_EMAIL) { entry ->
                val vm: AuthViewModel = viewModel(remember(entry) { navController.getBackStackEntry("forgot_pwd_flow") })
                CheckEmailScreen(navController, vm)
            }
        }

        composable(Routes.HOME) { HomeScreen(navController) }
    }
}