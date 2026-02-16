package com.vidopi.processor.service

import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetadataServiceTest {

	private lateinit var metadataService: MetadataService

	@BeforeEach
	fun setUp() {
		metadataService = MetadataService()
	}

	@Test
	fun `generateMetadataFile should create valid JSON file`() {
		// Given
		val videoResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.mp4",
			publicUrl = "https://cdn.example.com/videos/test-uuid.mp4",
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		val thumbnailResults = mapOf(
			"small" to R2Service.ThumbnailResult(
				bucket = "test-bucket",
				key = "videos/test-uuid-small.jpg",
				publicUrl = "https://cdn.example.com/videos/test-uuid-small.jpg",
				downloadUrl = "https://presigned-url.com/thumb-small.jpg",
				expiresInSeconds = 3600
			),
			"medium" to R2Service.ThumbnailResult(
				bucket = "test-bucket",
				key = "videos/test-uuid-medium.jpg",
				publicUrl = "https://cdn.example.com/videos/test-uuid-medium.jpg",
				downloadUrl = "https://presigned-url.com/thumb-medium.jpg",
				expiresInSeconds = 3600
			)
		)

		val originalFileName = "test-video.mp4"
		val fileSize = 1024000L
		val contentType = "video/mp4"

		// When
		val metadataFile = metadataService.generateMetadataFile(
			videoResult = videoResult,
			thumbnailResults = thumbnailResults,
			originalFileName = originalFileName,
			fileSize = fileSize,
			contentType = contentType
		)

		// Then
		assertNotNull(metadataFile)
		assertTrue(metadataFile.exists())
		assertTrue(metadataFile.length() > 0)
		assertTrue(metadataFile.name.endsWith(".json"))

		// Verify JSON content is valid
		val objectMapper = ObjectMapper()
		val metadata = objectMapper.readValue(metadataFile, VideoMetadata::class.java)
		
		assertEquals(originalFileName, metadata.video.fileName)
		assertEquals(fileSize, metadata.video.fileSize)
		assertEquals(contentType, metadata.video.contentType)
		assertEquals(videoResult.downloadUrl, metadata.video.url)
		assertEquals(videoResult.key, metadata.video.key)
		
		assertEquals(2, metadata.thumbnails.size)
		assertTrue(metadata.thumbnails.containsKey("small"))
		assertTrue(metadata.thumbnails.containsKey("medium"))
		
		assertEquals("test-bucket", metadata.metadata.bucket)
		assertNotNull(metadata.uploadedAt)

		// Cleanup
		metadataFile.delete()
	}

	@Test
	fun `generateMetadataFile should handle empty thumbnails`() {
		// Given
		val videoResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.mp4",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		// When
		val metadataFile = metadataService.generateMetadataFile(
			videoResult = videoResult,
			thumbnailResults = emptyMap(),
			originalFileName = "test.mp4",
			fileSize = 1000L,
			contentType = "video/mp4"
		)

		// Then
		assertNotNull(metadataFile)
		val objectMapper = ObjectMapper()
		val metadata = objectMapper.readValue(metadataFile, VideoMetadata::class.java)
		
		assertTrue(metadata.thumbnails.isEmpty())

		// Cleanup
		metadataFile.delete()
	}

	@Test
	fun `generateMetadataFile should handle null contentType`() {
		// Given
		val videoResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.mp4",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		// When
		val metadataFile = metadataService.generateMetadataFile(
			videoResult = videoResult,
			thumbnailResults = emptyMap(),
			originalFileName = "test.mp4",
			fileSize = 1000L,
			contentType = null
		)

		// Then
		assertNotNull(metadataFile)
		val objectMapper = ObjectMapper()
		val metadata = objectMapper.readValue(metadataFile, VideoMetadata::class.java)
		
		assertEquals("video/mp4", metadata.video.contentType) // Should default to video/mp4

		// Cleanup
		metadataFile.delete()
	}

	@Test
	fun `generateMetadataFile should extract folder from key correctly`() {
		// Given
		val videoResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/subfolder/test-uuid.mp4",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		// When
		val metadataFile = metadataService.generateMetadataFile(
			videoResult = videoResult,
			thumbnailResults = emptyMap(),
			originalFileName = "test.mp4",
			fileSize = 1000L,
			contentType = "video/mp4"
		)

		// Then
		val objectMapper = ObjectMapper()
		val metadata = objectMapper.readValue(metadataFile, VideoMetadata::class.java)
		
		assertEquals("videos/subfolder", metadata.metadata.folder)

		// Cleanup
		metadataFile.delete()
	}
}
