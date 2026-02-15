package com.ktvidopi.domain

import java.util.UUID

data class Video(
    val id: UUID,
    val title: String,
    val fileName: String,
    val fileSize: Long,
    val status: ProcessingStatus,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant,
    val processedAt: java.time.Instant? = null,
    val errorMessage: String? = null
)
