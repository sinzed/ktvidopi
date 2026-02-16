package com.vidopi.processor.config

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

/**
 * Test configuration that provides mocked S3Client and S3Presigner
 * for integration and E2E tests.
 */
@TestConfiguration
class TestR2Config {

	@Bean
	@Primary
	fun testS3Client(): S3Client {
		return mockk(relaxed = true)
	}

	@Bean
	@Primary
	fun testS3Presigner(): S3Presigner {
		return mockk(relaxed = true)
	}
}
