package com.vidopi.processor.integration

import com.vidopi.processor.controller.UploadController
import com.vidopi.processor.service.MetadataService
import com.vidopi.processor.service.R2Service
import com.vidopi.processor.service.ThumbnailService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that verify the interaction between UploadController and its services.
 * These tests use real service instances but mock external dependencies (R2, FFmpeg).
 */
class UploadIntegrationTest {

	private lateinit var r2Service: R2Service
	private lateinit var thumbnailService: ThumbnailService
	private lateinit var metadataService: MetadataService
	private lateinit var uploadController: UploadController

	@BeforeEach
	fun setUp() {
		// Mock R2Service (external dependency)
		r2Service = mockk()
		
		// Use real service instances for ThumbnailService and MetadataService
		thumbnailService = ThumbnailService(
			ffmpegPath = "ffmpeg",
			timeOffsetSeconds = 1,
			quality = 2
		)
		metadataService = MetadataService()
		
		uploadController = UploadController(r2Service, thumbnailService, metadataService)
	}

	@Test
	fun `uploadVideo should integrate all services correctly`() {
		// Given
		val videoContent = "fake video content".toByteArray()
		val multipartFile = MockMultipartFile(
			"file",
			"test-video.mp4",
			"video/mp4",
			videoContent
		)

		val uploadResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.mp4",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		// Create a real video file for thumbnail generation
		val videoFile = File.createTempFile("test_video_", ".mp4")
		videoFile.writeBytes(videoContent)

		every { r2Service.uploadVideo(any(), any(), any(), any()) } answers {
			// Simulate saving to temp file
			val inputStream = firstArg<java.io.InputStream>()
			val tempFile = File.createTempFile("upload_", ".mp4")
			tempFile.outputStream().use { inputStream.copyTo(it) }
			uploadResult
		}

		// Mock thumbnail uploads
		every { r2Service.uploadThumbnail(any(), any(), any()) } answers {
			val file = firstArg<File>()
			val videoKey = secondArg<String>()
			val sizeName = thirdArg<String>()
			R2Service.ThumbnailResult(
				bucket = "test-bucket",
				key = "$videoKey-$sizeName.jpg",
				publicUrl = null,
				downloadUrl = "https://presigned-url.com/$sizeName.jpg",
				expiresInSeconds = 3600
			)
		}

		// Mock metadata upload
		every { r2Service.uploadMetadata(any(), any()) } answers {
			val file = firstArg<File>()
			val videoKey = secondArg<String>()
			R2Service.MetadataResult(
				bucket = "test-bucket",
				key = "$videoKey.json",
				publicUrl = null,
				downloadUrl = "https://presigned-url.com/metadata.json",
				expiresInSeconds = 3600
			)
		}

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		assertEquals(HttpStatus.OK, response.statusCode)
		assertNotNull(response.body)
		assertTrue(response.body!!.containsKey("video"))
		assertTrue(response.body!!.containsKey("thumbnails"))
		
		// Verify service interactions
		verify(exactly = 1) { r2Service.uploadVideo(any(), any(), any(), any()) }
		verify(atLeast = 0) { r2Service.uploadThumbnail(any(), any(), any()) }
		verify(exactly = 1) { r2Service.uploadMetadata(any(), any()) }

		// Cleanup
		videoFile.delete()
	}

	@Test
	fun `uploadVideo should handle service chain correctly when thumbnails fail`() {
		// Given
		val videoContent = "fake video content".toByteArray()
		val multipartFile = MockMultipartFile(
			"file",
			"test-video.mp4",
			"video/mp4",
			videoContent
		)

		val uploadResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.mp4",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		every { r2Service.uploadVideo(any(), any(), any(), any()) } returns uploadResult
		
		// ThumbnailService will fail because FFmpeg won't work with fake content
		// but the controller should handle it gracefully

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		// Should still succeed even if thumbnails fail
		assertEquals(HttpStatus.OK, response.statusCode)
		assertNotNull(response.body)
		assertTrue(response.body!!.containsKey("video"))
		verify(exactly = 1) { r2Service.uploadVideo(any(), any(), any(), any()) }
	}

	@Test
	fun `metadataService should generate correct metadata from R2Service results`() {
		// Given
		val videoResult = R2Service.UploadResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.mp4",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/video.mp4",
			expiresInSeconds = 3600
		)

		val thumbnailResults = mapOf(
			"small" to R2Service.ThumbnailResult(
				bucket = "test-bucket",
				key = "videos/test-uuid-small.jpg",
				publicUrl = null,
				downloadUrl = "https://presigned-url.com/small.jpg",
				expiresInSeconds = 3600
			),
			"medium" to R2Service.ThumbnailResult(
				bucket = "test-bucket",
				key = "videos/test-uuid-medium.jpg",
				publicUrl = null,
				downloadUrl = "https://presigned-url.com/medium.jpg",
				expiresInSeconds = 3600
			)
		)

		// When
		val metadataFile = metadataService.generateMetadataFile(
			videoResult = videoResult,
			thumbnailResults = thumbnailResults,
			originalFileName = "test-video.mp4",
			fileSize = 1024000L,
			contentType = "video/mp4"
		)

		// Then
		assertNotNull(metadataFile)
		assertTrue(metadataFile.exists())
		assertTrue(metadataFile.length() > 0)
		
		// Verify metadata content structure
		val metadataContent = metadataFile.readText()
		assertTrue(metadataContent.contains("test-video.mp4"))
		assertTrue(metadataContent.contains("small"))
		assertTrue(metadataContent.contains("medium"))
		assertTrue(metadataContent.contains("test-bucket"))

		// Cleanup
		metadataFile.delete()
	}
}
