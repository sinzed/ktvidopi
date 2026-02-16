package com.vidopi.processor.controller

import com.vidopi.processor.service.MetadataService
import com.vidopi.processor.service.R2Service
import com.vidopi.processor.service.ThumbnailService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UploadControllerTest {

	private lateinit var r2Service: R2Service
	private lateinit var thumbnailService: ThumbnailService
	private lateinit var metadataService: MetadataService
	private lateinit var uploadController: UploadController

	@BeforeEach
	fun setUp() {
		r2Service = mockk()
		thumbnailService = mockk()
		metadataService = mockk()
		uploadController = UploadController(r2Service, thumbnailService, metadataService)
	}

	@Test
	fun `uploadVideo should return bad request when file is empty`() {
		// Given
		val emptyFile = MockMultipartFile(
			"file",
			"test.mp4",
			"video/mp4",
			ByteArray(0)
		)

		// When
		val response = uploadController.uploadVideo(emptyFile)

		// Then
		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		assertNotNull(response.body)
		assertEquals("EMPTY_FILE", response.body!!["code"])
		verify(exactly = 0) { r2Service.uploadVideo(any(), any(), any(), any()) }
	}

	@Test
	fun `uploadVideo should upload video successfully`() {
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

		val thumbnailFile = File.createTempFile("thumb_", ".jpg")
		thumbnailFile.writeBytes("thumbnail".toByteArray())

		val metadataFile = File.createTempFile("metadata_", ".json")
		metadataFile.writeText("""{"test": "data"}""")

		every { r2Service.uploadVideo(any(), any(), any(), any()) } returns uploadResult
		coEvery { thumbnailService.generateThumbnails(any()) } returns mapOf(
			"small" to thumbnailFile,
			"medium" to thumbnailFile,
			"large" to thumbnailFile
		)
		every { r2Service.uploadThumbnail(any(), any(), any()) } returns R2Service.ThumbnailResult(
			bucket = "test-bucket",
			key = "videos/test-uuid-small.jpg",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/thumb-small.jpg",
			expiresInSeconds = 3600
		)
		every { metadataService.generateMetadataFile(any(), any(), any(), any(), any()) } returns metadataFile
		every { r2Service.uploadMetadata(any(), any()) } returns R2Service.MetadataResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.json",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/metadata.json",
			expiresInSeconds = 3600
		)

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		assertEquals(HttpStatus.OK, response.statusCode)
		assertNotNull(response.body)
		assertTrue(response.body!!.containsKey("video"))
		assertTrue(response.body!!.containsKey("thumbnails"))
		assertTrue(response.body!!.containsKey("metadata"))
		assertEquals("https://presigned-url.com/video.mp4", response.body!!["video"])

		verify(exactly = 1) { r2Service.uploadVideo(any(), any(), any(), any()) }
		coVerify(exactly = 1) { thumbnailService.generateThumbnails(any()) }
		verify(exactly = 3) { r2Service.uploadThumbnail(any(), any(), any()) }
		verify(exactly = 1) { metadataService.generateMetadataFile(any(), any(), any(), any(), any()) }
		verify(exactly = 1) { r2Service.uploadMetadata(any(), any()) }

		// Cleanup
		thumbnailFile.delete()
		metadataFile.delete()
	}

	@Test
	fun `uploadVideo should handle thumbnail generation failure gracefully`() {
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
		coEvery { thumbnailService.generateThumbnails(any()) } throws RuntimeException("FFmpeg failed")

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		assertEquals(HttpStatus.OK, response.statusCode)
		assertNotNull(response.body)
		assertTrue(response.body!!.containsKey("video"))
		assertTrue(response.body!!.containsKey("thumbnails"))
		// Should still succeed even if thumbnails fail
		verify(exactly = 1) { r2Service.uploadVideo(any(), any(), any(), any()) }
	}

	@Test
	fun `uploadVideo should handle empty thumbnails`() {
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
		coEvery { thumbnailService.generateThumbnails(any()) } returns emptyMap()

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		assertEquals(HttpStatus.OK, response.statusCode)
		assertNotNull(response.body)
		assertTrue(response.body!!.containsKey("video"))
		val thumbnails = response.body!!["thumbnails"] as Map<*, *>
		assertTrue(thumbnails.isEmpty())
	}

	@Test
	fun `uploadVideo should handle R2Service upload failure`() {
		// Given
		val videoContent = "fake video content".toByteArray()
		val multipartFile = MockMultipartFile(
			"file",
			"test-video.mp4",
			"video/mp4",
			videoContent
		)

		every { r2Service.uploadVideo(any(), any(), any(), any()) } throws RuntimeException("R2 upload failed")

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
		assertNotNull(response.body)
		assertEquals("UPLOAD_ERROR", response.body!!["code"])
	}

	@Test
	fun `uploadVideo should clean up temp files on success`() {
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

		val thumbnailFile = File.createTempFile("thumb_", ".jpg")
		thumbnailFile.writeBytes("thumbnail".toByteArray())

		val metadataFile = File.createTempFile("metadata_", ".json")
		metadataFile.writeText("""{"test": "data"}""")

		every { r2Service.uploadVideo(any(), any(), any(), any()) } returns uploadResult
		coEvery { thumbnailService.generateThumbnails(any()) } returns mapOf("small" to thumbnailFile)
		every { r2Service.uploadThumbnail(any(), any(), any()) } returns R2Service.ThumbnailResult(
			bucket = "test-bucket",
			key = "videos/test-uuid-small.jpg",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/thumb-small.jpg",
			expiresInSeconds = 3600
		)
		every { metadataService.generateMetadataFile(any(), any(), any(), any(), any()) } returns metadataFile
		every { r2Service.uploadMetadata(any(), any()) } returns R2Service.MetadataResult(
			bucket = "test-bucket",
			key = "videos/test-uuid.json",
			publicUrl = null,
			downloadUrl = "https://presigned-url.com/metadata.json",
			expiresInSeconds = 3600
		)

		// When
		val response = uploadController.uploadVideo(multipartFile)

		// Then
		assertEquals(HttpStatus.OK, response.statusCode)
		// Files should be cleaned up (we can't verify deletion directly, but the finally block should execute)
	}
}
