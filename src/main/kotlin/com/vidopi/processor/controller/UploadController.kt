package com.vidopi.processor.controller

import com.vidopi.processor.service.R2Service
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/upload")
class UploadController(
	private val r2Service: R2Service
) {
	private val logger = LoggerFactory.getLogger(UploadController::class.java)

	@PostMapping(value = ["/video"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun uploadVideo(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Any>> {
		return try {
			if (file.isEmpty) {
				return ResponseEntity.badRequest()
					.body(mapOf("error" to "File is empty", "code" to "EMPTY_FILE"))
			}

			logger.info("Received file upload request: ${file.originalFilename}, size: ${file.size} bytes")

			val publicUrl = r2Service.uploadVideo(
				inputStream = file.inputStream,
				originalFileName = file.originalFilename ?: "unknown",
				contentType = file.contentType,
				fileSize = file.size
			)

			ResponseEntity.ok(
				mapOf(
					"success" to true,
					"url" to publicUrl,
					"fileName" to (file.originalFilename ?: "unknown"),
					"fileSize" to file.size,
					"contentType" to (file.contentType ?: "unknown")
				)
			)
		} catch (e: Exception) {
			logger.error("Error uploading file: ${e.message}", e)
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(mapOf("error" to (e.message ?: "Unknown error"), "code" to "UPLOAD_ERROR"))
		}
	}
}
