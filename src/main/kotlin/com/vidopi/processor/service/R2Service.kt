package com.vidopi.processor.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.util.UUID

@Service
class R2Service(
	private val s3Client: S3Client,
	@Value("\${cloudflare.r2.bucket-name:default-bucket}") private val bucketName: String,
	@Value("\${cloudflare.r2.public-url-base:}") private val publicUrlBase: String
) {
	private val logger = LoggerFactory.getLogger(R2Service::class.java)

	fun uploadVideo(inputStream: InputStream, originalFileName: String, contentType: String?, fileSize: Long? = null): String {
		val fileExtension = originalFileName.substringAfterLast('.', "")
		val uniqueFileName = "${UUID.randomUUID()}.$fileExtension"
		
		logger.info("Uploading file: $originalFileName as $uniqueFileName to bucket: $bucketName")

		val putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(uniqueFileName)
			.contentType(contentType ?: "video/mp4")
			.build()

		val requestBody = if (fileSize != null && fileSize > 0) {
			RequestBody.fromInputStream(inputStream, fileSize)
		} else {
			// Use -1 to let SDK read until EOF
			RequestBody.fromInputStream(inputStream, -1)
		}
		
		s3Client.putObject(putObjectRequest, requestBody)
		
		val publicUrl = if (publicUrlBase.isNotBlank()) {
			"$publicUrlBase/$uniqueFileName"
		} else {
			// If no public URL base is configured, construct from endpoint
			"https://$bucketName.r2.cloudflarestorage.com/$uniqueFileName"
		}

		logger.info("File uploaded successfully. Public URL: $publicUrl")
		return publicUrl
	}
}
