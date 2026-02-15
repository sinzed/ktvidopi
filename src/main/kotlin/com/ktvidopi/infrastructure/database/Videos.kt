package com.ktvidopi.infrastructure.database

import com.ktvidopi.domain.ProcessingStatus
import com.ktvidopi.domain.Video
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID

object Videos : UUIDTable("videos") {
    val title = varchar("title", 255)
    val fileName = varchar("file_name", 255)
    val fileSize = long("file_size")
    val status = enumerationByName<ProcessingStatus>("status", 50)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val processedAt = timestamp("processed_at").nullable()
    val errorMessage = text("error_message").nullable()
}

fun ResultRow.toVideo(): Video = Video(
    id = this[Videos.id].value,
    title = this[Videos.title],
    fileName = this[Videos.fileName],
    fileSize = this[Videos.fileSize],
    status = this[Videos.status],
    createdAt = this[Videos.createdAt],
    updatedAt = this[Videos.updatedAt],
    processedAt = this[Videos.processedAt],
    errorMessage = this[Videos.errorMessage]
)
