package com.ktvidopi.integration

import com.ktvidopi.domain.ProcessingStatus
import com.ktvidopi.infrastructure.database.DatabaseFactory
import com.ktvidopi.infrastructure.database.Videos
import com.ktvidopi.repository.VideoRepositoryImpl
import com.ktvidopi.service.VideoProcessingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID

/**
 * Integration tests using Testcontainers to spin up a real Postgres instance.
 * Demonstrates:
 * - Real database interactions
 * - Coroutine-based repository operations
 * - End-to-end service behavior
 */
@Testcontainers
class VideoIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("test")
            withUsername("test")
            withPassword("test")
        }
    }
    
    private lateinit var videoRepository: VideoRepositoryImpl
    private lateinit var videoService: VideoProcessingService
    
    @BeforeEach
    fun setup() {
        DatabaseFactory.init(
            databaseUrl = postgres.jdbcUrl,
            driverClassName = postgres.driverClassName,
            user = postgres.username,
            password = postgres.password
        )
        
        // Clear tables before each test
        transaction {
            Videos.deleteAll()
        }
        
        videoRepository = VideoRepositoryImpl()
        videoService = VideoProcessingService(videoRepository)
    }
    
    @Test
    fun `createVideo should persist video and trigger async processing`() = runTest {
        // Given
        val title = "Integration Test Video"
        val fileName = "test.mp4"
        val fileSize = 1024L
        
        // When
        val createdVideo = videoService.createVideo(title, fileName, fileSize)
        
        // Then - verify video was persisted
        val retrievedVideo = videoService.getVideo(createdVideo.id)
        assertThat(retrievedVideo).isNotNull
        assertThat(retrievedVideo?.title).isEqualTo(title)
        assertThat(retrievedVideo?.status).isEqualTo(ProcessingStatus.PENDING)
        
        // Wait for async processing to complete
        delay(7000) // Processing takes ~6 seconds
        
        // Verify status was updated
        val processedVideo = videoService.getVideo(createdVideo.id)
        assertThat(processedVideo?.status).isEqualTo(ProcessingStatus.COMPLETED)
        assertThat(processedVideo?.processedAt).isNotNull
    }
    
    @Test
    fun `getAllVideos should return all persisted videos`() = runTest {
        // Given
        val video1 = videoService.createVideo("Video 1", "video1.mp4", 1024L)
        val video2 = videoService.createVideo("Video 2", "video2.mp4", 2048L)
        
        // When
        val allVideos = videoService.getAllVideos()
        
        // Then
        assertThat(allVideos).hasSize(2)
        assertThat(allVideos.map { it.id }).containsExactlyInAnyOrder(video1.id, video2.id)
    }
    
    @Test
    fun `getVideo should return null for non-existent video`() = runTest {
        // Given
        val nonExistentId = UUID.randomUUID()
        
        // When
        val result = videoService.getVideo(nonExistentId)
        
        // Then
        assertThat(result).isNull()
    }
}
