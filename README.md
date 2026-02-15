# KtVidoPi - Async Video Processing Service

A production-ready Kotlin backend service demonstrating **coroutine-first architecture** with clean separation of concerns, structured concurrency, and comprehensive testing.

## 🎯 Project Overview

This is a small but complete async video processing service that showcases:

- **Coroutines everywhere** - Using suspend functions and Flow for async operations
- **Structured concurrency** - Proper use of coroutineScope, SupervisorJob, and cancellation
- **Clean architecture** - Clear separation between API, Service, Repository, and Domain layers
- **Production practices** - Docker, CI/CD, comprehensive testing, structured logging

## 🏗 Architecture

### Layer Separation

```
┌─────────────────────────────────────┐
│         API Layer (Ktor)            │  ← HTTP endpoints, DTOs, exception handling
├─────────────────────────────────────┤
│      Service Layer (Coroutines)     │  ← Business logic, async processing
├─────────────────────────────────────┤
│    Repository Layer (Suspend)       │  ← Data access abstraction
├─────────────────────────────────────┤
│   Infrastructure (Database)         │  ← Exposed ORM, Postgres
└─────────────────────────────────────┘
```

### Key Design Decisions

1. **Coroutine-First**: All I/O operations use suspend functions, never blocking threads
2. **Constructor Injection**: Dependencies injected via constructors (no frameworks needed)
3. **Interface-Based Repositories**: Easy to swap implementations for testing
4. **DTO Mapping**: Separate API models from domain models
5. **Structured Concurrency**: SupervisorJob prevents child failures from cancelling parent

## ⚡ Coroutines Deep Dive

### Why Coroutines Over Threads?

**Threads:**
- Expensive to create (1MB stack per thread)
- Limited scalability (thousands of threads max)
- Blocking operations tie up threads
- Context switching overhead

**Coroutines:**
- Lightweight (few KB per coroutine)
- Millions of coroutines possible
- Suspending functions free threads for other work
- Cooperative multitasking

### Key Concepts Demonstrated

#### 1. Structured Concurrency

```kotlin
suspend fun processVideo(videoId: UUID) = coroutineScope {
    // All child coroutines must complete before this function returns
    // If any child fails, cancellation propagates properly
    launch { step1() }
    launch { step2() }
    // Parent waits for all children
}
```

**Why it matters**: Ensures no leaked coroutines, proper cleanup, and predictable cancellation.

#### 2. SupervisorJob

```kotlin
private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

**Why SupervisorJob?**: If one video processing job fails, it doesn't cancel other processing jobs. Regular Job would cancel all children.

#### 3. Dispatchers.IO

```kotlin
withContext(Dispatchers.IO) {
    videoRepository.findById(id) // Database call
}
```

**Why Dispatchers.IO?**: Database operations are blocking I/O. This dispatcher has a large thread pool optimized for I/O operations.

#### 4. Suspending vs Blocking

```kotlin
// ❌ BLOCKING - ties up thread for 6 seconds
Thread.sleep(6000)

// ✅ SUSPENDING - thread is free to handle other requests
delay(6000)
```

**Difference**: `delay()` suspends the coroutine, freeing the thread. `Thread.sleep()` blocks the thread.

### Cancellation Propagation

Coroutines support cooperative cancellation:

```kotlin
coroutineScope {
    launch {
        while (isActive) { // Checks cancellation status
            delay(100)
            // Do work
        }
    }
}
// When scope is cancelled, isActive becomes false
```

## 🧪 Testing Strategy

### Unit Tests (MockK)

- **Service Layer**: Tests business logic with mocked repositories
- **Controller Layer**: Tests HTTP handling with mocked services
- **Fast execution**: No external dependencies

### Integration Tests (Testcontainers)

- **Real Postgres**: Spins up actual database in Docker
- **End-to-end**: Tests complete flow from API to database
- **Coroutine-aware**: Uses `runTest` for proper coroutine testing

## 🚀 Getting Started

### Prerequisites

- JDK 17+
- Docker & Docker Compose
- Gradle (or use included wrapper)

### Running Locally

1. **Start database**:
   ```bash
   docker-compose up -d postgres
   ```

2. **Run application**:
   ```bash
   ./gradlew run
   ```

3. **Or use Docker Compose**:
   ```bash
   docker-compose up
   ```

### API Endpoints

#### Create Video (triggers async processing)
```bash
curl -X POST http://localhost:8080/api/videos \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My Video",
    "fileName": "video.mp4",
    "fileSize": 1024000
  }'
```

#### Get All Videos
```bash
curl http://localhost:8080/api/videos
```

#### Get Video by ID
```bash
curl http://localhost:8080/api/videos/{id}
```

#### Poll for Status
```bash
# Video status transitions: PENDING → PROCESSING → COMPLETED/FAILED
curl http://localhost:8080/api/videos/{id}
```

### Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests (requires Docker)
./gradlew test --tests "*IntegrationTest"
```

## 📦 Tech Stack

- **Kotlin** 1.9.22
- **Ktor** 2.3.7 - Coroutine-native web framework
- **Exposed** 0.45.0 - Type-safe SQL DSL
- **PostgreSQL** 16 - Database
- **HikariCP** - Connection pooling
- **MockK** - Mocking for Kotlin
- **Testcontainers** - Integration testing
- **Logback** - Structured logging (JSON format)

## 🐳 Docker

### Build Image
```bash
docker build -t ktvidopi .
```

### Run with Docker Compose
```bash
docker-compose up
```

The `docker-compose.yml` includes:
- Postgres service with health checks
- Application service with proper dependencies
- Volume persistence for database

## 🔄 CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`):
- Runs on push/PR to main/develop
- Sets up Postgres service
- Runs all tests
- Builds application

## 📝 Code Structure

```
src/
├── main/kotlin/com/ktvidopi/
│   ├── api/              # HTTP layer (controllers, DTOs, exceptions)
│   ├── domain/           # Domain models (Video, ProcessingStatus)
│   ├── repository/       # Data access interfaces and implementations
│   ├── service/          # Business logic with coroutines
│   └── infrastructure/   # Database setup (Exposed tables)
└── test/kotlin/
    ├── api/              # Controller tests
    ├── service/          # Service unit tests
    └── integration/     # End-to-end tests
```

## 🎓 Key Learning Points

### For CTOs/Interviewers

1. **Structured Concurrency**: The code demonstrates understanding of coroutine lifecycle and cancellation
2. **Architecture**: Clear separation of concerns, testable design
3. **Production Readiness**: Docker, CI/CD, logging, error handling
4. **Coroutine Mastery**: Proper use of dispatchers, scopes, and cancellation

### Common Interview Questions

**Q: Why coroutines over threads?**
- Lightweight, scalable, non-blocking
- Better resource utilization
- Structured concurrency prevents leaks

**Q: What's the difference between blocking and suspending?**
- Blocking: Thread is tied up, can't handle other requests
- Suspending: Coroutine pauses, thread is free

**Q: What is structured concurrency?**
- Parent coroutine waits for all children
- Cancellation propagates properly
- No leaked coroutines

**Q: When would you use SupervisorJob?**
- When child failures shouldn't cancel siblings
- Background processing jobs
- Independent tasks

## 🔍 Debugging Coroutines

### Stack Traces

Coroutine stack traces show the suspension points:
```
at com.ktvidopi.service.VideoProcessingService.processVideo(VideoProcessingService.kt:45)
at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
```

### Coroutine Debugging

Enable coroutine debugging:
```kotlin
-Dkotlinx.coroutines.debug
```

## 📚 Further Reading

- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Structured Concurrency](https://kotlinlang.org/docs/coroutines-guide.html#structured-concurrency)
- [Ktor Documentation](https://ktor.io/docs/)
- [Exposed Framework](https://github.com/JetBrains/Exposed)

## 📄 License

MIT

---

**Built with ❤️ using Kotlin Coroutines**
