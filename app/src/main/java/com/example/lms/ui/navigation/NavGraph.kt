package com.example.lms.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.lms.ui.screen.instructor.course.CourseFormScreen
import com.example.lms.ui.screen.instructor.course.MyCoursesScreen
import com.example.lms.ui.screen.instructor.curriculum.CurriculumScreen
import com.example.lms.ui.screen.instructor.curriculum.LessonFormScreen
import com.example.lms.viewmodel.AuthViewModel
import com.example.lms.viewmodel.CourseViewModel
import com.example.lms.viewmodel.CurriculumViewModel
import com.example.lms.viewmodel.LessonViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    val currentUserName = auth.currentUser?.displayName ?: "Instructor"

    // Scoped ViewModels
    val courseViewModel: CourseViewModel = viewModel()
    val curriculumViewModel: CurriculumViewModel = viewModel()
    val lessonViewModel: LessonViewModel = viewModel()

    NavHost(
        navController = navController, 
        startDestination = if (currentUserId.isNotEmpty()) Routes.MY_COURSES else Routes.LOGIN
    ) {

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

        // --- Instructor Flow ---

        // 1. Dashboard: Danh sách khóa học
        composable(Routes.MY_COURSES) {
            MyCoursesScreen(
                instructorId = currentUserId,
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

        // 2. Form: Thêm/Sửa khóa học
        composable(Routes.COURSE_FORM) {
            CourseFormScreen(
                instructorId = currentUserId,
                instructorName = currentUserName,
                viewModel = courseViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 3. Curriculum: Quản lý nội dung (Lessons & Quizzes)
        composable(Routes.CURRICULUM) {
            val courseUiState by courseViewModel.uiState.collectAsStateWithLifecycle()
            val currentCourse = courseUiState.currentCourse
            val courseId = currentCourse?.id ?: ""
            
            CurriculumScreen(
                courseTitle = currentCourse?.title ?: "Nội dung khóa học",
                courseId = courseId,
                viewModel = curriculumViewModel,
                onBackClick = { navController.popBackStack() },
                onAddLesson = {
                    lessonViewModel.initWith(null, courseId)
                    navController.navigate(Routes.LESSON_FORM)
                },
                onAddQuiz = {
                    navController.navigate(Routes.QUIZ_FORM)
                },
                onEditLesson = { lessonItem ->
                    lessonViewModel.initWith(lessonItem.lesson, courseId)
                    navController.navigate(Routes.LESSON_FORM)
                },
                onEditQuiz = { quizItem ->
                    // To be implemented when QuizFormScreen is ready
                    navController.navigate(Routes.QUIZ_FORM)
                }
            )
        }

        // 4. Form: Bài học
        composable(Routes.LESSON_FORM) {
            LessonFormScreen(
                viewModel = lessonViewModel,
                onBackClick = { 
                    navController.popBackStack()
                    curriculumViewModel.loadCurriculum()
                }
            )
        }

        // 5. Form: Bài kiểm tra
        composable(Routes.QUIZ_FORM) {
            // Placeholder cho QuizFormScreen
        }
    }
}
