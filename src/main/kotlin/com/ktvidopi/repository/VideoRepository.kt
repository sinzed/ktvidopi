package com.ktvidopi.repository

import com.ktvidopi.domain.ProcessingStatus
import com.ktvidopi.domain.Video
import com.ktvidopi.infrastructure.database.DatabaseFactory.dbQuery
import com.ktvidopi.infrastructure.database.Videos
import com.ktvidopi.infrastructure.database.toVideo
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

interface VideoRepository {
    suspend fun create(video: Video): Video
    suspend fun findById(id: UUID): Video?
    suspend fun findAll(): List<Video>
    suspend fun updateStatus(id: UUID, status: ProcessingStatus, errorMessage: String? = null): Boolean
    suspend fun updateProcessedAt(id: UUID, processedAt: Instant): Boolean
}

class VideoRepositoryImpl : VideoRepository {
    override suspend fun create(video: Video): Video = dbQuery {
        Videos.insert {
            it[id] = video.id
            it[title] = video.title
            it[fileName] = video.fileName
            it[fileSize] = video.fileSize
            it[status] = video.status
            it[createdAt] = video.createdAt
            it[updatedAt] = video.updatedAt
            it[processedAt] = video.processedAt
            it[errorMessage] = video.errorMessage
        }
        video
    }
    
    override suspend fun findById(id: UUID): Video? = dbQuery {
        Videos.select { Videos.id eq id }
            .map { it.toVideo() }
            .singleOrNull()
    }
    
    override suspend fun findAll(): List<Video> = dbQuery {
        Videos.selectAll()
            .map { it.toVideo() }
    }
    
    override suspend fun updateStatus(id: UUID, status: ProcessingStatus, errorMessage: String?): Boolean = dbQuery {
        Videos.update({ Videos.id eq id }) {
            it[Videos.status] = status
            it[Videos.updatedAt] = Instant.now()
            it[Videos.errorMessage] = errorMessage
        } > 0
    }
    
    override suspend fun updateProcessedAt(id: UUID, processedAt: Instant): Boolean = dbQuery {
        Videos.update({ Videos.id eq id }) {
            it[Videos.processedAt] = processedAt
            it[Videos.updatedAt] = Instant.now()
        } > 0
    }
}
