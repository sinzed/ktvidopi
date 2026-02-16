package com.vidopi.processor.service

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class R2ServiceTest {

	private lateinit var s3Client: S3Client
	private lateinit var s3Presigner: S3Presigner
	private lateinit var r2Service: R2Service

	@BeforeEach
	fun setUp() {
		s3Client = mockk()
		s3Presigner = mockk()
		r2Service = R2Service(
			s3Client = s3Client,
			s3Presigner = s3Presigner,
			bucketName = "test-bucket",
			folderPrefix = "",
			publicUrlBase = "",
			presignedUrlTtlSeconds = 3600
		)
	}

	@Test
	fun `uploadVideo should upload file successfully`() {
		// Given
		val inputStream = ByteArrayInputStream("test video content".toByteArray())
		val originalFileName = "test-video.mp4"
		val contentType = "video/mp4"
		val fileSize = 1000L

		val putObjectRequest = mockk<PutObjectRequest>()
		val requestBody = mockk<RequestBody>()

		every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()
		
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/video.mp4"
		every { presignedRequest.url() } returns presignedUrl
		every { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) } returns presignedRequest

		// When
		val result = r2Service.uploadVideo(inputStream, originalFileName, contentType, fileSize)

		// Then
		assertNotNull(result)
		assertEquals("test-bucket", result.bucket)
		assertTrue(result.key.endsWith(".mp4"))
		assertEquals(3600, result.expiresInSeconds)
		verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
		verify(exactly = 1) { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) }
	}

	@Test
	fun `uploadVideo should handle folder prefix correctly`() {
		// Given
		val r2ServiceWithPrefix = R2Service(
			s3Client = s3Client,
			s3Presigner = s3Presigner,
			bucketName = "test-bucket",
			folderPrefix = "videos/",
			publicUrlBase = "",
			presignedUrlTtlSeconds = 3600
		)
		val inputStream = ByteArrayInputStream("test".toByteArray())

		every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/video.mp4"
		every { presignedRequest.url() } returns presignedUrl
		every { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) } returns presignedRequest

		// When
		val result = r2ServiceWithPrefix.uploadVideo(inputStream, "test.mp4", "video/mp4", null)

		// Then
		assertTrue(result.key.startsWith("videos/"))
	}

	@Test
	fun `uploadVideo should use public URL base when configured`() {
		// Given
		val r2ServiceWithPublicUrl = R2Service(
			s3Client = s3Client,
			s3Presigner = s3Presigner,
			bucketName = "test-bucket",
			folderPrefix = "",
			publicUrlBase = "https://cdn.example.com",
			presignedUrlTtlSeconds = 3600
		)
		val inputStream = ByteArrayInputStream("test".toByteArray())

		every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/video.mp4"
		every { presignedRequest.url() } returns presignedUrl
		every { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) } returns presignedRequest

		// When
		val result = r2ServiceWithPublicUrl.uploadVideo(inputStream, "test.mp4", "video/mp4", null)

		// Then
		assertNotNull(result.publicUrl)
		assertTrue(result.publicUrl!!.startsWith("https://cdn.example.com/"))
	}

	@Test
	fun `uploadThumbnail should upload thumbnail successfully`() {
		// Given
		val thumbnailFile = File.createTempFile("thumb_", ".jpg")
		thumbnailFile.writeBytes("thumbnail content".toByteArray())
		val videoKey = "videos/test-uuid.mp4"
		val sizeName = "small"

		every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/thumb.jpg"
		every { presignedRequest.url() } returns presignedUrl
		every { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) } returns presignedRequest

		// When
		val result = r2Service.uploadThumbnail(thumbnailFile, videoKey, sizeName)

		// Then
		assertNotNull(result)
		assertEquals("test-bucket", result.bucket)
		assertTrue(result.key.contains("test-uuid-small.jpg"))
		assertEquals(3600, result.expiresInSeconds)
		verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
		
		// Cleanup
		thumbnailFile.delete()
	}

	@Test
	fun `uploadMetadata should upload metadata successfully`() {
		// Given
		val metadataFile = File.createTempFile("metadata_", ".json")
		metadataFile.writeText("""{"test": "data"}""")
		val videoKey = "videos/test-uuid.mp4"

		every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/metadata.json"
		every { presignedRequest.url() } returns presignedUrl
		every { s3Presigner.presignGetObject(any<GetObjectPresignRequest>()) } returns presignedRequest

		// When
		val result = r2Service.uploadMetadata(metadataFile, videoKey)

		// Then
		assertNotNull(result)
		assertEquals("test-bucket", result.bucket)
		assertTrue(result.key.contains("test-uuid.json"))
		assertEquals(3600, result.expiresInSeconds)
		verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
		
		// Cleanup
		metadataFile.delete()
	}

	@Test
	fun `uploadVideo should enforce minimum TTL of 60 seconds`() {
		// Given
		val r2ServiceWithLowTtl = R2Service(
			s3Client = s3Client,
			s3Presigner = s3Presigner,
			bucketName = "test-bucket",
			folderPrefix = "",
			publicUrlBase = "",
			presignedUrlTtlSeconds = 30 // Below minimum
		)
		val inputStream = ByteArrayInputStream("test".toByteArray())

		every { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } returns mockk()
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/video.mp4"
		every { presignedRequest.url() } returns presignedUrl
		
		var capturedDuration: Duration? = null
		every { 
			s3Presigner.presignGetObject(match<GetObjectPresignRequest> {
				capturedDuration = it.signatureDuration()
				true
			})
		} returns presignedRequest

		// When
		r2ServiceWithLowTtl.uploadVideo(inputStream, "test.mp4", "video/mp4", null)

		// Then
		assertNotNull(capturedDuration)
		assertTrue(capturedDuration!!.seconds >= 60)
	}
}
