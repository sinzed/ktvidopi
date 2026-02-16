package com.vidopi.processor.e2e

import com.vidopi.processor.ProcessorApplication
import com.vidopi.processor.config.TestR2Config
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import java.net.URI

/**
 * End-to-end tests that test the full HTTP request/response cycle.
 * These tests use MockMvc to simulate HTTP requests and verify responses.
 */
@SpringBootTest(classes = [ProcessorApplication::class])
@AutoConfigureMockMvc
@Import(TestR2Config::class)
@TestPropertySource(properties = [
	"cloudflare.r2.access-key-id=test-key",
	"cloudflare.r2.secret-access-key=test-secret",
	"cloudflare.r2.endpoint=https://test.r2.cloudflarestorage.com",
	"cloudflare.r2.bucket-name=test-bucket",
	"cloudflare.r2.folder-prefix=",
	"cloudflare.r2.public-url-base=",
	"cloudflare.r2.presigned-url-ttl-seconds=3600",
	"thumbnail.ffmpeg-path=ffmpeg",
	"thumbnail.time-offset-seconds=1",
	"thumbnail.quality=2"
])
class UploadE2ETest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var s3Client: S3Client

	@Autowired
	private lateinit var s3Presigner: S3Presigner

	@BeforeEach
	fun setUp() {
		// Set up mocks for S3 operations
		every { s3Client.putObject(any(), any()) } returns mockk<PutObjectResponse>()
		
		val presignedRequest = mockk<PresignedGetObjectRequest>()
		val presignedUrl = mockk<URI>()
		every { presignedUrl.toExternalForm() } returns "https://presigned-url.com/test.mp4"
		every { presignedRequest.url() } returns presignedUrl
		every { s3Presigner.presignGetObject(any()) } returns presignedRequest
	}

	@Test
	fun `POST /api/upload/video should return 400 when file is empty`() {
		// Given
		val emptyFile = MockMultipartFile(
			"file",
			"test.mp4",
			"video/mp4",
			ByteArray(0)
		)

		// When & Then
		mockMvc.perform(
			multipart("/api/upload/video")
				.file(emptyFile)
		)
			.andExpect(status().isBadRequest)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.code").value("EMPTY_FILE"))
			.andExpect(jsonPath("$.error").value("File is empty"))
	}

	@Test
	fun `POST /api/upload/video should return 200 with video URL when upload succeeds`() {
		// Given
		val videoContent = "fake video content".toByteArray()
		val multipartFile = MockMultipartFile(
			"file",
			"test-video.mp4",
			"video/mp4",
			videoContent
		)

		// When & Then
		mockMvc.perform(
			multipart("/api/upload/video")
				.file(multipartFile)
		)
			.andExpect(status().isOk)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.video").exists())
			.andExpect(jsonPath("$.thumbnails").exists())
	}

	@Test
	fun `POST /api/upload/video should return JSON with correct structure`() {
		// Given
		val videoContent = "fake video content".toByteArray()
		val multipartFile = MockMultipartFile(
			"file",
			"test-video.mp4",
			"video/mp4",
			videoContent
		)

		// When & Then
		mockMvc.perform(
			multipart("/api/upload/video")
				.file(multipartFile)
		)
			.andExpect(status().isOk)
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.video").isString())
			.andExpect(jsonPath("$.thumbnails").isMap())
	}

	@Test
	fun `POST /api/upload/video should handle missing filename gracefully`() {
		// Given
		val videoContent = "fake video content".toByteArray()
		val multipartFile = MockMultipartFile(
			"file",
			null,
			"video/mp4",
			videoContent
		)

		// When & Then
		mockMvc.perform(
			multipart("/api/upload/video")
				.file(multipartFile)
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.video").exists())
	}

	@Test
	fun `POST /api/upload/video should handle large files`() {
		// Given - Create a larger file (simulate)
		val largeContent = ByteArray(1024 * 1024) { it.toByte() } // 1MB
		val multipartFile = MockMultipartFile(
			"file",
			"large-video.mp4",
			"video/mp4",
			largeContent
		)

		// When & Then
		mockMvc.perform(
			multipart("/api/upload/video")
				.file(multipartFile)
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.video").exists())
	}
}
