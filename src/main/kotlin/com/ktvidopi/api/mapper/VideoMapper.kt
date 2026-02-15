package com.ktvidopi.api.mapper

import com.ktvidopi.api.dto.VideoResponse
import com.ktvidopi.domain.Video

object VideoMapper {
    fun Video.toDto(): VideoResponse = VideoResponse(
        id = this.id,
        title = this.title,
        fileName = this.fileName,
        fileSize = this.fileSize,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        processedAt = this.processedAt,
        errorMessage = this.errorMessage
    )
}
