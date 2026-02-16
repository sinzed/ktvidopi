package com.vidopi.processor.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.InputStream
import java.time.Duration
import java.util.UUID

@Service
class R2Service(
	private val s3Client: S3Client,
	private val s3Presigner: S3Presigner,
	@Value("\${cloudflare.r2.bucket-name:default-bucket}") private val bucketName: String,
	@Value("\${cloudflare.r2.folder-prefix:}") private val folderPrefix: String,
	@Value("\${cloudflare.r2.public-url-base:}") private val publicUrlBase: String,
	@Value("\${cloudflare.r2.presigned-url-ttl-seconds:3600}") private val presignedUrlTtlSeconds: Long
) {
	private val logger = LoggerFactory.getLogger(R2Service::class.java)

	data class UploadResult(
		val bucket: String,
		val key: String,
		val publicUrl: String?,
		val downloadUrl: String,
		val expiresInSeconds: Long
	)

	fun uploadVideo(inputStream: InputStream, originalFileName: String, contentType: String?, fileSize: Long? = null): UploadResult {
		val fileExtension = originalFileName.substringAfterLast('.', "")
		val uniqueFileName = "${UUID.randomUUID()}.$fileExtension"
		
		// Build the object key with folder prefix if configured
		val objectKey = if (folderPrefix.isNotBlank()) {
			val normalizedPrefix = folderPrefix.trim().removePrefix("/").let { 
				if (it.endsWith("/")) it else "$it/"
			}
			"$normalizedPrefix$uniqueFileName"
		} else {
			uniqueFileName
		}
		
		logger.info("Uploading file: $originalFileName as $objectKey to bucket: $bucketName")
		logger.info("Using bucket name from config: $bucketName, folder prefix: ${folderPrefix.takeIf { it.isNotBlank() } ?: "none"}")

		val putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(objectKey)
			.contentType(contentType ?: "video/mp4")
			.build()

		val requestBody = if (fileSize != null && fileSize > 0) {
			RequestBody.fromInputStream(inputStream, fileSize)
		} else {
			// Use -1 to let SDK read until EOF
			RequestBody.fromInputStream(inputStream, -1)
		}
		
		s3Client.putObject(putObjectRequest, requestBody)
		
		val publicUrl = publicUrlBase
			.takeIf { it.isNotBlank() }
			?.trimEnd('/')
			?.let { "$it/$objectKey" }

		// NOTE: R2's `*.r2.cloudflarestorage.com` is an S3 API endpoint (not a public asset URL).
		// Return a presigned URL so the client can download without credentials.
		val getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(objectKey)
			.build()

		val ttl = presignedUrlTtlSeconds.coerceAtLeast(60)
		val presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(ttl))
			.getObjectRequest(getObjectRequest)
			.build()

		val downloadUrl = s3Presigner.presignGetObject(presignRequest).url().toExternalForm()

		logger.info("File uploaded successfully. key=$objectKey publicUrl=$publicUrl presignedTtlSeconds=$ttl")
		return UploadResult(
			bucket = bucketName,
			key = objectKey,
			publicUrl = publicUrl,
			downloadUrl = downloadUrl,
			expiresInSeconds = ttl
		)
	}
}
