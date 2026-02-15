package com.ktvidopi

import com.ktvidopi.api.VideoController
import com.ktvidopi.api.exception.ErrorResponse
import com.ktvidopi.api.exception.VideoNotFoundException
import com.ktvidopi.infrastructure.database.DatabaseFactory
import com.ktvidopi.repository.VideoRepository
import com.ktvidopi.repository.VideoRepositoryImpl
import com.ktvidopi.service.VideoProcessingService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    
    // Database configuration from environment variables
    val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/ktvidopi"
    val databaseUser = System.getenv("DATABASE_USER") ?: "postgres"
    val databasePassword = System.getenv("DATABASE_PASSWORD") ?: "postgres"
    
    // Initialize database
    DatabaseFactory.init(
        databaseUrl = databaseUrl,
        driverClassName = "org.postgresql.Driver",
        user = databaseUser,
        password = databasePassword
    )
    logger.info("Database initialized")
    
    // Dependency injection - constructor injection pattern
    val videoRepository: VideoRepository = VideoRepositoryImpl()
    val videoProcessingService = VideoProcessingService(videoRepository)
    val videoController = VideoController(videoProcessingService)
    
    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Exception handling
    install(StatusPages) {
        exception<VideoNotFoundException> { call, cause ->
            call.respond(
                io.ktor.http.HttpStatusCode.NotFound,
                ErrorResponse(message = cause.message ?: "Video not found", code = "VIDEO_NOT_FOUND")
            )
        }
        
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                io.ktor.http.HttpStatusCode.BadRequest,
                ErrorResponse(message = cause.message ?: "Invalid request", code = "BAD_REQUEST")
            )
        }
        
        exception<Exception> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                ErrorResponse(message = "Internal server error", code = "INTERNAL_ERROR")
            )
        }
    }
    
    // Routing
    routing {
        videoController.videoRoutes()
    }
    
    logger.info("Application started on port 8080")
}
