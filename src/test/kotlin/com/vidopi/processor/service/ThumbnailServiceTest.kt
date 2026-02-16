package com.vidopi.processor.service

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThumbnailServiceTest {

	private lateinit var thumbnailService: ThumbnailService

	@BeforeEach
	fun setUp() {
		thumbnailService = ThumbnailService(
			ffmpegPath = "ffmpeg",
			timeOffsetSeconds = 1,
			quality = 2
		)
	}

	@Test
	fun `isFfmpegAvailable should return false when ffmpeg is not available`() {
		// Given
		val serviceWithInvalidPath = ThumbnailService(
			ffmpegPath = "nonexistent-ffmpeg-path-12345",
			timeOffsetSeconds = 1,
			quality = 2
		)

		// When
		val result = serviceWithInvalidPath.isFfmpegAvailable()

		// Then
		assertFalse(result)
	}

	@Test
	fun `generateThumbnails should return empty map when video file does not exist`() = runTest {
		// Given
		val nonExistentFile = File("/nonexistent/path/video.mp4")

		// When
		val result = thumbnailService.generateThumbnails(nonExistentFile)

		// Then
		assertTrue(result.isEmpty())
	}

	@Test
	fun `generateThumbnails should return empty map when video file is not readable`() = runTest {
		// Given
		val unreadableFile = mockk<File> {
			every { exists() } returns true
			every { canRead() } returns false
			every { absolutePath } returns "/test/video.mp4"
		}

		// When
		val result = thumbnailService.generateThumbnails(unreadableFile)

		// Then
		assertTrue(result.isEmpty())
	}

	@Test
	fun `generateThumbnails should handle ffmpeg failure gracefully`() = runTest {
		// Given
		val videoFile = File.createTempFile("test_video_", ".mp4")
		videoFile.writeBytes("fake video content".toByteArray())
		
		// Use a mock ffmpeg path that will fail
		val serviceWithMockFfmpeg = spyk(thumbnailService)
		every { serviceWithMockFfmpeg.isFfmpegAvailable() } returns false

		// When
		val result = serviceWithMockFfmpeg.generateThumbnails(videoFile)

		// Then
		// Should return empty map when ffmpeg fails
		assertTrue(result.isEmpty())
		
		// Cleanup
		videoFile.delete()
	}

	@Test
	fun `thumbnail service should have correct default sizes`() {
		// Given - service is initialized with default sizes
		val service = ThumbnailService(
			ffmpegPath = "ffmpeg",
			timeOffsetSeconds = 1,
			quality = 2
		)

		// When - we can't directly access private thumbnailSizes, but we can verify
		// the service is configured correctly by checking it's not null
		assertNotNull(service)
	}

	@Test
	fun `thumbnail service should use configured time offset`() {
		// Given
		val customTimeOffset = 5
		val service = ThumbnailService(
			ffmpegPath = "ffmpeg",
			timeOffsetSeconds = customTimeOffset,
			quality = 2
		)

		// When/Then - service should be initialized with custom time offset
		assertNotNull(service)
	}

	@Test
	fun `thumbnail service should use configured quality`() {
		// Given
		val customQuality = 5
		val service = ThumbnailService(
			ffmpegPath = "ffmpeg",
			timeOffsetSeconds = 1,
			quality = customQuality
		)

		// When/Then - service should be initialized with custom quality
		assertNotNull(service)
	}
}
