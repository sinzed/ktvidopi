package com.vidopi.processor.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

data class ThumbnailSize(
	val name: String,
	val width: Int,
	val height: Int
)

@Service
class ThumbnailService(
	@Value("\${thumbnail.ffmpeg-path:ffmpeg}") private val ffmpegPath: String,
	@Value("\${thumbnail.time-offset-seconds:1}") private val timeOffsetSeconds: Int,
	@Value("\${thumbnail.quality:2}") private val quality: Int // 2-31, lower is better quality
) {
	private val logger = LoggerFactory.getLogger(ThumbnailService::class.java)

	// Define 3 thumbnail sizes
	private val thumbnailSizes = listOf(
		ThumbnailSize("small", 320, 180),
		ThumbnailSize("medium", 640, 360),
		ThumbnailSize("large", 1280, 720)
	)

	/**
	 * Generates multiple thumbnails of different sizes from a video file.
	 * @param videoFile File object pointing to the video file
	 * @return Map of size name to thumbnail File, or empty map if generation failed
	 */
	fun generateThumbnails(videoFile: File): Map<String, File> {
		val thumbnails = mutableMapOf<String, File>()
		
		for (size in thumbnailSizes) {
			val thumbnail = generateThumbnail(videoFile, size)
			if (thumbnail != null) {
				thumbnails[size.name] = thumbnail
			}
		}
		
		return thumbnails
	}

	/**
	 * Generates a thumbnail from a video file with specific dimensions.
	 * @param videoFile File object pointing to the video file
	 * @param size ThumbnailSize specifying width and height
	 * @return File object pointing to the generated thumbnail, or null if generation failed
	 */
	private fun generateThumbnail(videoFile: File, size: ThumbnailSize): File? {
		val tempThumbnailFile = File.createTempFile("thumb_", "_${System.currentTimeMillis()}.jpg")
		
		return try {
			if (!videoFile.exists() || !videoFile.canRead()) {
				logger.error("Video file does not exist or is not readable: ${videoFile.absolutePath}")
				return null
			}

			logger.info("Generating thumbnail from video file: ${videoFile.absolutePath}")

			// Generate thumbnail using FFmpeg
			val command = listOf(
				ffmpegPath,
				"-i", videoFile.absolutePath,
				"-ss", timeOffsetSeconds.toString(), // Seek to specific time
				"-vframes", "1", // Extract only 1 frame
				"-vf", "scale=${size.width}:${size.height}:force_original_aspect_ratio=decrease,pad=${size.width}:${size.height}:(ow-iw)/2:(oh-ih)/2", // Scale and pad to maintain aspect ratio
				"-q:v", quality.toString(), // Quality setting
				"-y", // Overwrite output file
				tempThumbnailFile.absolutePath
			)

			logger.info("Executing FFmpeg command: ${command.joinToString(" ")}")
			
			val process = ProcessBuilder(command)
				.redirectErrorStream(true)
				.start()

			val exitCode = process.waitFor()
			
			if (exitCode == 0 && tempThumbnailFile.exists() && tempThumbnailFile.length() > 0) {
				logger.info("Thumbnail generated successfully: ${tempThumbnailFile.absolutePath} (${tempThumbnailFile.length()} bytes)")
				tempThumbnailFile
			} else {
				logger.warn("FFmpeg failed with exit code: $exitCode. Thumbnail file exists: ${tempThumbnailFile.exists()}, size: ${tempThumbnailFile.length()}")
				// Clean up on failure
				tempThumbnailFile.delete()
				null
			}
		} catch (e: Exception) {
			logger.error("Error generating thumbnail: ${e.message}", e)
			tempThumbnailFile.delete()
			null
		}
	}

	/**
	 * Checks if FFmpeg is available in the system.
	 */
	fun isFfmpegAvailable(): Boolean {
		return try {
			val process = ProcessBuilder(ffmpegPath, "-version")
				.redirectErrorStream(true)
				.start()
			val exitCode = process.waitFor()
			exitCode == 0
		} catch (e: Exception) {
			logger.warn("FFmpeg not available: ${e.message}")
			false
		}
	}
}
