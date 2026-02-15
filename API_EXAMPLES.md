# API Examples

## Create Video

Creates a video record and triggers async processing.

```bash
curl -X POST http://localhost:8080/api/videos \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My Awesome Video",
    "fileName": "awesome-video.mp4",
    "fileSize": 52428800
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "My Awesome Video",
  "fileName": "awesome-video.mp4",
  "fileSize": 52428800,
  "status": "PENDING",
  "createdAt": "2026-02-15T10:30:00Z",
  "updatedAt": "2026-02-15T10:30:00Z",
  "processedAt": null,
  "errorMessage": null
}
```

## Get All Videos

Retrieves all videos in the system.

```bash
curl http://localhost:8080/api/videos
```

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "My Awesome Video",
    "fileName": "awesome-video.mp4",
    "fileSize": 52428800,
    "status": "COMPLETED",
    "createdAt": "2026-02-15T10:30:00Z",
    "updatedAt": "2026-02-15T10:35:00Z",
    "processedAt": "2026-02-15T10:35:00Z",
    "errorMessage": null
  }
]
```

## Get Video by ID

Retrieves a specific video by its ID.

```bash
curl http://localhost:8080/api/videos/550e8400-e29b-41d4-a716-446655440000
```

**Response (if found):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "My Awesome Video",
  "fileName": "awesome-video.mp4",
  "fileSize": 52428800,
  "status": "PROCESSING",
  "createdAt": "2026-02-15T10:30:00Z",
  "updatedAt": "2026-02-15T10:32:00Z",
  "processedAt": null,
  "errorMessage": null
}
```

**Response (if not found):**
```json
{
  "message": "Video with id 550e8400-e29b-41d4-a716-446655440000 not found",
  "code": "VIDEO_NOT_FOUND"
}
```

## Polling for Status

To check processing status, poll the GET endpoint:

```bash
# Initial state
curl http://localhost:8080/api/videos/550e8400-e29b-41d4-a716-446655440000
# Status: PENDING

# After a few seconds
curl http://localhost:8080/api/videos/550e8400-e29b-41d4-a716-446655440000
# Status: PROCESSING

# After ~6 seconds (processing completes)
curl http://localhost:8080/api/videos/550e8400-e29b-41d4-a716-446655440000
# Status: COMPLETED (or FAILED if error occurred)
```

## Status Flow

```
PENDING → PROCESSING → COMPLETED
                      ↓
                    FAILED (if error)
```

Processing typically takes ~6 seconds (simulated).
