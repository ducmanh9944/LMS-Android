package com.example.lms.ui.screen.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.lms.data.model.Course
import com.example.lms.ui.component.ProgressBar
import com.example.lms.ui.navigation.Routes
import com.example.lms.util.formatPrice
import com.example.lms.viewmodel.AuthViewModel
import com.example.lms.viewmodel.CourseViewModel

private val Indigo = Color(0xFF4B5CC4)
private val StarYellow = Color(0xFFFFC107)
private val SurfaceGray = Color(0xFFF5F6FA)
private val TextPrimary = Color(0xFF1A1D2E)
private val TextSecondary = Color(0xFF6B7280)
private val CardWhite = Color(0xFFFFFFFF)

@Composable
fun StudentHomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    courseViewModel: CourseViewModel = viewModel()
) {
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val courseUiState by courseViewModel.uiState.collectAsStateWithLifecycle()
    val user = authUiState.currentUser

    LaunchedEffect(Unit) {
        courseViewModel.getSuggestedCourses()
    }

    val continueCourse = remember {
        Course(id = "1", title = "Advanced Machine Learning", duration = "Bài học 12: Neural Networks")
    }
    val myCourses = remember {
        listOf(
            Course(id = "1", title = "UI/UX Design", instructorName = "8/20 Bài học"),
            Course(id = "2", title = "Full-stack React", instructorName = "4/15 Bài học")
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SurfaceGray),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header ──
        item {
            StudentHeader(
                userName = user?.fullName ?: "Học viên",
                avatarUrl = user?.avatarUrl ?: "",
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onCartClick = { navController.navigate(Routes.CART) }
            )
        }

        // ── Tiếp tục học ──
        item {
            SectionTitle("Tiếp tục học", Modifier.padding(16.dp, 8.dp))
            ContinueLearningCard(
                course = continueCourse,
                onContinueClick = { /* Navigate to player */ },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Khóa học đang học ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Các khóa học đang học")
                Text(
                    text = "Xem tất cả", color = Indigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { navController.navigate(Routes.MY_LEARNING) }
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myCourses) { MyCourseCard(it) }
            }
        }

        // ── Gợi ý cho bạn  ──
        item { SectionTitle("Gợi ý cho bạn", Modifier.padding(16.dp, 12.dp)) }
        
        if (courseUiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Indigo, modifier = Modifier.size(30.dp))
                }
            }
        } else {
            items(courseUiState.suggestedCourses) { course ->
                SuggestedCourseCard(
                    course = course,
                    onClick = { },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StudentHeader(userName: String, avatarUrl: String, onSearchClick: () -> Unit, onCartClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CardWhite).padding(16.dp, 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Indigo.copy(0.15f)), contentAlignment = Alignment.Center) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else Icon(Icons.Default.Person, null, tint = Indigo)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((-4).dp)) {
            Text("Xin chào", fontSize = 12.sp, color = TextSecondary)
            Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        IconButton(onSearchClick) { Icon(Icons.Default.Search, null, tint = TextPrimary) }
        IconButton(onCartClick) { Icon(Icons.Default.ShoppingCart, null, tint = TextPrimary) }
    }
}

@Composable
private fun ContinueLearningCard(course: Course, onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.linearGradient(listOf(Color(0xFF2C3E8C), Color(0xFF6B79D4), Color(0xFFE8A838))))) {
                if (course.thumbnailUrl.isNotBlank()) AsyncImage(course.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(16.dp)) {
                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(course.duration, fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("75% Hoàn thành", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Indigo)
                    Text("12/16 Bài học", fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(progress = 0.75f)
                Spacer(Modifier.height(16.dp))
                Button(onContinueClick, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(Indigo)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Tiếp tục", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MyCourseCard(course: Course) {
    Card(Modifier.width(155.dp), RoundedCornerShape(12.dp), CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(100.dp).background(Brush.linearGradient(listOf(Color(0xFFE8926A), Color(0xFFF5C49A)))))
            Column(Modifier.padding(10.dp)) {
                Text(course.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Text(course.instructorName, fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
//                LinearProgressIndicator(progress = { 0.4f }, Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = Indigo, trackColor = Indigo.copy(0.15f))
                ProgressBar(progress = 0.4f, height = 5.dp)
            }
        }
    }
}

@Composable
private fun SuggestedCourseCard(
    course: Course,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceGray)
            ) {
                if (course.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = course.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)

                ) {
                    Text(
                        text = course.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Text(
                        text = course.instructorName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = StarYellow,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text(
                            text = "${course.rating}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = " (${course.reviewCount} đánh giá)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formatPrice(course.price),
                        color = Indigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = modifier)
}
