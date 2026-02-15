package com.ktvidopi.api

import com.ktvidopi.api.dto.CreateVideoRequest
import com.ktvidopi.api.dto.VideoResponse
import com.ktvidopi.api.exception.ErrorResponse
import com.ktvidopi.api.exception.VideoNotFoundException
import com.ktvidopi.api.mapper.VideoMapper.toDto
import com.ktvidopi.service.VideoProcessingService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Controller layer - handles HTTP requests and delegates to service layer.
 * Demonstrates clean separation of concerns.
 */
class VideoController(private val videoProcessingService: VideoProcessingService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun Route.videoRoutes() {
        route("/api/videos") {
            // Create video and trigger async processing
            post {
                val request = call.receive<CreateVideoRequest>()
                logger.info("Creating video: ${request.title}")
                
                val video = videoProcessingService.createVideo(
                    title = request.title,
                    fileName = request.fileName,
                    fileSize = request.fileSize
                )
                
                call.respond(HttpStatusCode.Created, video.toDto())
            }
            
            // Get all videos
            get {
                logger.info("Fetching all videos")
                val videos = videoProcessingService.getAllVideos()
                call.respond(videos.map { it.toDto() })
            }
            
            // Get video by ID
            get("/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                logger.info("Fetching video: $id")
                
                val video = videoProcessingService.getVideo(id)
                    ?: throw VideoNotFoundException(id)
                
                call.respond(video.toDto())
            }
        }
    }
}
