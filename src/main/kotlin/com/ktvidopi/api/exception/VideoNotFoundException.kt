package com.ktvidopi.api.exception

import java.util.UUID

class VideoNotFoundException(id: UUID) : RuntimeException("Video with id $id not found")
