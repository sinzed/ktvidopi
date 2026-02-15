package com.ktvidopi.service

import com.ktvidopi.domain.ProcessingStatus
import com.ktvidopi.domain.Video
import com.ktvidopi.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Service layer demonstrating coroutine-first architecture with structured concurrency.
 * 
 * Key concepts demonstrated:
 * - SupervisorJob: Prevents child coroutine failures from cancelling parent
 * - withContext(Dispatchers.IO): Switches to IO dispatcher for blocking operations
 * - coroutineScope: Ensures structured concurrency - all child coroutines complete before parent
 * - Cancellation propagation: Proper handling of cancellation signals
 */
class VideoProcessingService(
    private val videoRepository: VideoRepository,
    private val processingScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Creates a video and triggers async processing.
     * Uses structured concurrency to ensure proper cleanup.
     */
    suspend fun createVideo(title: String, fileName: String, fileSize: Long): Video {
        val video = Video(
            id = UUID.randomUUID(),
            title = title,
            fileName = fileName,
            fileSize = fileSize,
            status = ProcessingStatus.PENDING,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val savedVideo = withContext(Dispatchers.IO) {
            videoRepository.create(video)
        }
        
        // Launch async processing job - uses SupervisorJob so failures don't cancel parent
        processingScope.launch {
            processVideo(savedVideo.id)
        }
        
        logger.info("Video created with id: ${savedVideo.id}, processing started")
        return savedVideo
    }
    
    suspend fun getVideo(id: UUID): Video? = withContext(Dispatchers.IO) {
        videoRepository.findById(id)
    }
    
    suspend fun getAllVideos(): List<Video> = withContext(Dispatchers.IO) {
        videoRepository.findAll()
    }
    
    /**
     * Simulates heavy video processing work.
     * Demonstrates:
     * - Structured concurrency with coroutineScope
     * - Status updates during processing
     * - Error handling with proper status updates
     * - Cancellation awareness
     */
    private suspend fun processVideo(videoId: UUID) = coroutineScope {
        try {
            logger.info("Starting processing for video: $videoId")
            
            // Update status to PROCESSING
            withContext(Dispatchers.IO) {
                videoRepository.updateStatus(videoId, ProcessingStatus.PROCESSING)
            }
            
            // Simulate heavy processing work
            // In real scenario, this would be: transcoding, thumbnail generation, etc.
            simulateHeavyProcessing()
            
            // Update status to COMPLETED
            val processedAt = Instant.now()
            withContext(Dispatchers.IO) {
                videoRepository.updateStatus(videoId, ProcessingStatus.COMPLETED)
                videoRepository.updateProcessedAt(videoId, processedAt)
            }
            
            logger.info("Processing completed for video: $videoId")
        } catch (e: Exception) {
            logger.error("Processing failed for video: $videoId", e)
            
            // Update status to FAILED with error message
            withContext(Dispatchers.IO) {
                videoRepository.updateStatus(
                    videoId,
                    ProcessingStatus.FAILED,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * Simulates heavy processing work with multiple steps.
     * Uses delay() which is a suspending function - non-blocking.
     * Compare this to Thread.sleep() which would block the thread.
     */
    private suspend fun simulateHeavyProcessing() {
        // Step 1: Validate file
        delay(500)
        
        // Step 2: Extract metadata
        delay(1000)
        
        // Step 3: Generate thumbnails
        delay(1500)
        
        // Step 4: Transcode (simulated)
        delay(2000)
        
        // Step 5: Upload to CDN (simulated)
        delay(1000)
        
        // Total: ~6 seconds of simulated work
        // All non-blocking - thread is free to handle other requests
    }
}
