package com.ktvidopi.api.dto

import com.ktvidopi.domain.ProcessingStatus
import java.time.Instant
import java.util.UUID

data class CreateVideoRequest(
    val title: String,
    val fileName: String,
    val fileSize: Long
)

data class VideoResponse(
    val id: UUID,
    val title: String,
    val fileName: String,
    val fileSize: Long,
    val status: ProcessingStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val processedAt: Instant?,
    val errorMessage: String?
)
