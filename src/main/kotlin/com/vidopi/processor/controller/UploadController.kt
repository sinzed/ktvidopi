package com.vidopi.processor.controller

import com.vidopi.processor.service.MetadataService
import com.vidopi.processor.service.R2Service
import com.vidopi.processor.service.ThumbnailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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
	private val thumbnailService: ThumbnailService,
	private val metadataService: MetadataService
) {
	private val logger = LoggerFactory.getLogger(UploadController::class.java)

	@PostMapping(value = ["/video"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
	fun uploadVideo(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Any>> {
		var tempVideoFile: File? = null
		val thumbnailFiles = mutableListOf<File>()
		var metadataFile: File? = null
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

			// Generate and upload thumbnails (3 sizes) and metadata in parallel using coroutines
			val (thumbnailUrls, metadataUrl) = runBlocking {
				try {
					// Generate all thumbnails in parallel
					val thumbnails = thumbnailService.generateThumbnails(tempVideoFile)
					
					if (thumbnails.isEmpty()) {
						logger.warn("No thumbnails were generated")
						emptyMap<String, String>() to null
					} else {
						// Upload all thumbnails to R2 in parallel
						val thumbnailResults = coroutineScope {
							val uploadDeferred = thumbnails.map { (sizeName, thumbnailFile) ->
								async(Dispatchers.IO) {
									thumbnailFiles.add(thumbnailFile)
									try {
										val thumbnailResult = r2Service.uploadThumbnail(thumbnailFile, result.key, sizeName)
										logger.info("Thumbnail ($sizeName) uploaded successfully: ${thumbnailResult.key}")
										sizeName to thumbnailResult
									} catch (e: Exception) {
										logger.error("Error uploading thumbnail ($sizeName): ${e.message}", e)
										null
									}
								}
							}
							
							// Wait for all uploads to complete and filter out nulls
							uploadDeferred.awaitAll()
								.filterNotNull()
								.associate { it.first to it.second }
						}
						
						// Generate and upload metadata in parallel with thumbnail processing
						val metadataResult = async(Dispatchers.IO) {
							try {
								val metadata = metadataService.generateMetadataFile(
									videoResult = result,
									thumbnailResults = thumbnailResults,
									originalFileName = file.originalFilename ?: "unknown",
									fileSize = file.size,
									contentType = file.contentType
								)
								metadataFile = metadata
								val metadataUploadResult = r2Service.uploadMetadata(metadata, result.key)
								logger.info("Metadata uploaded successfully: ${metadataUploadResult.key}")
								metadataUploadResult.downloadUrl
							} catch (e: Exception) {
								logger.error("Error generating/uploading metadata: ${e.message}", e)
								null
							}
						}
						
						// Wait for metadata upload to complete
						val metadataUrlValue = metadataResult.await()
						
						// Return thumbnail URLs and metadata URL
						thumbnailResults.mapValues { it.value.downloadUrl } to metadataUrlValue
					}
				} catch (e: Exception) {
					logger.error("Error generating thumbnails: ${e.message}", e)
					// Don't fail the entire request if thumbnail generation fails
					emptyMap<String, String>() to null
				}
			}

			// Return simplified JSON with download URLs including metadata
			val response = mutableMapOf<String, Any>(
				"video" to result.downloadUrl,
				"thumbnails" to thumbnailUrls
			)
			
			// Add metadata URL if available
			if (metadataUrl != null) {
				response["metadata"] = metadataUrl
			}
			
			// Log all URLs to console
			logger.info("=== Upload Complete ===")
			logger.info("Video URL: ${result.downloadUrl}")
			logger.info("Thumbnails:")
			thumbnailUrls.forEach { (size, url) ->
				logger.info("  $size: $url")
			}
			if (metadataUrl != null) {
				logger.info("Metadata URL: $metadataUrl")
			}
			logger.info("======================")

			ResponseEntity.ok(response)
		} catch (e: Exception) {
			logger.error("Error uploading file: ${e.message}", e)
			ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(mapOf("error" to (e.message ?: "Unknown error"), "code" to "UPLOAD_ERROR"))
		} finally {
			// Clean up temp files
			tempVideoFile?.delete()
			thumbnailFiles.forEach { it.delete() }
			metadataFile?.delete()
		}
	}
}
