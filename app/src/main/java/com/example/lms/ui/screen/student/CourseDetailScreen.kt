package com.example.lms.ui.screen.student

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.lms.data.model.*
import com.example.lms.ui.component.TopBar
import com.example.lms.util.CourseDetailEvent
import com.example.lms.util.CourseDetailUiState
import com.example.lms.util.formatPrice
import com.example.lms.viewmodel.CourseDetailViewModel


private val Indigo        = Color(0xFF4B5CC4)
private val StarYellow    = Color(0xFFFFC107)
private val SurfaceGray   = Color(0xFFF5F6FA)
private val TextPrimary   = Color(0xFF1A1D2E)
private val TextSecondary = Color(0xFF6B7280)
private val CardWhite     = Color(0xFFFFFFFF)

@Composable
fun CourseDetailScreen(
    navController: NavController,
    viewModel: CourseDetailViewModel,
    courseId: String,
    userId: String,
    onLessonClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Chỉ load toàn bộ khi ID khóa học thay đổi
    LaunchedEffect(courseId, userId) {
        viewModel.loadCourseDetail(courseId, userId)
    }

    // Khi quay lại (Resume), chỉ refresh tiến độ bài học cuối
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProgressOnly(courseId, userId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CourseDetailEvent.EnrollSuccess -> {
                    Toast.makeText(context, "Đăng ký khóa học thành công!", Toast.LENGTH_SHORT).show()
                }
                is CourseDetailEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Chi tiết khóa học",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = SurfaceGray,
        bottomBar = {
            if (!uiState.isLoading && uiState.course != null) {
                CourseDetailBottomBar(
                    course = uiState.course!!,
                    isEnrolled = uiState.isEnrolled,
                    isEnrolling = uiState.isEnrolling,
                    onEnrollClick = {
                        val lastLessonId = uiState.progress?.lastLessonId
                        val firstItemId = uiState.curriculum.firstOrNull()?.id
                        val targetId = if (!lastLessonId.isNullOrEmpty()) lastLessonId else firstItemId
                        
                        if (targetId != null) {
                            onLessonClick(targetId)
                        } else {
                            Toast.makeText(context, "Khóa học chưa có nội dung", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBuyNowClick = {
                        if (!uiState.isEnrolled) {
                            viewModel.enrollCourse(userId, courseId)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> LoadingState(PaddingValues(0.dp))
                uiState.course == null -> ErrorState(PaddingValues(0.dp))
                else -> CourseDetailContent(
                    paddingValues = PaddingValues(0.dp),
                    course = uiState.course!!,
                    uiState = uiState,
                    onLessonClick = onLessonClick
                )
            }
        }
    }
}

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Indigo)
    }
}

@Composable
private fun ErrorState(paddingValues: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Text(text = "Không tìm thấy khóa học", color = TextSecondary, fontSize = 15.sp)
    }
}

@Composable
private fun CourseDetailContent(
    paddingValues: PaddingValues,
    course: Course,
    uiState: CourseDetailUiState,
    onLessonClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
    ) {
        item { VideoPlayer(videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4") }
        item {
            val categoryName = uiState.categories.find { it.id == course.categoryId }?.name ?: "Chưa phân loại"
            CourseInfoSection(course = course, categoryName = categoryName)
        }
        item {
            InstructorSection(
                instructorName = course.instructorName,
                avatarUrl = uiState.instructor?.avatarUrl,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        item {
            CourseDetailTabs(
                course = course,
                curriculum = uiState.curriculum,
                isEnrolled = uiState.isEnrolled,
                onLessonClick = onLessonClick
            )
        }
    }
}

@Composable
private fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var shouldHide by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    DisposableEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
        onDispose {}
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_STOP -> {
                    shouldHide = true
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            shouldHide = true
            lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.Black)
            .graphicsLayer {
                alpha = if (shouldHide) 0f else 1f
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                it.player = null
            }
        )
    }
}

@Composable
private fun CourseInfoSection(course: Course, categoryName: String) {
    Column(modifier = Modifier.fillMaxWidth().background(CardWhite).padding(16.dp)) {
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Indigo.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(text = categoryName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Indigo)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = course.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 28.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem(icon = Icons.Default.Star, text = "${course.rating} (${course.reviewCount} đánh giá)", iconColor = StarYellow)
            StatItem(icon = Icons.Default.People, text = "${course.enrollmentCount} học viên")
        }
        Spacer(modifier = Modifier.height(6.dp))
        StatItem(
            icon = Icons.Default.AccessTime,
            text = "${course.duration} • ${when(course.level) {
                CourseLevel.BEGINNER -> "Cơ bản"
                CourseLevel.INTERMEDIATE -> "Trung cấp"
                CourseLevel.ADVANCED -> "Nâng cao"
            }}"
        )
    }
}

@Composable
private fun StatItem(icon: ImageVector, text: String, iconColor: Color = TextSecondary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = text, fontSize = 13.sp, color = TextSecondary)
    }
}

@Composable
private fun InstructorSection(instructorName: String, avatarUrl: String?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().border( width = 1.dp, color = Color(0xFFE2E4ED), shape = RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(CardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Indigo.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                if (avatarUrl.isNullOrEmpty()) {
                    Text(text = instructorName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Indigo)
                } else {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                Text(text = instructorName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = "Giảng viên", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun CourseDetailTabs(course: Course, curriculum: List<CurriculumItem>, isEnrolled: Boolean, onLessonClick: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tổng quan", "Đánh giá")

    Column(modifier = Modifier.fillMaxWidth().background(CardWhite)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = CardWhite, contentColor = Indigo, indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Indigo)
        }) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index },
                    text = { Text(text = title, fontSize = 14.sp, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                    selectedContentColor = Indigo, unselectedContentColor = TextSecondary
                )
            }
        }
        when (selectedTab) {
            0 -> OverviewTab(course, curriculum, isEnrolled, onLessonClick)
            1 -> ReviewTab()
        }
    }
}

@Composable
private fun OverviewTab(course: Course, curriculum: List<CurriculumItem>, isEnrolled: Boolean, onLessonClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DescriptionSection(description = course.description)
        CurriculumSection(curriculum, course.lessonCount, course.duration, isEnrolled, onLessonClick)
    }
}

@Composable
private fun DescriptionSection(description: String) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = "Mô tả", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description, fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.animateContentSize())
        Row(modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (expanded) "Thu gọn" else "Xem thêm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Indigo)
            Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Indigo, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CurriculumSection(
    curriculum: List<CurriculumItem>,
    lessonCount: Int,
    duration: String,
    isEnrolled: Boolean,
    onLessonClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách bài học",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "$lessonCount bài học • $duration",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(width = 1.dp, color = Color(0xFFDBDBE6), shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                curriculum.forEachIndexed { index, item ->
                    when (item) {
                        is CurriculumItem.LessonItem -> CurriculumRow(
                            title = item.lesson.title,
                            subtitle = item.lesson.duration,
                            isEnrolled = isEnrolled,
                            isQuiz = false,
                            onClick = { if (isEnrolled) onLessonClick(item.lesson.id) }
                        )
                        is CurriculumItem.QuizItem -> CurriculumRow(
                            title = item.quiz.title,
                            subtitle = "${item.quiz.durationMinutes} phút • ${item.quiz.questions.size} câu hỏi",
                            isEnrolled = isEnrolled,
                            isQuiz = true,
                            onClick = { if (isEnrolled) onLessonClick(item.quiz.id) }
                        )
                    }
                    if (index < curriculum.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color(0xFFDBDBE6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurriculumRow(
    title: String,
    subtitle: String,
    isEnrolled: Boolean,
    isQuiz: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnrolled, onClick = onClick)
            .padding(start = 16.dp, end = 26.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !isEnrolled -> Color(0xFFF1F2F8)
                        isQuiz      -> Color(0xFF10B981).copy(alpha = 0.12f)
                        else        -> Indigo.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    !isEnrolled -> Icons.Default.Lock
                    isQuiz      -> Icons.Outlined.Quiz
                    else        -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = when {
                    !isEnrolled -> Color(0xFFADB5BD)
                    isQuiz      -> Color(0xFF10B981)
                    else        -> Indigo
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((-4).dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnrolled) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        if (isQuiz) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Quiz",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun ReviewTab() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = "Chưa có đánh giá", fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun CourseDetailBottomBar(
    course: Course,
    isEnrolled: Boolean,
    isEnrolling: Boolean,
    onEnrollClick: () -> Unit,
    onBuyNowClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 0.dp, color = CardWhite) {
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E4ED))
        if (isEnrolled) {
            Button(
                onClick = onEnrollClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(Indigo)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Tiếp tục học", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val isFree = course.price == 0.0
                Text(text = if (isFree) "Miễn phí" else formatPrice(course.price), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isFree) {
                    Button(
                        onClick = onBuyNowClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(Indigo),
                        enabled = !isEnrolling
                    ) {
                        if (isEnrolling) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(text = "Đăng ký học", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Indigo),
                            enabled = !isEnrolling
                        ) {
                            Text(text = "Thêm vào giỏ", fontSize = 14.sp, color = Indigo, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onBuyNowClick,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(Indigo),
                            enabled = !isEnrolling
                        ) {
                            if (isEnrolling) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(text = "Mua ngay", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}