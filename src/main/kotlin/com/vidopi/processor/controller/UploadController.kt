package com.vidopi.processor.controller

import com.vidopi.processor.service.R2Service
import com.vidopi.processor.service.ThumbnailService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RestController
@RequestMapping("/api/upload")
class UploadController(
	private val r2Service: R2Service,
	private val thumbnailService: ThumbnailService
) {
	private val logger = LoggerFactory.getLogger(UploadController::class.java)

	@PostMapping(value = ["/video"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
	fun uploadVideo(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Any>> {
		var tempVideoFile: File? = null
		val thumbnailFiles = mutableListOf<File>()
		return try {
			if (file.isEmpty) {
				return ResponseEntity.badRequest()
					.body(mapOf("error" to "File is empty", "code" to "EMPTY_FILE"))
			}

			logger.info("Received file upload request: ${file.originalFilename}, size: ${file.size} bytes")

			// Save video to temp file (MultipartFile inputStream can only be read once)
			tempVideoFile = File.createTempFile("upload_", "_${System.currentTimeMillis()}")
			Files.copy(file.inputStream, tempVideoFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
			logger.info("Video saved to temp file: ${tempVideoFile.absolutePath}")

			// Upload video from temp file
			val result = r2Service.uploadVideo(
				inputStream = tempVideoFile.inputStream(),
				originalFileName = file.originalFilename ?: "unknown",
				contentType = file.contentType,
				fileSize = file.size
			)

			// Generate and upload thumbnails (3 sizes)
			val thumbnailUrls = mutableMapOf<String, String>()
			try {
				val thumbnails = thumbnailService.generateThumbnails(tempVideoFile)
				
				for ((sizeName, thumbnailFile) in thumbnails) {
					thumbnailFiles.add(thumbnailFile)
					try {
						val thumbnailResult = r2Service.uploadThumbnail(thumbnailFile, result.key, sizeName)
						thumbnailUrls[sizeName] = thumbnailResult.downloadUrl
						logger.info("Thumbnail ($sizeName) uploaded successfully: ${thumbnailResult.key}")
					} catch (e: Exception) {
						logger.error("Error uploading thumbnail ($sizeName): ${e.message}", e)
					}
				}
				
				if (thumbnails.isEmpty()) {
					logger.warn("No thumbnails were generated")
				}
			} catch (e: Exception) {
				logger.error("Error generating thumbnails: ${e.message}", e)
				// Don't fail the entire request if thumbnail generation fails
			}

			// Return simplified JSON with just download URLs
			val response = mapOf(
				"video" to result.downloadUrl,
				"thumbnails" to thumbnailUrls
			)

			ResponseEntity.ok(response)
		} catch (e: Exception) {
			logger.error("Error uploading file: ${e.message}", e)
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(mapOf("error" to (e.message ?: "Unknown error"), "code" to "UPLOAD_ERROR"))
		} finally {
			// Clean up temp files
			tempVideoFile?.delete()
			thumbnailFiles.forEach { it.delete() }
		}
	}
}
