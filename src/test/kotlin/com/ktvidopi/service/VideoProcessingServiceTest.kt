package com.ktvidopi.service

import com.ktvidopi.domain.ProcessingStatus
import com.ktvidopi.domain.Video
import com.ktvidopi.repository.VideoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class VideoProcessingServiceTest {
    private val videoRepository = mockk<VideoRepository>()
    private val service = VideoProcessingService(videoRepository)
    
    @Test
    fun `createVideo should create video and return it`() = runTest {
        // Given
        val video = Video(
            id = UUID.randomUUID(),
            title = "Test Video",
            fileName = "test.mp4",
            fileSize = 1024L,
            status = ProcessingStatus.PENDING,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { videoRepository.create(any()) } returns video
        
        // When
        val result = service.createVideo("Test Video", "test.mp4", 1024L)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result.title).isEqualTo("Test Video")
        assertThat(result.status).isEqualTo(ProcessingStatus.PENDING)
        coVerify(exactly = 1) { videoRepository.create(any()) }
    }
    
    @Test
    fun `getVideo should return video when exists`() = runTest {
        // Given
        val videoId = UUID.randomUUID()
        val video = Video(
            id = videoId,
            title = "Test Video",
            fileName = "test.mp4",
            fileSize = 1024L,
            status = ProcessingStatus.COMPLETED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { videoRepository.findById(videoId) } returns video
        
        // When
        val result = service.getVideo(videoId)
        
        // Then
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(videoId)
        coVerify(exactly = 1) { videoRepository.findById(videoId) }
    }
    
    @Test
    fun `getVideo should return null when not exists`() = runTest {
        // Given
        val videoId = UUID.randomUUID()
        coEvery { videoRepository.findById(videoId) } returns null
        
        // When
        val result = service.getVideo(videoId)
        
        // Then
        assertThat(result).isNull()
    }
    
    @Test
    fun `getAllVideos should return all videos`() = runTest {
        // Given
        val videos = listOf(
            Video(
                id = UUID.randomUUID(),
                title = "Video 1",
                fileName = "video1.mp4",
                fileSize = 1024L,
                status = ProcessingStatus.PENDING,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Video(
                id = UUID.randomUUID(),
                title = "Video 2",
                fileName = "video2.mp4",
                fileSize = 2048L,
                status = ProcessingStatus.COMPLETED,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        
        coEvery { videoRepository.findAll() } returns videos
        
        // When
        val result = service.getAllVideos()
        
        // Then
        assertThat(result).hasSize(2)
        coVerify(exactly = 1) { videoRepository.findAll() }
    }
}
