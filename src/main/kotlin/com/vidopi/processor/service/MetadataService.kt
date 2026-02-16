package com.vidopi.processor.service

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant

data class VideoMetadata(
	val video: VideoInfo,
	val thumbnails: Map<String, ThumbnailInfo>,
	val metadata: MetadataInfo,
	val uploadedAt: String
)

data class VideoInfo(
	val url: String,
	val key: String,
	val fileName: String,
	val fileSize: Long,
	val contentType: String
)

data class ThumbnailInfo(
	val url: String,
	val key: String,
	val size: String
)

data class MetadataInfo(
	val bucket: String,
	val folder: String
)

@Service
class MetadataService {
	private val logger = LoggerFactory.getLogger(MetadataService::class.java)
	private val objectMapper: ObjectMapper = jacksonObjectMapper()

	/**
	 * Generates JSON metadata file for video and thumbnails.
	 * @param videoResult Video upload result
	 * @param thumbnailResults Map of thumbnail size to upload result
	 * @param originalFileName Original file name
	 * @param fileSize Original file size
	 * @param contentType Original content type
	 * @return File containing the JSON metadata
	 */
	fun generateMetadataFile(
		videoResult: R2Service.UploadResult,
		thumbnailResults: Map<String, R2Service.ThumbnailResult>,
		originalFileName: String,
		fileSize: Long,
		contentType: String?
	): File {
		val metadata = VideoMetadata(
			video = VideoInfo(
				url = videoResult.downloadUrl,
				key = videoResult.key,
				fileName = originalFileName,
				fileSize = fileSize,
				contentType = contentType ?: "video/mp4"
			),
			thumbnails = thumbnailResults.mapValues { (sizeName, result) ->
				ThumbnailInfo(
					url = result.downloadUrl,
					key = result.key,
					size = sizeName
				)
			},
			metadata = MetadataInfo(
				bucket = videoResult.bucket,
				folder = videoResult.key.substringBeforeLast("/")
			),
			uploadedAt = Instant.now().toString()
		)

		val jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata)
		val tempFile = File.createTempFile("metadata_", "_${System.currentTimeMillis()}.json")
		tempFile.writeText(jsonContent)
		
		logger.info("Generated metadata file: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
		return tempFile
	}
}
