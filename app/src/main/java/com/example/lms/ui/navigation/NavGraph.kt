package com.example.lms.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.lms.data.model.UserRole
import com.example.lms.ui.component.InstructorBottomBar
import com.example.lms.ui.component.StudentBottomBar
import com.example.lms.ui.screen.auth.CheckEmailScreen
import com.example.lms.ui.screen.auth.ForgotPasswordScreen
import com.example.lms.ui.screen.auth.LoginScreen
import com.example.lms.ui.screen.auth.RegisterScreen
import com.example.lms.ui.screen.instructor.InstructorHomeScreen
import com.example.lms.ui.screen.instructor.course.CourseFormScreen
import com.example.lms.ui.screen.instructor.course.MyCoursesScreen
import com.example.lms.ui.screen.instructor.curriculum.CurriculumScreen
import com.example.lms.ui.screen.instructor.curriculum.LessonFormScreen
import com.example.lms.ui.screen.instructor.curriculum.QuizFormScreen
import com.example.lms.ui.screen.instructor.profile.InstructorProfileScreen
import com.example.lms.ui.screen.student.StudentHomeScreen
import com.example.lms.viewmodel.AuthViewModel
import com.example.lms.viewmodel.CourseViewModel
import com.example.lms.viewmodel.CurriculumViewModel
import com.example.lms.viewmodel.LessonViewModel
import com.example.lms.viewmodel.QuizViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel()
    val courseViewModel: CourseViewModel = viewModel()
    val curriculumViewModel: CurriculumViewModel = viewModel()
    val lessonViewModel: LessonViewModel = viewModel()
    val quizViewModel: QuizViewModel = viewModel()

    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val user = authUiState.currentUser


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val instructorMainRoutes = listOf(Routes.HOME, Routes.MY_COURSES, "stats", Routes.PROFILE)
    val studentMainRoutes = listOf(Routes.HOME, Routes.MY_LEARNING, Routes.NOTIFICATIONS, Routes.PROFILE)
    
    val showBottomNav = if (user?.role == UserRole.INSTRUCTOR) {
        currentDestination?.route in instructorMainRoutes
    } else {
        currentDestination?.route in studentMainRoutes
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav && user != null) {
                if (user.role == UserRole.INSTRUCTOR) {
                    InstructorBottomBar(
                        navController = navController,
                        currentDestination = currentDestination
                    )
                } else {
                    StudentBottomBar(
                        navController = navController,
                        currentDestination = currentDestination
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authViewModel.isUserLoggedIn()) Routes.HOME else Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth Routes
            composable(Routes.LOGIN) { LoginScreen(navController, authViewModel) }
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

            composable(Routes.HOME) {
                if (authUiState.isLoading || user == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (user.role == UserRole.INSTRUCTOR) {
                        InstructorHomeScreen(navController, authViewModel)
                    } else {
                        StudentHomeScreen(navController, authViewModel)
                    }
                }
            }

            composable(Routes.PROFILE) {
                InstructorProfileScreen(
                    authViewModel = authViewModel,
                    onLogoutClick = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Student Flow
            composable(Routes.MY_LEARNING) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Màn hình khóa học của tôi")
                }
            }
            composable(Routes.NOTIFICATIONS) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Màn hình thông báo")
                }
            }
            composable(Routes.SEARCH) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Màn hình tìm kiếm")
                }
            }

            // Instructor Flow
            composable(Routes.MY_COURSES) {
                MyCoursesScreen(
                    instructorId = authViewModel.getCurrentUserId(),
                    viewModel = courseViewModel,
                    onNavigateToAddCourse = {
                        courseViewModel.onCourseSelected(null)
                        navController.navigate(Routes.COURSE_FORM)
                    },
                    onNavigateToEditCourse = { course ->
                        courseViewModel.onCourseSelected(course)
                        navController.navigate(Routes.COURSE_FORM)
                    },
                    onManageContent = { course ->
                        courseViewModel.onCourseSelected(course)
                        curriculumViewModel.setCourseId(course.id)
                        navController.navigate(Routes.CURRICULUM)
                    }
                )
            }

            composable(Routes.COURSE_FORM) {
                CourseFormScreen(
                    instructorId = authViewModel.getCurrentUserId(),
                    authViewModel = authViewModel,
                    viewModel = courseViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.CURRICULUM) {
                CurriculumScreen(
                    courseViewModel = courseViewModel,
                    viewModel = curriculumViewModel,
                    onBackClick = { navController.popBackStack() },
                    onAddLesson = { courseId ->
                        lessonViewModel.initWith(null, courseId)
                        navController.navigate(Routes.LESSON_FORM)
                    },
                    onAddQuiz = { courseId ->
                        quizViewModel.initWith(null, courseId)
                        navController.navigate(Routes.QUIZ_FORM)
                    },
                    onEditLesson = { lessonItem, courseId ->
                        lessonViewModel.initWith(lessonItem.lesson, courseId)
                        navController.navigate(Routes.LESSON_FORM)
                    },
                    onEditQuiz = { quizItem, courseId ->
                        quizViewModel.initWith(quizItem.quiz, courseId)
                        navController.navigate(Routes.QUIZ_FORM)
                    }
                )
            }

            composable(Routes.LESSON_FORM) {
                LessonFormScreen(
                    viewModel = lessonViewModel,
                    onBackClick = {
                        navController.popBackStack()
                        curriculumViewModel.loadCurriculum()
                    }
                )
            }

            composable(Routes.QUIZ_FORM) {
                QuizFormScreen(
                    viewModel = quizViewModel,
                    onBackClick = {
                        navController.popBackStack()
                        curriculumViewModel.loadCurriculum()
                    }
                )
            }
        }
    }
}
