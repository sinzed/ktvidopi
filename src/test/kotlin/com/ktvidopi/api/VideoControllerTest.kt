package com.ktvidopi.api

import com.ktvidopi.api.dto.CreateVideoRequest
import com.ktvidopi.api.dto.VideoResponse
import com.ktvidopi.api.exception.ErrorResponse
import com.ktvidopi.api.exception.VideoNotFoundException
import com.ktvidopi.domain.ProcessingStatus
import com.ktvidopi.domain.Video
import com.ktvidopi.service.VideoProcessingService
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class VideoControllerTest {
    private val videoProcessingService = mockk<VideoProcessingService>()
    private val controller = VideoController(videoProcessingService)
    
    private fun Application.testModule() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        install(StatusPages) {
            exception<VideoNotFoundException> { call, cause ->
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(message = cause.message ?: "Video not found", code = "VIDEO_NOT_FOUND")
                )
            }
        }
        
        routing {
            controller.videoRoutes()
        }
    }
    
    @Test
    fun `POST videos should create video`() = runTest {
        // Given
        val videoId = UUID.randomUUID()
        val video = Video(
            id = videoId,
            title = "Test Video",
            fileName = "test.mp4",
            fileSize = 1024L,
            status = ProcessingStatus.PENDING,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery {
            videoProcessingService.createVideo("Test Video", "test.mp4", 1024L)
        } returns video
        
        // When & Then
        testApplication {
            application {
                testModule()
            }
            
            val response = client.post("/api/videos") {
                contentType(ContentType.Application.Json)
                setBody(CreateVideoRequest("Test Video", "test.mp4", 1024L))
            }
            
            assertThat(response.status).isEqualTo(HttpStatusCode.Created)
            val body: VideoResponse = response.body()
            assertThat(body.title).isEqualTo("Test Video")
            assertThat(body.status).isEqualTo(ProcessingStatus.PENDING)
        }
    }
    
    @Test
    fun `GET videos should return all videos`() = runTest {
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
            )
        )
        
        coEvery { videoProcessingService.getAllVideos() } returns videos
        
        // When & Then
        testApplication {
            application {
                testModule()
            }
            
            val response = client.get("/api/videos")
            
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body: List<VideoResponse> = response.body()
            assertThat(body).hasSize(1)
        }
    }
    
    @Test
    fun `GET video by id should return video when exists`() = runTest {
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
        
        coEvery { videoProcessingService.getVideo(videoId) } returns video
        
        // When & Then
        testApplication {
            application {
                testModule()
            }
            
            val response = client.get("/api/videos/$videoId")
            
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body: VideoResponse = response.body()
            assertThat(body.id).isEqualTo(videoId)
        }
    }
    
    @Test
    fun `GET video by id should return 404 when not exists`() = runTest {
        // Given
        val videoId = UUID.randomUUID()
        coEvery { videoProcessingService.getVideo(videoId) } returns null
        
        // When & Then
        testApplication {
            application {
                testModule()
            }
            
            val response = client.get("/api/videos/$videoId")
            
            assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
            val body: ErrorResponse = response.body()
            assertThat(body.code).isEqualTo("VIDEO_NOT_FOUND")
        }
    }
}
