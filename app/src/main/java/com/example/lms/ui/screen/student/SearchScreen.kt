package com.example.lms.ui.screen.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

// ── Màu sắc (dùng chung với StudentHomeScreen) ────────────────────────────────
private val Indigo        = Color(0xFF4B5CC4)
private val StarYellow    = Color(0xFFFFC107)
private val SurfaceGray   = Color(0xFFF5F6FA)
private val TextPrimary   = Color(0xFF1A1D2E)
private val TextSecondary = Color(0xFF6B7280)
private val CardWhite     = Color(0xFFFFFFFF)
private val ChipBorder    = Color(0xFFE0E0E0)

// ── Data class ────────────────────────────────────────────────────────────────
data class CourseSearchResult(
    val id: String,
    val title: String,
    val category: String,         // "Design", "Tech", "Marketing"…
    val rating: Float,
    val reviewCount: String,      // "1.2k"
    val durationHours: Int,
    val level: String,            // "Beginner", "Intermediate", "Advanced"
    val price: Long,              // VND, 0 = miễn phí
    val thumbnailUrl: String
)

// ── Sample data (preview / placeholder) ───────────────────────────────────────
private val sampleResults = listOf(
    CourseSearchResult("1", "Advanced UI/UX Design Masterclass", "Design", 4.8f, "1.2k", 12, "Beginner", 499_000, ""),
    CourseSearchResult("2", "Cybersecurity Essentials", "Tech", 4.8f, "1.2k", 12, "Beginner", 499_000, ""),
    CourseSearchResult("3", "UI Design Fundamentals", "Design", 4.8f, "1.2k", 12, "Beginner", 499_000, ""),
    CourseSearchResult("4", "Full-stack React", "Tech", 4.8f, "1.2k", 12, "Beginner", 499_000, ""),
    CourseSearchResult("5", "Digital Marketing Strategy", "Marketing", 4.9f, "2.1k", 8, "Beginner", 399_000, ""),
)

private val allCategories = listOf("Tất cả", "Design", "Tech", "Marketing", "Business", "Data")

// ── Màn hình tìm kiếm ─────────────────────────────────────────────────────────
@Composable
fun SearchScreen(
    navController: NavController,
    // Thay bằng state từ ViewModel thật
    initialQuery: String = "",
    allCourses: List<CourseSearchResult> = sampleResults,
    categories: List<String> = allCategories,
    onCourseClick: (String) -> Unit = { id -> navController.navigate("course_detail/$id") }
) {
    var query by remember { mutableStateOf(initialQuery) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Filter logic – thay bằng Flow từ ViewModel
    val filteredCourses = remember(query, selectedCategory, allCourses) {
        allCourses.filter { course ->
            val matchesQuery = query.isBlank() ||
                    course.title.contains(query, ignoreCase = true) ||
                    course.category.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategory == "Tất cả" ||
                    course.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    // Auto-focus khi vào màn hình
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
    ) {
        // ── Top bar ──
        SearchTopBar(
            onBackClick = { navController.popBackStack() }
        )

        // ── Search field ──
        SearchField(
            query = query,
            onQueryChange = { query = it },
            onSearch = { focusManager.clearFocus() },
            focusRequester = focusRequester,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ── Category chips ──
        CategoryChipRow(
            categories = categories,
            selected = selectedCategory,
            onSelect = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Results ──
        if (filteredCourses.isEmpty()) {
            EmptySearchResult(query = query)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCourses, key = { it.id }) { course ->
                    SearchCourseCard(
                        course = course,
                        onClick = { onCourseClick(course.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
private fun SearchTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = TextPrimary
            )
        }
        Text(
            text = "Tìm kiếm khóa học",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

// ── Search field ──────────────────────────────────────────────────────────────
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = {
            Text(
                text = "Tìm kiếm",
                color = TextSecondary,
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFEEEFF5),
            unfocusedContainerColor = Color(0xFFEEEFF5),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Indigo,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

// ── Category chips ────────────────────────────────────────────────────────────
@Composable
private fun CategoryChipRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Indigo else CardWhite)
                    .then(
                        if (!isSelected) Modifier.clip(CircleShape) else Modifier
                    )
                    .clickable { onSelect(category) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
            if (!isSelected) {
                // border effect via outer box
            }
        }
    }
}

// ── Course card ───────────────────────────────────────────────────────────────
@Composable
private fun SearchCourseCard(
    course: CourseSearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Left: text info ──
            Column(modifier = Modifier.weight(1f)) {
                // Category label
                Text(
                    text = course.category,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Indigo
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Title
                Text(
                    text = course.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = StarYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${course.rating} (${course.reviewCount} đánh giá)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Duration & level
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⏱", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${course.durationHours} tiếng • ${course.level}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Indigo.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (course.price == 0L) "Miễn phí"
                        else "%,dđ".format(course.price).replace(',', '.'),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Indigo
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Right: thumbnail ──
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Indigo.copy(alpha = 0.12f))
            ) {
                if (course.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = course.thumbnailUrl,
                        contentDescription = course.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder gradient theo category
                    val gradientColors = when (course.category) {
                        "Design"    -> listOf(Color(0xFFFFD4A8), Color(0xFFF5A76C))
                        "Tech"      -> listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                        "Marketing" -> listOf(Color(0xFF89CFF0), Color(0xFF5BA4CF))
                        "Business"  -> listOf(Color(0xFFB8E8C8), Color(0xFF6DBF92))
                        else        -> listOf(Color(0xFFCDC5F5), Color(0xFF9D8DF1))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(gradientColors))
                    )
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptySearchResult(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔍", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (query.isBlank()) "Nhập từ khóa để tìm kiếm"
            else "Không tìm thấy kết quả cho\n\"$query\"",
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(
    name = "Search Screen – Full",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_6
)
@Composable
private fun SearchScreenPreview() {
    MaterialTheme {
        SearchScreen(navController = rememberNavController())
    }
}

@Preview(
    name = "Search Screen – Filtered Tech",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_6
)
@Composable
private fun SearchScreenTechPreview() {
    MaterialTheme {
        var selected by remember { mutableStateOf("Tech") }
        SearchScreen(
            navController = rememberNavController(),
            allCourses = sampleResults.filter { it.category == "Tech" }
        )
    }
}

@Preview(
    name = "Search Screen – Empty",
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_6
)
@Composable
private fun SearchScreenEmptyPreview() {
    MaterialTheme {
        SearchScreen(
            navController = rememberNavController(),
            initialQuery = "xyzabc123",
            allCourses = emptyList()
        )
    }
}

@Preview(
    name = "Course Search Card",
    showBackground = true,
    backgroundColor = 0xFFF5F6FA
)
@Composable
private fun SearchCourseCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchCourseCard(
                course = CourseSearchResult(
                    "1", "Advanced UI/UX Design Masterclass",
                    "Design", 4.8f, "1.2k", 12, "Beginner", 499_000, ""
                ),
                onClick = {}
            )
            SearchCourseCard(
                course = CourseSearchResult(
                    "2", "Cybersecurity Essentials",
                    "Tech", 4.8f, "1.2k", 12, "Beginner", 499_000, ""
                ),
                onClick = {}
            )
            SearchCourseCard(
                course = CourseSearchResult(
                    "3", "Khóa học miễn phí Git & GitHub",
                    "Tech", 4.5f, "980", 6, "Beginner", 0, ""
                ),
                onClick = {}
            )
        }
    }
}

@Preview(
    name = "Category Chip Row",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun CategoryChipRowPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            CategoryChipRow(
                categories = allCategories,
                selected = "Design",
                onSelect = {}
            )
        }
    }
}