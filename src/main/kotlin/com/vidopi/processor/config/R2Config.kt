package com.vidopi.processor.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class R2Config {

	@Value("\${cloudflare.r2.access-key-id}")
	private lateinit var accessKeyId: String

	@Value("\${cloudflare.r2.secret-access-key}")
	private lateinit var secretAccessKey: String

	@Value("\${cloudflare.r2.endpoint}")
	private lateinit var endpoint: String

	@Value("\${cloudflare.r2.bucket-name:default-bucket}")
	private lateinit var bucketName: String

	@Bean
	fun r2S3Client(): S3Client {
		val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
		val credentialsProvider = StaticCredentialsProvider.create(credentials)

		return S3Client.builder()
			.endpointOverride(URI(endpoint))
			.region(Region.of("auto"))
			.credentialsProvider(credentialsProvider)
			.forcePathStyle(true)
			.build()
	}

	@Bean
	fun r2BucketName(): String = bucketName
}
