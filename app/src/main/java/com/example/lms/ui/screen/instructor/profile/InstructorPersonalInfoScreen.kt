package com.example.lms.ui.screen.instructor.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lms.data.model.Instructor
import com.example.lms.ui.component.TopBar
import com.example.lms.util.InstructorPersonalInfoEvent
import com.example.lms.viewmodel.InstructorPersonalInfoViewModel

@Composable
fun InstructorPersonalInfoRoute(
    instructorId: String,
    viewModel: InstructorPersonalInfoViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(instructorId) {
        viewModel.init(instructorId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is InstructorPersonalInfoEvent.ShowError) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    InstructorPersonalInfoScreen(
        isLoading = uiState.isLoading,
        instructor = uiState.instructor,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick
    )
}

@Composable
private fun InstructorPersonalInfoScreen(
    isLoading: Boolean,
    instructor: Instructor?,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "Thông tin cá nhân",
                onBackClick = onBackClick
            )
        },
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4B5CC4))
            }
            return@Scaffold
        }

        if (instructor == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có thông tin cá nhân",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = "Chuyên môn",
                value = instructor.expertise.ifBlank { "Chưa cập nhật" }
            )
            InfoCard(
                title = "Số năm kinh nghiệm",
                value = if (instructor.experienceYears > 0) {
                    "${instructor.experienceYears} năm"
                } else {
                    "Chưa cập nhật"
                }
            )
            InfoCard(
                title = "Trình độ/Bằng cấp",
                value = instructor.qualification.ifBlank { "Chưa cập nhật" }
            )
            InfoCard(
                title = "Tài khoản ngân hàng",
                value = instructor.bankAccount.ifBlank { "Chưa cập nhật" }
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(14.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = Color(0xFF1E293B),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

